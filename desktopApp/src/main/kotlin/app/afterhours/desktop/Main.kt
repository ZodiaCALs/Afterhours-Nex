package app.afterhours.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.afterhours.core.AppInfo
import app.afterhours.core.AudioPlayer
import app.afterhours.core.CacheItem
import app.afterhours.core.LocalAudio
import app.afterhours.core.AUDIO_FILES
import app.afterhours.core.LanguagePacks
import app.afterhours.core.Video
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.FrameWindowScope
import app.afterhours.core.CaptionEngine
import app.afterhours.core.Storage
import app.afterhours.PhoneServer
import app.afterhours.ui.App
import app.afterhours.ui.PhoneLink
import app.afterhours.ui.PickedFile
import app.afterhours.ui.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.charset.Charset
import java.util.TimeZone
import javax.imageio.ImageIO

/** AfterhoursNex on Windows, macOS and Linux: the shared app in a native window. */
fun main() = application {
    val platform = remember { DesktopPlatform() }
    val icon = remember { DesktopPlatform::class.java.getResourceAsStream("/icon.png")?.use { BitmapPainter(ImageIO.read(it).toComposeImageBitmap()) } }
    val mac = platform.name == "macOS"
    Window(
        onCloseRequest = { platform.close(); exitApplication() },
        title = AppInfo.NAME,
        icon = icon,
        state = rememberWindowState(width = 1200.dp, height = 820.dp),
        undecorated = !mac,                          // borderless: the app draws its own window buttons
    ) {
        if (mac) {                                   // macOS: keep its buttons, over a title bar that's part of the app
            window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
            window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
            window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
        }
        window.minimumSize = java.awt.Dimension(720, 560)
        platform.frame = this
        platform.onClose = { platform.close(); exitApplication() }
        App(platform)
    }
}

class DesktopPlatform : Platform {
    override val name: String = System.getProperty("os.name").let {
        when { it.startsWith("Mac") -> "macOS"; it.startsWith("Win") -> "Windows"; else -> "Linux" }
    }
    private val dir = dataDir()
    override val storage: Storage = FileStorage(dir)
    // The light FFmpeg player when the bundled FFmpeg can open https itself; otherwise JavaFX, loaded only then.
    private val audio: AudioPlayer = if (FfmpegPlayer.usable) FfmpegPlayer() else JavaFxPlayer(File(dir, "converted"))
    override val player = audio
    override val captions: CaptionEngine = WhisperEngine(dir)
    private val video = YtDlp(File(dir, "video"), File(dir, "tools")).also { Video.bridge = it }
    private val phoneServer = PhoneServer(dir, captions, video, File(dir, "video"))
    override val videoTool: String? get() = video.path
    override val packs: LanguagePacks = ArgosPacks()
    var frame: FrameWindowScope? = null
    var onClose: () -> Unit = {}
    override val chromeTop: Dp get() = if (name == "macOS") 28.dp else 0.dp
    override val windowButtons: Boolean get() = name != "macOS"
    override fun minimizeWindow() { frame?.window?.extendedState = java.awt.Frame.ICONIFIED }
    override fun toggleMaximizeWindow() {
        val w = frame?.window ?: return
        w.maximizedBounds = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds   // keep the taskbar visible
        w.extendedState = if (w.extendedState and java.awt.Frame.MAXIMIZED_BOTH != 0) java.awt.Frame.NORMAL else java.awt.Frame.MAXIMIZED_BOTH
    }
    override fun closeWindow() = onClose()
    @Composable override fun DragArea(modifier: Modifier) { frame?.WindowDraggableArea(modifier) { Box(Modifier.fillMaxSize()) } }
    @Composable override fun ResizeEdges() { val w = frame?.window ?: return; if (name != "macOS") ResizeHandles(w) }
    override val phone: PhoneLink get() = phoneServer
    override fun locale(): String = java.util.Locale.getDefault().toLanguageTag()

    override fun openUrl(url: String) {
        try { Desktop.getDesktop().browse(URI(url)) } catch (e: Exception) { /* no browser to open it with */ }
    }
    override fun now(): Long = System.currentTimeMillis()
    override fun utcOffsetMinutes(): Int = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
    override fun decode(bytes: ByteArray, charset: String): String? = try { String(bytes, Charset.forName(charset)) } catch (e: Exception) { null }
    fun close() { phoneServer.close(); when (val a = audio) { is FfmpegPlayer -> a.close(); is JavaFxPlayer -> a.close() } }

    override fun cacheItems(): List<CacheItem> = listOf(
        folder("converted", "Converted audio", File(dir, "converted"), "Opus, Ogg and FLAC episodes, converted so they can play here.", everyday = true),
        folder("video", "Video audio", File(dir, "video"), "Audio fetched from video sites; fetched again when you play them.", everyday = true),
        folder("models", "Speech models", File(dir, "models"), "Used for auto captions; downloaded again when needed.", everyday = false),
    )

    override fun clearCache(id: String): Boolean = when (id) {
        "converted" -> empty(File(dir, "converted"))
        "video" -> empty(File(dir, "video"))
        "models" -> empty(File(dir, "models"))
        else -> false
    }

    private fun folder(id: String, label: String, f: File, note: String, everyday: Boolean): CacheItem {
        val files = f.walkTopDown().filter { it.isFile }.toList()
        return CacheItem(id, label, files.sumOf { it.length() }, files.size, note, everyday)
    }

    /** Deletes what's in a folder; anything in use stays, and is cleaned next time. */
    private fun empty(f: File): Boolean = f.listFiles()?.map { it.deleteRecursively() }?.all { it } ?: true

    override suspend fun openFile(extensions: List<String>): PickedFile? = withContext(Dispatchers.Swing) {
        val dialog = FileDialog(null as Frame?, "Choose a file", FileDialog.LOAD)
        dialog.setFilenameFilter { _, n -> extensions.any { n.lowercase().endsWith(".$it") } }
        dialog.isVisible = true
        val name = dialog.file ?: return@withContext null
        val file = File(dialog.directory, name)
        PickedFile(file.name, file.readBytes())
    }

    override suspend fun saveImage(name: String, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        runCatching { File(File(dir, "covers").apply { mkdirs() }, name).apply { writeBytes(bytes) }.toURI().toString() }.getOrNull()
    }

    override suspend fun openAudio(): List<LocalAudio> = withContext(Dispatchers.Swing) {
        val dialog = FileDialog(null as Frame?, "Import audio files", FileDialog.LOAD)
        dialog.isMultipleMode = true
        dialog.setFilenameFilter { _, n -> AUDIO_FILES.any { n.lowercase().endsWith(".$it") } }
        dialog.isVisible = true
        dialog.files.orEmpty().filter { it.isFile }.map { LocalAudio(it.toURI().toString(), it.name, it.length()) }
    }

    override suspend fun saveFile(name: String, bytes: ByteArray): Boolean = withContext(Dispatchers.Swing) {
        val dialog = FileDialog(null as Frame?, "Save", FileDialog.SAVE)
        dialog.file = name
        dialog.isVisible = true
        val chosen = dialog.file ?: return@withContext false
        File(dialog.directory, chosen).writeBytes(bytes)
        true
    }

    private fun dataDir(): File {
        val home = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> File(System.getenv("APPDATA") ?: home, "AfterhoursNex")
            os.contains("mac") -> File(home, "Library/Application Support/AfterhoursNex")
            else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "afterhoursnex")
        }
    }
}

/** Library files in the user's data folder, written safely (to a temporary file first). */
class FileStorage(private val dir: File) : Storage {
    init { dir.mkdirs() }
    private fun file(name: String) = File(dir, name.replace(Regex("[^A-Za-z0-9._-]"), "_"))
    override fun read(name: String): String? = file(name).takeIf { it.exists() }?.readText()
    override fun write(name: String, text: String) {
        val target = file(name)
        val tmp = File(dir, target.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) { target.delete(); tmp.renameTo(target) }
    }
    override fun delete(name: String) { file(name).delete() }
    override fun list(prefix: String): List<String> = dir.listFiles()?.map { it.name }?.filter { it.startsWith(prefix) && !it.endsWith(".tmp") } ?: emptyList()
    override fun bytes(name: String): Long = file(name).length()
}
