package app.afterhours

import app.afterhours.core.AppInfo
import app.afterhours.core.AsrOptions
import app.afterhours.core.CaptionEngine
import app.afterhours.core.FeedInfo
import app.afterhours.core.VideoBridge
import app.afterhours.core.VideoHit
import app.afterhours.ui.PhoneLink
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.net.URLDecoder
import app.afterhours.ui.PhoneState
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Properties
import java.util.concurrent.Executors

/**
 * Lets phones on the same network use AfterhoursNex from this computer. It serves the phone version of the
 * app, and its helper for feeds, search and websites ([Api], shared with the Android web app), only on the
 * computer's network address and only behind a private link: opening the link sets a cookie, and every
 * request without the right cookie is refused.
 */
class PhoneServer(private val dir: File, private val captions: CaptionEngine?, private val video: VideoBridge?, private val videoDir: File) : PhoneLink {
    private val _state = MutableStateFlow(PhoneState())
    override val state: StateFlow<PhoneState> = _state.asStateFlow()
    private val prefs = File(dir, "phone.properties")
    private val api = Api({ name -> PhoneServer::class.java.getResourceAsStream("/phone/$name") ?: throw IOException("Missing $name") }, "https://itunes.apple.com")
    private var server: HttpServer? = null
    private var host = ""
    private var port = 0
    private var enabled = false
    @Volatile private var key = ""

    init {
        val p = Properties()
        runCatching { prefs.inputStream().use { p.load(it) } }
        key = p.getProperty("key").orEmpty().ifBlank { newKey() }
        enabled = p.getProperty("on") == "true"
        if (enabled) start()
    }

    @Synchronized override fun setEnabled(on: Boolean) {
        enabled = on; save()
        if (on) start() else stop()
    }

    @Synchronized override fun renew() {
        key = newKey(); save()
        if (server != null) publish()
    }

    @Synchronized fun close() { server?.stop(0); server = null }

    private fun start() {
        if (server != null) return publish()
        val address = lanAddress() ?: return fail("Phone access couldn’t start: this computer isn’t connected to a network.")
        for (p in 8765..8775) {
            try {
                val s = HttpServer.create(InetSocketAddress(address, p), 0)
                s.executor = Executors.newFixedThreadPool(6) { r -> Thread(r, "phone-access").apply { isDaemon = true } }
                s.createContext("/") { ex -> serve(ex) }
                s.start()
                server = s; host = address.hostAddress; port = p
                return publish()
            } catch (_: IOException) {
                // that port is taken: try the next one
            }
        }
        fail("Phone access couldn’t start: ports 8765 to 8775 are all in use.")
    }

    private fun stop() { close(); _state.value = PhoneState() }
    private fun fail(message: String) { _state.value = PhoneState(on = false, error = message) }

    private fun publish() {
        val url = "http://$host:$port/?key=$key"
        _state.value = PhoneState(on = true, url = url, qr = qr(url))
    }

    private fun save() {
        runCatching {
            dir.mkdirs()
            val p = Properties()
            p.setProperty("key", key); p.setProperty("on", enabled.toString())
            prefs.outputStream().use { p.store(it, "AfterhoursNex phone access") }
        }
    }

    private fun serve(ex: HttpExchange) {
        try {
            val hostHeader = ex.requestHeaders.getFirst("Host").orEmpty().substringBefore(':')
            if (hostHeader != host) return send(ex, 403, "text/plain; charset=utf-8", "This address isn’t allowed.".toByteArray())
            val query = ex.requestURI.rawQuery.orEmpty()
            val path = ex.requestURI.rawPath
            if (path.startsWith("/api/nex/")) {          // the AfterhoursNex phone app, borrowing this computer's captions and videos
                val q = query.split('&').filter { '=' in it }.associate { it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), Charsets.UTF_8) }
                if (!same(q["key"].orEmpty(), key)) return send(ex, 401, "text/plain; charset=utf-8", "Open the current phone link.".toByteArray())
                return nex(ex, path, q)
            }
            val given = query.split('&').firstOrNull { it.startsWith("key=") }?.removePrefix("key=")
            if (given != null) {
                if (!same(given, key)) return send(ex, 401, "text/html; charset=utf-8", DENIED)
                ex.responseHeaders.add("Set-Cookie", "ah_key=$key; Path=/; Max-Age=31536000; SameSite=Strict; HttpOnly")
                ex.responseHeaders.add("Location", "/")
                return send(ex, 303, "text/plain", ByteArray(0))
            }
            val cookie = ex.requestHeaders["Cookie"].orEmpty().flatMap { it.split(';') }.map { it.trim() }
                .firstOrNull { it.startsWith("ah_key=") }?.removePrefix("ah_key=")
            if (cookie == null || !same(cookie, key)) return send(ex, 401, "text/html; charset=utf-8", DENIED)
            val r = api.handle(ex.requestURI.rawPath, query, ex.requestMethod)
            r.headers.forEach { (k, v) -> ex.responseHeaders.set(k, v) }
            send(ex, r.status, r.type, r.body)
        } catch (_: Exception) {
            runCatching { send(ex, 500, "text/plain; charset=utf-8", "Something went wrong on the computer.".toByteArray()) }
        } finally {
            ex.close()
        }
    }

    private fun nex(ex: HttpExchange, path: String, q: Map<String, String>) {
        val json = "application/json; charset=utf-8"
        fun err(message: String) = buildJsonObject { put("error", message) }
        when {
            path == "/api/nex/ping" -> send(ex, 200, json, buildJsonObject {
                put("name", AppInfo.NAME); put("version", AppInfo.VERSION); put("captions", captions != null); put("video", video?.available == true)
            }.toString().toByteArray())
            path == "/api/nex/captions" -> stream(ex) { line ->
                val engine = captions ?: return@stream line(err("Auto captions aren’t available on this computer."))
                val options = runCatching { Json.decodeFromString(AsrOptions.serializer(), q["options"].orEmpty()) }.getOrDefault(AsrOptions())
                runBlocking {
                    engine.transcribe(q["url"].orEmpty(), q["start"]?.toLongOrNull() ?: 0L, q["duration"]?.toLongOrNull() ?: 0L, options,
                        status = { m, p -> line(buildJsonObject { put("status", m); put("progress", p) }) },
                        onCues = { cues -> line(buildJsonObject { putJsonArray("cues") {
                            cues.forEach { c -> addJsonObject { put("start", c.start); put("end", c.end); put("text", c.text); put("speaker", c.speaker) } } } }) })
                }
            }
            path == "/api/nex/video/feed" -> {
                val v = video?.takeIf { it.available } ?: return send(ex, 503, json, err("Install yt-dlp on this computer to play videos as audio.").toString().toByteArray())
                val info = runBlocking { v.feed(q["url"].orEmpty(), q["sid"].orEmpty()) }
                send(ex, 200, json, Json.encodeToString(FeedInfo.serializer(), info).toByteArray())
            }
            path == "/api/nex/video/search" -> {
                val hits = video?.takeIf { it.available }?.let { v -> runBlocking { v.search(q["q"].orEmpty(), q["limit"]?.toIntOrNull() ?: 6) } }.orEmpty()
                send(ex, 200, json, Json.encodeToString(ListSerializer(VideoHit.serializer()), hits).toByteArray())
            }
            path == "/api/nex/video/prepare" -> stream(ex) { line ->
                val v = video?.takeIf { it.available } ?: return@stream line(err("Install yt-dlp on this computer to play videos as audio."))
                val uri = runBlocking { v.audio(q["url"].orEmpty()) { m, p -> line(buildJsonObject { put("status", m); put("progress", p) }) } }
                line(buildJsonObject { put("file", File(java.net.URI(uri)).name) })
            }
            path.startsWith("/api/nex/file/") -> file(ex, path.removePrefix("/api/nex/file/"))
            else -> send(ex, 404, "text/plain; charset=utf-8", "Not found.".toByteArray())
        }
    }

    /** A reply sent a line of JSON at a time, as the work goes on. */
    private fun stream(ex: HttpExchange, body: ((JsonObject) -> Unit) -> Unit) {
        ex.responseHeaders.set("Content-Type", "application/x-ndjson; charset=utf-8")
        ex.sendResponseHeaders(200, 0)
        val out = ex.responseBody
        val line: (JsonObject) -> Unit = { o -> synchronized(out) { out.write((o.toString() + "\n").toByteArray()); out.flush() } }
        try {
            body(line)
        } catch (e: Exception) {
            runCatching { line(buildJsonObject { put("error", e.message ?: "Something went wrong on the computer.") }) }
        }
        runCatching { out.close() }
    }

    /** A prepared video's audio, with byte ranges so the phone can seek. */
    private fun file(ex: HttpExchange, name: String) {
        val f = File(videoDir, name)
        if (!Regex("""^[\w.-]+$""").matches(name) || !f.isFile) return send(ex, 404, "text/plain; charset=utf-8", "Not found.".toByteArray())
        val total = f.length()
        val range = ex.requestHeaders.getFirst("Range")?.let { Regex("""bytes=(\d*)-(\d*)""").find(it) }
        var start = 0L
        var end = total - 1
        if (range != null) {
            range.groupValues[1].toLongOrNull()?.let { start = it }
            range.groupValues[2].toLongOrNull()?.let { end = minOf(it, total - 1) }
        }
        val length = (end - start + 1).coerceAtLeast(0)
        ex.responseHeaders.set("Content-Type", if (name.endsWith(".m4a") || name.endsWith(".mp4")) "audio/mp4" else "application/octet-stream")
        ex.responseHeaders.set("Accept-Ranges", "bytes")
        if (range != null) ex.responseHeaders.set("Content-Range", "bytes $start-$end/$total")
        ex.sendResponseHeaders(if (range != null) 206 else 200, length)
        java.io.RandomAccessFile(f, "r").use { raf ->
            raf.seek(start)
            val buf = ByteArray(64 * 1024)
            var left = length
            ex.responseBody.use { out ->
                while (left > 0) {
                    val n = raf.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                    if (n < 0) break
                    out.write(buf, 0, n); left -= n
                }
            }
        }
    }

    private fun send(ex: HttpExchange, status: Int, type: String, body: ByteArray) {
        ex.responseHeaders.set("Content-Type", type)
        ex.responseHeaders.set("X-Content-Type-Options", "nosniff")
        ex.responseHeaders.set("Referrer-Policy", "no-referrer")
        val head = ex.requestMethod.equals("HEAD", ignoreCase = true)
        ex.sendResponseHeaders(status, if (head || body.isEmpty()) -1 else body.size.toLong())
        if (!head && body.isNotEmpty()) ex.responseBody.use { it.write(body) }
    }

    private companion object {
        val DENIED = """<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>AfterhoursNex</title>
<body style="font: 17px/1.5 system-ui, sans-serif; background: #141218; color: #e6e1e5; padding: 32px; max-width: 34em; margin: auto">
<h2 style="font-weight: 600">This link isn’t valid any more</h2>
<p>Open the current link from AfterhoursNex on your computer, under Settings › Use on your phone.</p>
<p lang="zh-Hans">此链接已失效。请在电脑上的 AfterhoursNex 中打开“设置 › 在手机上使用”，使用当前链接。</p>
<p lang="ja">このリンクは無効になりました。コンピュータの AfterhoursNex の「設定 › スマートフォンで使う」にある現在のリンクを開いてください。</p></body>""".toByteArray()

        fun newKey(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(18).also { SecureRandom().nextBytes(it) })
        fun same(a: String, b: String) = MessageDigest.isEqual(a.toByteArray(), b.toByteArray())

        /** The address phones can reach this computer on: the one used for the default route, else any private IPv4 address. */
        fun lanAddress(): InetAddress? {
            runCatching {
                DatagramSocket().use { s ->
                    s.connect(InetAddress.getByName("192.0.2.1"), 9)        // picks a route; nothing is sent
                    val a = s.localAddress
                    if (a is Inet4Address && !a.isAnyLocalAddress && !a.isLoopbackAddress) return a
                }
            }
            return NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { it.inetAddresses.toList() }.firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
        }

        fun qr(text: String): List<BooleanArray>? = runCatching {
            val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
            List(m.height) { y -> BooleanArray(m.width) { x -> m.get(x, y) } }
        }.getOrNull()
    }
}
