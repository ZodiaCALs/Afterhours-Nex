package app.afterhours.core

import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.encodeURLParameter
import io.ktor.utils.io.readAvailable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*

/**
 * Your computer, for a phone. With the phone link from the desktop app (Settings › Use on your phone), a phone borrows
 * its computer's auto captions and video podcasts over Wi-Fi, so the same features work everywhere and the phone stays light.
 */
object Paired {
    @Volatile var link: String = ""
    val ready: Boolean get() = base.isNotEmpty() && key.isNotEmpty()
    private val base: String get() = Regex("^(https?://[^/?#]+)").find(link.trim())?.groupValues?.get(1).orEmpty()
    private val key: String get() = Regex("[?&]key=([^&#]+)").find(link)?.groupValues?.get(1).orEmpty()

    fun url(path: String, vararg q: Pair<String, String>): String = buildString {
        append(base).append(path).append("?key=").append(key)
        for ((k, v) in q) append('&').append(k).append('=').append(v.encodeURLParameter())
    }

    suspend fun ping(): JsonObject = Json.parseToJsonElement(Net.fetch(url("/api/nex/ping"), 1 shl 16).text).jsonObject

    /** Reads a stream of JSON lines as the computer sends them (captions and downloads take a while). */
    suspend fun lines(u: String, each: (JsonObject) -> Unit) {
        Net.client.prepareGet(u) { timeout { requestTimeoutMillis = 6 * 3_600_000L; socketTimeoutMillis = 15 * 60_000L } }.execute { response ->
            val ch = response.bodyAsChannel()
            var pending = ByteArray(0)                 // bytes, so a character split between two reads stays whole
            val buf = ByteArray(16 * 1024)
            fun emit(bytes: ByteArray) { val line = bytes.decodeToString().trim(); if (line.isNotEmpty()) each(Json.parseToJsonElement(line).jsonObject) }
            while (!ch.isClosedForRead) {
                val n = ch.readAvailable(buf, 0, buf.size)
                if (n <= 0) continue
                pending += buf.copyOf(n)
                while (true) {
                    val i = pending.indexOf('\n'.code.toByte()); if (i < 0) break
                    emit(pending.copyOfRange(0, i)); pending = pending.copyOfRange(i + 1, pending.size)
                }
            }
            if (pending.isNotEmpty()) emit(pending)
        }
    }

    fun failure(o: JsonObject) { o["error"]?.jsonPrimitive?.contentOrNull?.let { throw FetchException(it) } }
}

/** Auto captions made by the paired computer, for episodes on the web. */
class RemoteCaptions : CaptionEngine {
    override val models: List<Pair<String, String>> = emptyList()
    override fun hasModel(name: String) = true
    override suspend fun transcribe(url: String, startMs: Long, durationMs: Long, options: AsrOptions,
                                    status: (String, Float) -> Unit, onCues: (List<Cue>) -> Unit) {
        if (!url.startsWith("http")) throw FetchException("Your computer can caption episodes from the web, not files on this phone.")
        status("Asking your computer to make captions…", -1f)
        Paired.lines(Paired.url("/api/nex/captions", "url" to url, "start" to "$startMs", "duration" to "$durationMs",
            "options" to Json.encodeToString(AsrOptions.serializer(), options))) { o ->
            Paired.failure(o)
            o["status"]?.jsonPrimitive?.contentOrNull?.let { status(it, o["progress"]?.jsonPrimitive?.floatOrNull ?: -1f) }
            o["cues"]?.jsonArray?.let { arr ->
                onCues(arr.map { c -> val j = c.jsonObject
                    Cue(j["start"]?.jsonPrimitive?.longOrNull ?: 0, j["end"]?.jsonPrimitive?.longOrNull ?: 0, j["text"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        j["speaker"]?.jsonPrimitive?.contentOrNull.orEmpty()) })
            }
        }
    }
}

/** Video podcasts fetched by the paired computer (with its yt-dlp) and streamed to the phone. */
class RemoteVideo : VideoBridge {
    private val json = Json { ignoreUnknownKeys = true }
    override val available: Boolean get() = Paired.ready
    override fun handles(url: String) = VIDEO_SITES.containsMatchIn(url)
    override suspend fun feed(url: String, sid: String): FeedInfo =
        json.decodeFromString(FeedInfo.serializer(), Net.fetch(Paired.url("/api/nex/video/feed", "url" to url, "sid" to sid), 8 shl 20).text)
    override suspend fun audio(url: String, status: (String, Float) -> Unit): String {
        var file = ""
        Paired.lines(Paired.url("/api/nex/video/prepare", "url" to url)) { o ->
            Paired.failure(o)
            o["status"]?.jsonPrimitive?.contentOrNull?.let { status(it, o["progress"]?.jsonPrimitive?.floatOrNull ?: -1f) }
            o["file"]?.jsonPrimitive?.contentOrNull?.let { file = it }
        }
        if (file.isBlank()) throw FetchException("Couldn’t get this video’s audio.")
        return Paired.url("/api/nex/file/$file")
    }
    override suspend fun search(query: String, limit: Int): List<VideoHit> =
        json.decodeFromString(ListSerializer(VideoHit.serializer()), Net.fetch(Paired.url("/api/nex/video/search", "q" to query, "limit" to "$limit"), 4 shl 20).text)
}

/** The video sites AfterhoursNex plays as audio. */
val VIDEO_SITES = Regex("""(?i)https?://([\w-]+\.)*(youtube\.com|youtu\.be|bilibili\.com|b23\.tv|nicovideo\.jp|vimeo\.com)/""")

// ---- updates from GitHub releases ----

data class Release(val version: String, val title: String, val page: String, val download: String)

object Updates {
    /** The latest release of [repo] ("owner/name") on GitHub, with the download that suits [platform]. */
    suspend fun latest(repo: String, platform: String): Release? {
        if (!Regex("""^[\w.-]+/[\w.-]+$""").matches(repo)) return null
        val o = Json.parseToJsonElement(Net.fetch("https://api.github.com/repos/$repo/releases/latest", 2 shl 20).text).jsonObject
        val tag = o["tag_name"]?.jsonPrimitive?.contentOrNull ?: return null
        val version = Regex("""\d+(\.\d+){1,3}""").find(tag)?.value ?: return null
        val page = o["html_url"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val assets = o["assets"]?.jsonArray?.mapNotNull { a ->
            val j = a.jsonObject
            val name = j["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            name to (j["browser_download_url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null)
        }.orEmpty()
        val wanted = when (platform) { "macOS" -> listOf(".dmg"); "Windows" -> listOf(".msi", ".exe"); "Android" -> listOf(".apk"); else -> emptyList() }
        val download = wanted.firstNotNullOfOrNull { ext -> assets.firstOrNull { it.first.endsWith(ext) }?.second } ?: page
        return Release(version, o["name"]?.jsonPrimitive?.contentOrNull ?: tag, page, download)
    }

    /** True when [candidate] ("1.2.1") is newer than [current] ("1.2.0"). */
    fun newer(candidate: String, current: String): Boolean {
        val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }; val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) { val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }; if (x != y) return x > y }
        return false
    }
}

// ---- audio files from this device ----

data class LocalAudio(val uri: String, val name: String, val bytes: Long)

/** The audio files AfterhoursNex imports. */
val AUDIO_FILES = listOf("mp3", "m4a", "m4b", "aac", "ogg", "oga", "opus", "flac", "wav", "aif", "aiff", "wma", "mka", "webm", "alac")
