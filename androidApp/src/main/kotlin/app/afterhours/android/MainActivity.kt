package app.afterhours.android

import app.afterhours.core.CaptionEngine
import app.afterhours.core.Video
import app.afterhours.core.Paired
import app.afterhours.core.RemoteVideo
import app.afterhours.core.RemoteCaptions
import app.afterhours.core.LocalAudio
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import app.afterhours.core.AudioPlayer
import app.afterhours.core.Storage
import app.afterhours.ui.App
import app.afterhours.ui.PickedFile
import app.afterhours.ui.Platform
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset
import java.util.TimeZone

/** AfterhoursNex on Android: the shared app, with Media3 playing the audio. */
class MainActivity : ComponentActivity() {
    private val opener = registerForActivityResult(ActivityResultContracts.OpenDocument()) { AndroidPlatform.get(this).opened(it) }
    private val saver = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { AndroidPlatform.get(this).saved(it) }
    private val audioPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { AndroidPlatform.get(this).pickedAudio(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)   // so the playback controls can show
        }
        val platform = AndroidPlatform.get(applicationContext)
        platform.launchOpen = { types -> opener.launch(types) }
        platform.launchSave = { name -> saver.launch(name) }
        platform.launchAudio = { audioPicker.launch(arrayOf("audio/*")) }
        setContent {
            App(platform) { enabled, onBack -> BackHandler(enabled = enabled, onBack = onBack) }
        }
    }
}

class AndroidPlatform private constructor(private val context: Context) : Platform {
    override val name = "Android"
    override fun locale(): String = java.util.Locale.getDefault().toLanguageTag()
    override val storage: Storage = FileStorage(File(context.filesDir, "afterhours"))
    override val player: AudioPlayer = AndroidPlayer(context)

    override fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // no browser to open it with
        }
    }

    override fun now(): Long = System.currentTimeMillis()
    override fun utcOffsetMinutes(): Int = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
    override fun decode(bytes: ByteArray, charset: String): String? = try { String(bytes, Charset.forName(charset)) } catch (e: Exception) { null }

    // The system's file pickers, started by the activity
    var launchOpen: ((Array<String>) -> Unit)? = null
    var launchSave: ((String) -> Unit)? = null
    private var openRequest: CompletableDeferred<Uri?>? = null
    private var saveRequest: CompletableDeferred<Uri?>? = null
    fun opened(uri: Uri?) { openRequest?.complete(uri); openRequest = null }
    fun saved(uri: Uri?) { saveRequest?.complete(uri); saveRequest = null }

    override suspend fun openFile(extensions: List<String>): PickedFile? {
        val launch = launchOpen ?: return null
        val request = CompletableDeferred<Uri?>()
        withContext(Dispatchers.Main) { openRequest = request; launch(arrayOf("*/*")) }
        val uri = request.await() ?: return null
        return withContext(Dispatchers.IO) {
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                ?: uri.lastPathSegment ?: "file"
            context.contentResolver.openInputStream(uri)?.use { PickedFile(name, it.readBytes()) }
        }
    }

    // Audio files from this phone: kept readable after a restart, so they play again later
    var launchAudio: (() -> Unit)? = null
    private var audioRequest: CompletableDeferred<List<Uri>>? = null
    fun pickedAudio(uris: List<Uri>) { audioRequest?.complete(uris); audioRequest = null }

    override suspend fun saveImage(name: String, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        runCatching { val f = java.io.File(java.io.File(context.filesDir, "covers").apply { mkdirs() }, name); f.writeBytes(bytes); Uri.fromFile(f).toString() }.getOrNull()
    }

    override suspend fun openAudio(): List<LocalAudio> {
        val launch = launchAudio ?: return emptyList()
        val request = CompletableDeferred<List<Uri>>()
        withContext(Dispatchers.Main) { audioRequest = request; launch() }
        val uris = request.await()
        return withContext(Dispatchers.IO) {
            uris.map { uri ->
                runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                val info = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                    if (c.moveToFirst()) (c.getString(0) ?: "Audio") to c.getLong(1) else null
                }
                LocalAudio(uri.toString(), info?.first ?: uri.lastPathSegment ?: "Audio", info?.second ?: 0L)
            }
        }
    }

    // Auto captions and video podcasts, made on the computer this phone is paired with (Settings › Your computer)
    private val remoteCaptions = RemoteCaptions()
    override val captions: CaptionEngine? get() = if (Paired.ready) remoteCaptions else null
    init { Video.bridge = RemoteVideo() }

    override suspend fun saveFile(name: String, bytes: ByteArray): Boolean {
        val launch = launchSave ?: return false
        val request = CompletableDeferred<Uri?>()
        withContext(Dispatchers.Main) { saveRequest = request; launch(name) }
        val uri = request.await() ?: return false
        return withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes); true } ?: false }
    }

    companion object {
        @Volatile private var instance: AndroidPlatform? = null
        fun get(context: Context): AndroidPlatform = instance ?: synchronized(this) { instance ?: AndroidPlatform(context.applicationContext).also { instance = it } }
    }
}

/** Library files in the app's private storage, written safely (to a temporary file first). */
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
