package app.afterhours.core

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class Crawled(val source: String, val feedUrl: String, val pageUrl: String, val title: String, val image: String, val description: String, val episodes: List<Episode>)

/** Finds a show's feed on its website, or failing that, the episodes on the site itself. */
object Crawler {
    private val TAG_ATTR = Regex("([\\w:-]+)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))")
    private val AUDIO = Regex("https?://[^\\s\"'<>\\\\]+?\\.(?:mp3|m4a|m4b|aac|ogg|oga|opus|wav|flac)(?:\\?[^\\s\"'<>\\\\]*)?", RegexOption.IGNORE_CASE)
    private val EPISODE_HINT = Regex("(episode|/ep[-_/]?\\d|/e/\\d|podcast|/show|/listen|/audio|/radio|/archives?/|/\\d{4}/\\d{2}/|/p/|/posts?/|/blog/)", RegexOption.IGNORE_CASE)
    private val SKIP = Regex("(about|contact|privacy|terms|login|sign-?in|sign-?up|register|cart|shop|store|/tags?/|categor|/author/|search|subscribe|donate|\\.(?:jpe?g|png|gif|svg|webp|pdf|zip)(?:\\?|$))", RegexOption.IGNORE_CASE)
    private val FEEDISH = Regex("(/feed\\b|/rss\\b|rss\\.xml|feed\\.xml|podcast\\.xml|format=rss)", RegexOption.IGNORE_CASE)
    private val GUESSES = listOf("feed", "feed/podcast", "podcast/feed", "rss", "rss.xml", "feed.xml", "podcast.xml", "index.xml", "?format=rss")

    suspend fun crawl(url: String, hint: String, sid: String): Crawled {
        val page = Net.fetch(url, 4 shl 20)
        val html = page.text
        val base = page.finalUrl
        if (Feeds.looksLikeFeed(html)) {
            if (!html.contains("<enclosure", ignoreCase = true)) throw FetchException("That feed has no audio episodes.")
            return Crawled("rss", base, base, "", "", "", emptyList())
        }
        val meta = metas(html)
        val pageTitle = clean(meta["og:title"] ?: Regex("(?is)<title[^>]*>(.*?)</title>").find(html)?.groupValues?.get(1))
        val siteTitle = clean(meta["og:site_name"] ?: pageTitle)
        val image = meta["og:image"]?.let { resolveUrl(base, it) }.orEmpty()
        val description = clean(meta["og:description"] ?: meta["description"])
        val links = anchors(html, base)
        val explicit = advertisedFeeds(html, base, links).toMutableList()
        appleId(html)?.let { id -> runCatching { Apple.lookup(id, "gb") }.getOrNull()?.feedUrl?.let { explicit.add(it) } }
        val pageDir = base.substringBefore('?').substringBefore('#').let { if (it.endsWith("/")) it else "$it/" }
        val root = Regex("^(https?://[^/?#]+)").find(base)?.groupValues?.get(1)?.plus("/") ?: pageDir
        val guesses = GUESSES.flatMap { listOf(resolveUrl(pageDir, it), resolveUrl(root, it)) }
        val feed = firstFeed(explicit, null) ?: firstFeed(guesses, listOf(hint, siteTitle, pageTitle))
        if (feed != null) return Crawled("rss", feed, base, siteTitle, image, description, emptyList())

        val seen = LinkedHashSet<String>()
        val episodes = pageEpisodes(html, base, pageTitle, image, sid, seen).toMutableList()
        if (episodes.size < 3) {
            val gate = Semaphore(4)
            val more = coroutineScope {
                episodeLinks(links, base).map { link -> async { gate.withPermit { episodePage(link, sid) } } }.awaitAll()
            }
            for (e in more.filterNotNull()) if (seen.add(e.url)) episodes.add(e)
        }
        if (episodes.isEmpty()) throw FetchException("Afterhours couldn’t find a feed or any episodes on that page.")
        return Crawled("web", "", base, siteTitle, image, description, episodes.take(200))
    }

    private suspend fun episodePage(link: String, sid: String): Episode? = try {
        val f = Net.fetch(link, 2 shl 20)
        val meta = metas(f.text)
        val title = clean(meta["og:title"] ?: Regex("(?is)<title[^>]*>(.*?)</title>").find(f.text)?.groupValues?.get(1))
        pageEpisodes(f.text, f.finalUrl, title, meta["og:image"]?.let { resolveUrl(f.finalUrl, it) }.orEmpty(), sid, LinkedHashSet()).firstOrNull()
    } catch (e: FetchException) { null }

    private suspend fun firstFeed(candidates: List<String>, wants: List<String>?): String? {
        val tried = LinkedHashSet<String>()
        for (c in candidates) {
            if (!c.startsWith("http") || !tried.add(c) || tried.size > 14) continue
            val f = try { Net.fetch(c, 2 shl 20) } catch (e: FetchException) { continue }
            val t = f.text
            if (!Feeds.looksLikeFeed(t) || !t.contains("<enclosure", ignoreCase = true)) continue
            if (wants != null) {
                val title = clean(Regex("(?is)<title[^>]*>(.*?)</title>").find(t.replace(Regex("(?is)<image\\b.*?</image>"), ""))?.groupValues?.get(1))
                if (wants.none { it.isNotBlank() && related(title, it) }) continue
            }
            return f.finalUrl
        }
        return null
    }

    // ---- pieces that don't touch the network (tested directly) ----

    fun attrs(tag: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (m in TAG_ATTR.findAll(tag)) {
            val g = m.groups
            out[m.groupValues[1].lowercase()] = decodeEntities(g[3]?.value ?: g[4]?.value ?: g[5]?.value ?: "")
        }
        return out
    }

    fun metas(html: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (m in Regex("(?i)<meta\\b[^>]*>").findAll(html)) {
            val a = attrs(m.value)
            val key = (a["property"] ?: a["name"] ?: a["itemprop"] ?: "").lowercase()
            val content = a["content"]
            if (key.isNotEmpty() && content != null && key !in out) out[key] = content.trim()
        }
        return out
    }

    /** Links on the page: (address, text). */
    fun anchors(html: String, base: String): List<Pair<String, String>> =
        Regex("(?is)<a\\b([^>]*)>(.*?)</a>").findAll(html).mapNotNull { m ->
            val href = attrs(m.groupValues[1])["href"]?.trim() ?: return@mapNotNull null
            if (href.startsWith("#") || href.startsWith("javascript:") || href.startsWith("mailto:")) null else resolveUrl(base, href) to clean(m.groupValues[2])
        }.toList()

    fun advertisedFeeds(html: String, base: String, links: List<Pair<String, String>>): List<String> {
        val out = ArrayList<String>()
        for (m in Regex("(?i)<link\\b[^>]*>").findAll(html)) {
            val a = attrs(m.value)
            val rel = a["rel"].orEmpty().lowercase(); val type = a["type"].orEmpty().lowercase()
            if ("alternate" in rel && ("rss" in type || "atom" in type)) a["href"]?.let { out.add(resolveUrl(base, it)) }
        }
        links.filter { FEEDISH.containsMatchIn(it.first) }.forEach { out.add(it.first) }
        return out
    }

    fun appleId(html: String): Long? = Regex("podcasts\\.apple\\.com/[^\"'\\s<>]*?/id(\\d{5,12})").find(html)?.groupValues?.get(1)?.toLongOrNull()

    /** Audio on a page: from its metadata, audio tags, links, and data embedded in the page. */
    fun pageEpisodes(html: String, base: String, pageTitle: String, image: String, sid: String, seen: MutableSet<String>): List<Episode> {
        val meta = metas(html)
        val date = parseDate(meta["article:published_time"] ?: Regex("<time[^>]*datetime=[\"']([^\"']+)[\"']").find(html)?.groupValues?.get(1))
        val desc = clean(meta["og:description"] ?: meta["description"])
        val found = ArrayList<Pair<String, String>>()
        for (k in listOf("og:audio", "og:audio:url", "og:audio:secure_url", "twitter:player:stream")) meta[k]?.let { found.add(resolveUrl(base, it) to pageTitle) }
        Regex("(?i)<(?:audio|source)\\b[^>]*\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']").findAll(html).forEach { found.add(resolveUrl(base, it.groupValues[1]) to pageTitle) }
        anchors(html, base).filter { AUDIO.matches(it.first) }.forEach { found.add(it.first to it.second.ifBlank { pageTitle }) }
        val flat = html.replace("\\/", "/")
        for (m in AUDIO.findAll(flat)) {
            if (found.size > 120) break
            val before = flat.substring(maxOf(0, m.range.first - 800), m.range.first)
            val title = Regex("\"(?:title|name)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(before).lastOrNull()?.groupValues?.get(1)
            found.add(m.value to (title?.let { unescapeJson(it) } ?: pageTitle))
        }
        return found.mapNotNull { (url, title) ->
            if (!seen.add(url)) null
            else Episode(episodeId(sid, url), sid, clean(title).ifBlank { "Episode" }, url, date = date, image = image, desc = desc)
        }
    }

    fun episodeLinks(links: List<Pair<String, String>>, base: String): List<String> {
        val host = hostOf(base)
        val here = base.substringBefore('#').trimEnd('/')
        val out = ArrayList<String>()
        for ((href, _) in links) {
            val h = hostOf(href)
            if (h != host && !h.endsWith(".$host")) continue
            val clean = href.substringBefore('#')
            val path = clean.replaceFirst(Regex("^https?://[^/]+"), "")
            if (clean.trimEnd('/') == here || clean in out || SKIP.containsMatchIn(path) || !EPISODE_HINT.containsMatchIn(path)) continue
            out.add(clean)
            if (out.size >= 12) break
        }
        return out
    }

    private fun unescapeJson(s: String): String {
        val b = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val n = s[i + 1]
                if (n == 'u' && i + 5 < s.length) {
                    val code = s.substring(i + 2, i + 6).toIntOrNull(16)
                    if (code != null) { b.append(Char(code)); i += 6; continue }
                }
                b.append(when (n) { 'n' -> '\n'; 't' -> '\t'; else -> n })
                i += 2
            } else { b.append(c); i++ }
        }
        return b.toString()
    }
}
