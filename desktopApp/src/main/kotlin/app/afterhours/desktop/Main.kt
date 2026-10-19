package app.afterhours.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.afterhours.core.AppInfo
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
    Window(
        onCloseRequest = { platform.close(); exitApplication() },
        title = AppInfo.NAME,
        icon = icon,
        state = rememberWindowState(width = 1200.dp, height = 820.dp),
    ) {
        App(platform)
    }
}

class DesktopPlatform : Platform {
    override val name: String = System.getProperty("os.name").let {
        when { it.startsWith("Mac") -> "macOS"; it.startsWith("Win") -> "Windows"; else -> "Linux" }
    }
    private val dir = dataDir()
    override val storage: Storage = FileStorage(dir)
    private val audio = JavaFxPlayer(File(dir, "converted"))
    override val player = audio
    override val captions: CaptionEngine = WhisperEngine(dir)
    private val phoneServer = PhoneServer(dir)
    override val phone: PhoneLink get() = phoneServer
    override fun locale(): String = java.util.Locale.getDefault().toLanguageTag()

    override fun openUrl(url: String) {
        try { Desktop.getDesktop().browse(URI(url)) } catch (e: Exception) { /* no browser to open it with */ }
    }
    override fun now(): Long = System.currentTimeMillis()
    override fun utcOffsetMinutes(): Int = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
    override fun decode(bytes: ByteArray, charset: String): String? = try { String(bytes, Charset.forName(charset)) } catch (e: Exception) { null }
    fun close() { phoneServer.close(); audio.close() }

    override suspend fun openFile(extensions: List<String>): PickedFile? = withContext(Dispatchers.Swing) {
        val dialog = FileDialog(null as Frame?, "Choose a file", FileDialog.LOAD)
        dialog.setFilenameFilter { _, n -> extensions.any { n.lowercase().endsWith(".$it") } }
        dialog.isVisible = true
        val name = dialog.file ?: return@withContext null
        val file = File(dialog.directory, name)
        PickedFile(file.name, file.readBytes())
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
}
