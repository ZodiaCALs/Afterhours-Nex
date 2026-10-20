package app.afterhours.desktop

import app.afterhours.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File

/**
 * Video podcasts from YouTube, Bilibili and similar sites, heard as audio, through yt-dlp when it's installed
 * (it isn't bundled, which keeps AfterhoursNex light). Audio is fetched as AAC, which plays without converting.
 */
class YtDlp(private val dir: File, private val tools: File) : VideoBridge {
    val path: String? get() = exe
    private val own: File get() = File(tools, if (System.getProperty("os.name").startsWith("Win")) "yt-dlp.exe" else "yt-dlp")
    private val system: String? by lazy { findTool("yt-dlp") }
    /** The copy AfterhoursNex installed (in its own folder), or one the listener installed themselves. */
    private val exe: String? get() = own.takeIf { it.isFile && it.length() > 1_000_000 && it.canExecute() }?.absolutePath ?: system
    override val manageable = true
    override val installedByApp: Boolean get() = own.isFile && own.canExecute()

    /** Downloads yt-dlp's standalone program for this system from its official releases, about 35 MB. */
    override suspend fun install(progress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        tools.mkdirs()
        val os = System.getProperty("os.name"); val arch = System.getProperty("os.arch")
        val asset = when { os.startsWith("Mac") -> "yt-dlp_macos"; os.startsWith("Win") -> "yt-dlp.exe"
            arch.contains("aarch64") || arch.contains("arm64") -> "yt-dlp_linux_aarch64"; else -> "yt-dlp_linux" }
        val client = java.net.http.HttpClient.newBuilder().followRedirects(java.net.http.HttpClient.Redirect.NORMAL).build()
        val request = java.net.http.HttpRequest.newBuilder(java.net.URI("https://github.com/yt-dlp/yt-dlp/releases/latest/download/$asset"))
            .header("User-Agent", "AfterhoursNex/${AppInfo.VERSION}").build()
        val reply = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream())
        if (reply.statusCode() != 200) return@withContext false
        val total = reply.headers().firstValueAsLong("content-length").orElse(-1L)
        val part = File(tools, own.name + ".part")
        reply.body().use { input -> part.outputStream().use { out ->
            val buf = ByteArray(64 * 1024); var done = 0L
            while (true) { val n = input.read(buf); if (n < 0) break; out.write(buf, 0, n); done += n; if (total > 0) progress(done.toFloat() / total) }
        } }
        own.delete(); part.renameTo(own); own.setExecutable(true)
        own.canExecute()
    }

    override suspend fun update(): String = withContext(Dispatchers.IO) {
        val e = exe ?: return@withContext ""
        runCatching { runTool(e, listOf("-U"), 300).lines().lastOrNull { it.isNotBlank() }.orEmpty() }.getOrElse { it.message.orEmpty() }
    }

    override fun uninstall(): Boolean = own.delete()
    override val available: Boolean get() = exe != null
    override fun handles(url: String) = SITES.containsMatchIn(url)

    override suspend fun feed(url: String, sid: String): FeedInfo = withContext(Dispatchers.IO) {
        val o = Json.parseToJsonElement(runTool(tool(), listOf("-J", "--flat-playlist", "--playlist-end", "80", "--no-warnings", url), 180)).jsonObject
        val entries = o["entries"]?.jsonArray?.mapNotNull { it as? JsonObject } ?: listOf(o)
        FeedInfo(title = o.str("title").ifBlank { o.str("channel") }, author = o.str("channel").ifBlank { o.str("uploader") }, image = thumb(o),
            description = o.str("description").take(1500), link = url,
            episodes = entries.mapNotNull { e -> val v = e.str("url").ifBlank { e.str("webpage_url") }.ifBlank { return@mapNotNull null }
                Episode(id = stableId("v-", v), sid = sid, title = e.str("title"), url = v, date = date(e), duration = e.num("duration"),
                    image = thumb(e), desc = e.str("description").take(1500)) })
    }

    override suspend fun audio(url: String, status: (String, Float) -> Unit): String = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val id = stableId("y", url)
        fun done() = dir.listFiles()?.firstOrNull { it.name.startsWith("$id.") && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") }
        done()?.let { it.setLastModified(System.currentTimeMillis()); return@withContext it.toURI().toString() }
        runTool(tool(), listOf("-f", "bestaudio[ext=m4a]/bestaudio[acodec^=mp4a]/bestaudio", "--no-playlist", "--newline", "--no-warnings", "--ffmpeg-location", Ffmpeg.path,
            "-o", File(dir, "$id.%(ext)s").path, url), 1800) { line ->
            Regex("""\[download]\s+([\d.]+)%""").find(line)?.groupValues?.get(1)?.toFloatOrNull()?.let { status("Getting the audio… ${it.toInt()}%", it / 100f) }
        }
        val f = done() ?: throw FetchException("Couldn’t get this video’s audio.")
        trim()
        f.toURI().toString()
    }

    override suspend fun search(query: String, limit: Int): List<VideoHit> = withContext(Dispatchers.IO) {
        listOf("ytsearch" to "YouTube", "bilisearch" to "Bilibili").flatMap { (prefix, site) ->
            runCatching {
                val o = Json.parseToJsonElement(runTool(tool(), listOf("-J", "--flat-playlist", "--no-warnings", "$prefix$limit:$query"), 90)).jsonObject
                o["entries"]?.jsonArray?.mapNotNull { it as? JsonObject }?.mapNotNull { e ->
                    val v = e.str("url").ifBlank { e.str("webpage_url") }.ifBlank { return@mapNotNull null }
                    VideoHit(v, e.str("title"), e.str("channel").ifBlank { e.str("uploader") }, e.num("duration"), thumb(e), site)
                }.orEmpty()
            }.getOrDefault(emptyList())
        }
    }

    private fun tool() = exe ?: throw FetchException("Install yt-dlp to play videos as audio.")

    /** Keeps downloaded audio under 1.5 GB, removing the longest-unplayed first. */
    private fun trim() {
        val files = dir.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (f in files) { if (total < 1_500_000_000L) break; total -= f.length(); f.delete() }
    }

    private fun JsonObject.str(k: String) = this[k]?.jsonPrimitive?.contentOrNull.orEmpty()
    private fun JsonObject.num(k: String) = this[k]?.jsonPrimitive?.doubleOrNull?.toLong() ?: 0L
    private fun thumb(o: JsonObject): String = o.str("thumbnail").ifBlank {
        o["thumbnails"]?.jsonArray?.mapNotNull { (it as? JsonObject)?.str("url") }?.lastOrNull { it.startsWith("http") }.orEmpty()
    }
    private fun date(o: JsonObject): Long {
        o["timestamp"]?.jsonPrimitive?.longOrNull?.let { return it * 1000 }
        val d = o.str("upload_date")
        return if (d.length == 8) parseDate("${d.take(4)}-${d.substring(4, 6)}-${d.substring(6)}T12:00:00Z") else 0L
    }

    private companion object {
        val SITES = Regex("""(?i)https?://([\w-]+\.)*(youtube\.com|youtu\.be|bilibili\.com|b23\.tv|nicovideo\.jp|vimeo\.com)/""")
    }
}

/** Translation language packs through Argos Translate's argospm, when it's installed (pip install argostranslate). */
class ArgosPacks : LanguagePacks {
    private val exe: String? by lazy { findTool("argospm") }
    override val available: Boolean get() = exe != null
    override suspend fun installed() = withContext(Dispatchers.IO) { codes(runTool(tool(), listOf("list"), 60)) }
    override suspend fun catalog() = withContext(Dispatchers.IO) {
        runCatching { runTool(tool(), listOf("update"), 120) }
        codes(runTool(tool(), listOf("search"), 120))
    }
    override suspend fun install(pack: String) = withContext(Dispatchers.IO) { runCatching { runTool(tool(), listOf("install", pack), 1800) }.isSuccess }
    override suspend fun remove(pack: String) = withContext(Dispatchers.IO) { runCatching { runTool(tool(), listOf("remove", pack), 120) }.isSuccess }
    private fun tool() = exe ?: throw FetchException("Install Argos Translate (pip install argostranslate) to manage language packs.")
    private fun codes(out: String) = Regex("translate-[a-z]{2,3}_[a-z]{2,3}").findAll(out).map { it.value }.distinct().sorted().toList()
}
