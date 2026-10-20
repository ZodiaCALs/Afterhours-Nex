package app.afterhours.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Where the library is kept; each platform supplies one. */
interface Storage {
    fun read(name: String): String?
    fun write(name: String, text: String)
    fun delete(name: String)
    fun list(prefix: String): List<String> = emptyList()
    /** How much space a stored file takes. */
    fun bytes(name: String): Long = read(name)?.encodeToByteArray()?.size?.toLong() ?: 0L
}

data class SetupState(val total: Int = 0, val done: Int = 0, val running: Boolean = false, val note: String = "")

/** The shelf: shows, their episodes, listening progress and settings. */
class Library(private val storage: Storage, private val scope: CoroutineScope) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val showList = ListSerializer(Show.serializer())
    private val episodeList = ListSerializer(Episode.serializer())
    private val progressMap = MapSerializer(String.serializer(), Progress.serializer())
    private val stringList = ListSerializer(String.serializer())
    private val cueList = ListSerializer(Cue.serializer())

    private val _shows = MutableStateFlow<List<Show>>(emptyList())
    val shows: StateFlow<List<Show>> = _shows.asStateFlow()
    private val _episodes = MutableStateFlow<Map<String, List<Episode>>>(emptyMap())
    val episodes: StateFlow<Map<String, List<Episode>>> = _episodes.asStateFlow()
    private val _progress = MutableStateFlow<Map<String, Progress>>(emptyMap())
    val progress: StateFlow<Map<String, Progress>> = _progress.asStateFlow()
    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()
    private val _setup = MutableStateFlow(SetupState())
    val setup: StateFlow<SetupState> = _setup.asStateFlow()
    private val _loading = MutableStateFlow<Set<String>>(emptySet())
    val loading: StateFlow<Set<String>> = _loading.asStateFlow()
    private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
    val errors: StateFlow<Map<String, String>> = _errors.asStateFlow()
    private var removed: Set<String> = emptySet()

    fun load() {
        _settings.value = read("settings.json") { json.decodeFromString(Settings.serializer(), it) } ?: Settings()
        _progress.value = read("progress.json") { json.decodeFromString(progressMap, it) } ?: emptyMap()
        removed = read("removed.json") { json.decodeFromString(stringList, it).toSet() } ?: emptySet()
        val saved = read("shows.json") { json.decodeFromString(showList, it) }
        val shows = saved.orEmpty().toMutableList()
        for (p in Catalog.data.presets) {   // poster shows not on the shelf yet, unless they were removed
            val sid = "p-" + p.key
            if (shows.none { it.sid == sid } && p.key !in removed)
                shows.add(Show(sid = sid, title = p.title, author = p.author.ifBlank { p.artist }, cat = p.cat, preset = p.key, status = "pending", link = p.site))
        }
        _shows.value = shows
        _episodes.value = shows.associate { s -> s.sid to (read("eps-${s.sid}.json") { json.decodeFromString(episodeList, it) } ?: emptyList()) }
        if (saved == null) saveShows()
    }

    private inline fun <T> read(name: String, parse: (String) -> T): T? = try { storage.read(name)?.let(parse) } catch (e: Exception) { null }
    private fun saveShows() = storage.write("shows.json", json.encodeToString(showList, _shows.value))
    private fun saveEpisodes(sid: String) = storage.write("eps-$sid.json", json.encodeToString(episodeList, _episodes.value[sid].orEmpty()))
    private fun saveProgress() = storage.write("progress.json", json.encodeToString(progressMap, _progress.value))

    // ---- reading ----
    fun show(sid: String): Show? = _shows.value.firstOrNull { it.sid == sid }
    fun episode(id: String): Episode? = _episodes.value.values.asSequence().flatten().firstOrNull { it.id == id }
    fun sorted(sid: String): List<Episode> = _episodes.value[sid].orEmpty().let { list ->
        if (_settings.value.sort == "oldest") list.sortedBy { it.date } else list.sortedByDescending { it.date }
    }
    /** Episodes started but not finished, most recent first. */
    fun upNext(): List<Episode> = _progress.value.entries.filter { !it.value.done && it.value.t > 10 }
        .sortedByDescending { it.value.at }.mapNotNull { episode(it.key) }.take(12)
    fun latest(limit: Int = 40): List<Episode> = _episodes.value.values.flatMap { it.sortedByDescending { e -> e.date }.take(4) }
        .sortedByDescending { it.date }.take(limit)

    // ---- first launch: find the 30 poster shows ----
    fun startSetup() {
        if (_setup.value.running) return
        val pending = _shows.value.filter { it.status == "pending" }
        refreshAll()   // the shows already found get fresh episodes straight away
        if (pending.isEmpty()) return
        val presets = _shows.value.count { it.preset != null }
        _setup.value = SetupState(total = presets, done = presets - pending.size, running = true)
        scope.launch {
            for (s in pending) {
                val pick = s.preset?.let { Catalog.pick(it) } ?: continue
                val found = try { resolve(s, pick) } catch (e: CancellationException) { throw e } catch (e: Exception) { s.copy(status = "missing") }
                replace(found)
                _setup.update { it.copy(done = it.done + 1, note = "") }
                if (found.status == "ready" && found.source == "rss") launch { refreshNow(found.sid) }
            }
            _setup.update { it.copy(running = false) }
        }
    }

    private suspend fun appleSearch(term: String, country: String): List<AppleShow> = try {
        Apple.search(term, country, 10)
    } catch (e: FetchException) {
        if ("slow down" !in e.message.orEmpty()) emptyList() else {
            _setup.update { it.copy(note = "Apple’s directory asked for a short pause. Carrying on in a moment…") }
            delay(20_000)
            try { Apple.search(term, country, 10) } catch (e2: FetchException) { emptyList() }
        }
    }

    private suspend fun resolve(show: Show, pick: Pick): Show {
        val artist = pick.artist.ifBlank { pick.author }
        for (country in pick.countries.ifEmpty { listOf("us") }) {
            val best = appleSearch(pick.term.ifBlank { pick.title }, country)
                .filter { related(it.title, pick.title) }
                .maxByOrNull { (if (artist.isNotBlank() && related(it.artist, artist)) 2 else 0) + (if (it.title.equals(pick.title, true)) 1 else 0) }
            if (best != null) return show.copy(feedUrl = best.feedUrl, image = best.artwork, author = show.author.ifBlank { best.artist }, status = "ready")
        }
        if (pick.site.isNotBlank()) {
            val c = Crawler.crawl(pick.site, pick.title, show.sid)
            if (c.source == "rss") return show.copy(feedUrl = c.feedUrl, image = show.image.ifBlank { c.image }, status = "ready")
            setEpisodes(show.sid, c.episodes)
            return show.copy(source = "web", pageUrl = c.pageUrl, image = c.image, description = c.description, status = "ready")
        }
        return show.copy(status = "missing")
    }

    /** Look again for the poster shows that couldn't be found. */
    fun retryMissing(): Int {
        val missing = _shows.value.filter { it.status == "missing" && it.preset != null }
        if (missing.isEmpty()) return 0
        _shows.update { list -> list.map { if (it.status == "missing" && it.preset != null) it.copy(status = "pending") else it } }
        saveShows()
        startSetup()
        return missing.size
    }

    // ---- keeping episodes up to date ----
    fun refresh(sid: String) { scope.launch { refreshNow(sid) } }

    fun refreshAll() {
        scope.launch {
            val gate = Semaphore(4)
            for (s in _shows.value.filter { it.status == "ready" }) launch { gate.withPermit { refreshNow(s.sid) } }
        }
    }

    /** Fetches a show's episodes. Returns an error message, or null when it worked. */
    suspend fun refreshNow(sid: String): String? {
        val show = show(sid) ?: return null
        if (show.source == "local") return null           // files on this device: nothing to fetch
        if (show.status != "ready" || sid in _loading.value) return null
        _loading.update { it + sid }
        try {
            if (show.source == "video") {
                val bridge = Video.bridge?.takeIf { it.available } ?: throw FetchException("Video channels need yt-dlp on this computer.")
                setEpisodes(sid, bridge.feed(show.feedUrl, sid).episodes)
            } else if (show.source == "web") {
                val c = Crawler.crawl(show.pageUrl, show.title, sid)
                if (c.source == "rss") { replace(show.copy(source = "rss", feedUrl = c.feedUrl)); readFeed(sid) } else setEpisodes(sid, c.episodes)
            } else {
                try {
                    readFeed(sid)
                } catch (e: FetchException) {
                    val site = show.link.ifBlank { show.preset?.let { Catalog.pick(it)?.site }.orEmpty() }
                    if (site.isBlank()) throw e
                    val c = Crawler.crawl(site, show.title, sid)   // the feed failed: look on the show's website
                    when {
                        c.source == "rss" && c.feedUrl != show.feedUrl -> { replace(show.copy(feedUrl = c.feedUrl)); readFeed(sid) }
                        c.source == "web" -> { replace(show.copy(source = "web", pageUrl = c.pageUrl)); setEpisodes(sid, c.episodes) }
                        else -> throw e
                    }
                }
            }
            _errors.update { it - sid }
            return null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = e.message ?: "Couldn’t load episodes."
            _errors.update { it + (sid to message) }
            return message
        } finally {
            _loading.update { it - sid }
        }
    }

    private suspend fun readFeed(sid: String) {
        val show = show(sid) ?: return
        val f = Net.fetch(show.feedUrl)
        val info = Feeds.parse(f.text, sid, f.finalUrl)
        if (info.episodes.isEmpty()) throw FetchException("This feed has no playable episodes.")
        replace(show.copy(title = show.title.ifBlank { info.title }, author = show.author.ifBlank { info.author }, image = show.image.ifBlank { info.image },
            description = info.description.ifBlank { show.description }, link = show.link.ifBlank { info.link }))
        setEpisodes(sid, info.episodes)
    }

    private fun replace(show: Show) {
        _shows.update { list -> list.map { if (it.sid == show.sid) show else it } }
        saveShows()
    }

    private fun setEpisodes(sid: String, all: List<Episode>) {
        val list = all.map { if (it.desc.length > 1000) it.copy(desc = it.desc.take(1000) + "…") else it }   // long show notes cost memory
        _episodes.update { it + (sid to list) }
        saveEpisodes(sid)
    }

    // ---- adding and removing ----
    fun has(feedUrl: String) = _shows.value.any { it.feedUrl == feedUrl && feedUrl.isNotBlank() }

    fun addFromApple(a: AppleShow): Show {
        _shows.value.firstOrNull { it.feedUrl == a.feedUrl }?.let { return it }
        val s = Show(sid = stableId("u-", a.feedUrl), title = a.title, author = a.artist, image = a.artwork, feedUrl = a.feedUrl, addedAt = Clock.now())
        _shows.update { it + s }
        saveShows()
        refresh(s.sid)
        return s
    }

    /** Adds a show from a feed, an Apple Podcasts link or the show's website. */
    /** Audio files from this device (MP3, Ogg, FLAC and the rest), kept together as one show. */
    fun addLocal(files: List<LocalAudio>): Show {
        val sid = "local"
        val show = show(sid) ?: Show(sid = sid, title = "On this device", source = "local", description = "Audio files you’ve imported.", addedAt = Clock.now())
            .also { s -> _shows.update { it + s }; saveShows() }
        val have = _episodes.value[sid].orEmpty()
        val now = Clock.now()
        val fresh = files.filter { f -> have.none { it.url == f.uri } }.mapIndexed { i, f ->
            Episode(id = stableId("l-", f.uri), sid = sid, title = f.name.substringBeforeLast('.'), url = f.uri, date = now - i)
        }
        setEpisodes(sid, fresh + have)
        return show
    }

    /** A YouTube or Bilibili channel or playlist, as a show of its videos. */
    private suspend fun addVideo(url: String): Show {
        val sid = stableId("v-", url)
        _shows.value.firstOrNull { it.sid == sid }?.let { return it }
        val info = Video.bridge!!.feed(url, sid)
        if (info.episodes.isEmpty()) throw FetchException("No videos found at that link.")
        val s = Show(sid = sid, title = info.title.ifBlank { hostOf(url) }, author = info.author, image = info.image, source = "video", feedUrl = url,
            description = info.description, link = url, addedAt = Clock.now())
        _shows.update { it + s }
        saveShows()
        setEpisodes(sid, info.episodes)
        return s
    }

    suspend fun addByLink(url: String): Show {
        if (Video.handles(url)) return addVideo(url)
        Regex("podcasts\\.apple\\.com/.*?/id(\\d+)").find(url)?.groupValues?.get(1)?.toLongOrNull()?.let { id ->
            val a = Apple.lookup(id) ?: throw FetchException("Apple doesn’t list a public feed for that show.")
            return addFromApple(a)
        }
        val c = Crawler.crawl(url, "", stableId("u-", url))
        if (c.source == "rss") {
            val f = Net.fetch(c.feedUrl)
            val sid = stableId("u-", f.finalUrl)
            _shows.value.firstOrNull { it.sid == sid }?.let { return it }
            val info = Feeds.parse(f.text, sid, f.finalUrl)
            if (info.episodes.isEmpty()) throw FetchException("This feed has no playable episodes.")
            val s = Show(sid = sid, title = info.title.ifBlank { "Untitled show" }, author = info.author, image = info.image, feedUrl = f.finalUrl,
                description = info.description, link = info.link, addedAt = Clock.now())
            _shows.update { it + s }
            saveShows()
            setEpisodes(sid, info.episodes)
            return s
        }
        val sid = stableId("u-", c.pageUrl)
        val s = Show(sid = sid, title = c.title.ifBlank { hostOf(c.pageUrl) }, image = c.image, source = "web", pageUrl = c.pageUrl,
            description = c.description, link = c.pageUrl, addedAt = Clock.now())
        _shows.update { list -> list.filterNot { it.sid == sid } + s }
        saveShows()
        setEpisodes(sid, c.episodes.map { it.copy(sid = sid, id = episodeId(sid, it.url)) })
        return s
    }

    /** Points a show at a different feed or website. Returns an error message, or null when it worked. */
    suspend fun changeSource(sid: String, url: String): String? {
        val show = show(sid) ?: return null
        return try {
            val c = Crawler.crawl(url.trim(), show.title, sid)
            if (c.source == "rss") {
                replace(show.copy(source = "rss", feedUrl = c.feedUrl, status = "ready"))
                refreshNow(sid)
            } else {
                replace(show.copy(source = "web", pageUrl = c.pageUrl, status = "ready", image = show.image.ifBlank { c.image }))
                setEpisodes(sid, c.episodes)
                _errors.update { it - sid }
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.message ?: "Couldn’t use that address."
        }
    }

    // ---- the listener's own arrangement: names, pins, groups and order ----
    fun rename(sid: String, name: String) = arrange(sid) { it.copy(alias = name.trim().takeIf { n -> n != it.title }.orEmpty()) }
    fun setPinned(sid: String, pinned: Boolean) = arrange(sid) { it.copy(pinned = pinned) }
    fun setGroup(sid: String, group: String) = arrange(sid) { it.copy(group = group.trim()) }

    /** Puts the shows in this order (any not listed keep their place at the end). */
    fun reorder(sids: List<String>) {
        _shows.update { list -> val bySid = list.associateBy { it.sid }; sids.mapNotNull { bySid[it] } + list.filter { it.sid !in sids } }
        saveShows()
    }

    private fun arrange(sid: String, change: (Show) -> Show) {
        _shows.update { list -> list.map { if (it.sid == sid) change(it) else it } }
        saveShows()
    }

    fun setCover(sid: String, uri: String) = arrange(sid) { it.copy(cover = uri, coverStyle = "") }
    fun setCoverStyle(sid: String, style: String) = arrange(sid) { it.copy(coverStyle = style, cover = "") }
    fun resetCover(sid: String) = arrange(sid) { it.copy(cover = "", coverStyle = "") }

    private var lastRemoved: Triple<Show, List<Episode>, Int>? = null

    /** Puts back the show removed last, in its place, with its episodes. */
    fun undoRemove(): Show? {
        val (show, eps, index) = lastRemoved ?: return null
        lastRemoved = null
        _shows.update { list -> if (list.any { it.sid == show.sid }) list else list.toMutableList().apply { add(index.coerceIn(0, size), show) } }
        saveShows()
        if (eps.isNotEmpty()) setEpisodes(show.sid, eps)
        show.preset?.let { removed = removed - it; storage.write("removed.json", json.encodeToString(stringList, removed.toList())) }
        return show
    }

    fun remove(sid: String) {
        val show = show(sid) ?: return
        lastRemoved = Triple(show, _episodes.value[sid].orEmpty(), _shows.value.indexOfFirst { it.sid == sid })
        _shows.update { list -> list.filterNot { it.sid == sid } }
        _episodes.update { it - sid }
        saveShows()
        storage.delete("eps-$sid.json")
        show.preset?.let { removed = removed + it; storage.write("removed.json", json.encodeToString(stringList, removed.toList())) }
    }

    // ---- captions and OPML ----
    fun loadCues(id: String): List<Cue>? = read("cap-$id.json") { json.decodeFromString(cueList, it) }?.takeIf { it.isNotEmpty() }
    fun saveCues(id: String, cues: List<Cue>) = storage.write("cap-$id.json", json.encodeToString(cueList, cues))
    /** Deletes the cached episode lists; each show downloads its list again the next time it's refreshed. */
    fun clearEpisodeCache(): Int {
        val names = storage.list("eps-")
        names.forEach { storage.delete(it) }
        return names.size
    }

    fun clearSavedCaptions(): Int {
        val names = storage.list("cap-")
        names.forEach { storage.delete(it) }
        return names.size
    }

    fun exportOpml(): String = Opml.export(_shows.value)

    /** Adds the shows in an OPML file that aren't on the shelf yet. Returns how many were added. */
    fun importOpml(text: String): Int {
        val fresh = Opml.read(text).filter { (_, url) -> _shows.value.none { it.feedUrl == url } }
            .map { (title, url) -> Show(sid = stableId("u-", url), title = title, feedUrl = url, addedAt = Clock.now()) }
        if (fresh.isEmpty()) return 0
        _shows.update { it + fresh }
        saveShows()
        scope.launch {
            val gate = Semaphore(4)
            for (s in fresh) launch { gate.withPermit { refreshNow(s.sid) } }
        }
        return fresh.size
    }

    // ---- progress and settings ----
    fun setProgress(id: String, seconds: Long, length: Long, done: Boolean = false) {
        if (id.startsWith("radio:")) return          // a live station has no position to come back to
        _progress.update { it + (id to Progress(seconds, length, Clock.now(), done)) }
        saveProgress()
    }

    fun updateSettings(change: (Settings) -> Settings) {
        _settings.update(change)
        storage.write("settings.json", json.encodeToString(Settings.serializer(), _settings.value))
    }
}
