package app.afterhours.desktop

import java.io.File
import java.net.JarURLConnection
import java.security.MessageDigest

/**
 * FFmpeg, shipped with the app as a separate program, for audio formats JavaFX can't play and for speech recognition.
 * It comes from JAVE's platform package; when that's missing, the system's own ffmpeg is used.
 */
object Ffmpeg {
    val path: String by lazy { bundled() ?: "ffmpeg" }

    private fun bundled(): String? = try {
        val root = "ws/schild/jave/nativebin/"
        val loader = Ffmpeg::class.java.classLoader
        val dir = File(System.getProperty("java.io.tmpdir"), "afterhoursnex-ffmpeg").apply { mkdirs() }
        val conn = loader.getResource(root)?.openConnection() as? JarURLConnection
        val jar = conn?.jarFile
        val entry = jar?.entries()?.asSequence()?.firstOrNull { !it.isDirectory && it.name.startsWith(root) && it.name.substringAfterLast('/').startsWith("ffmpeg") }
        if (jar != null && entry != null) {
            val out = File(dir, entry.name.substringAfterLast('/'))
            if (out.length() != entry.size) jar.getInputStream(entry).use { i -> out.outputStream().use { i.copyTo(it) } }
            out.setExecutable(true)
            out.path
        } else null
    } catch (e: Exception) {
        null
    }

    private val UNSUPPORTED = Regex("\\.(opus|ogg|oga|flac|webm|weba|mka|wma|amr|ape|wv|mpc|spx|caf|ac3)(\\?|#|$)", RegexOption.IGNORE_CASE)
    fun needsConversion(url: String) = UNSUPPORTED.containsMatchIn(url)

    /** Converts an episode to AAC (which JavaFX plays) and keeps the six most recent conversions. */
    fun convertForPlayback(url: String, dir: File, cancelled: () -> Boolean): File? {
        dir.mkdirs()
        val name = MessageDigest.getInstance("SHA-1").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }.take(20)
        val out = File(dir, "$name.m4a")
        if (out.length() > 0) { out.setLastModified(System.currentTimeMillis()); return out }
        val part = File(dir, "$name.part")
        val p = try {
            ProcessBuilder(path, "-nostdin", "-hide_banner", "-loglevel", "error", "-y", "-i", url, "-vn", "-c:a", "aac", "-b:a", "160k", "-f", "mp4", part.path)
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
        } catch (e: Exception) {
            return null
        }
        while (p.isAlive) {
            if (cancelled()) { p.destroyForcibly(); part.delete(); return null }
            Thread.sleep(150)
        }
        if (p.exitValue() != 0 || part.length() == 0L || !part.renameTo(out)) { part.delete(); return null }
        dir.listFiles { f -> f.name.endsWith(".m4a") }?.sortedByDescending { it.lastModified() }?.drop(6)?.forEach { it.delete() }
        return out
    }
}
