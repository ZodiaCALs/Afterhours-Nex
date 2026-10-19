package app.afterhours.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.afterhours.core.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** What each platform provides: where to keep things, how to play audio, and how to open links. */
interface Platform {
    val name: String
    val storage: Storage
    val player: AudioPlayer
    fun openUrl(url: String)
    fun now(): Long
    fun utcOffsetMinutes(): Int
    fun decode(bytes: ByteArray, charset: String): String? = null
    /** On-device speech recognition, where the platform has it. */
    val captions: CaptionEngine? get() = null
    /** Lets the listener choose a file (with one of [extensions]). Returns null when they cancel. */
    suspend fun openFile(extensions: List<String>): PickedFile? = null
    /** Lets the listener save [bytes], suggesting [name]. Returns false when they cancel. */
    suspend fun saveFile(name: String, bytes: ByteArray): Boolean = false
    /** The device's language, such as "en-GB", "zh-CN" or "ja-JP". */
    fun locale(): String = "en"
    /** Lets phones on the same Wi-Fi use this computer's AfterhoursNex (desktop only). */
    val phone: PhoneLink? get() = null
}

data class PhoneState(val on: Boolean = false, val url: String = "", val error: String = "", val qr: List<BooleanArray>? = null)

interface PhoneLink {
    val state: kotlinx.coroutines.flow.StateFlow<PhoneState>
    fun setEnabled(on: Boolean)
    /** A new private link; phones using the old one are signed out. */
    fun renew()
}

class PickedFile(val name: String, val bytes: ByteArray)

sealed interface Screen {
    data object Home : Screen
    data object Discover : Screen
    data object Shelf : Screen
    data object Search : Screen
    data object Prefs : Screen
    data class ShowPage(val sid: String) : Screen
}

class AppState(val platform: Platform, val scope: CoroutineScope) {
    /** Feeds, the directory and playback work off the main thread; one failed task never stops the others. */
    private val work = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> println("Afterhours: ${e.message}") })
    val library: Library
    val playback: Playback
    val stack = mutableStateListOf<Screen>(Screen.Home)
    var nowPlaying by mutableStateOf(false)
    var searchTerm by mutableStateOf("")
    val snackbar = SnackbarHostState()

    init {
        Clock.now = { platform.now() }
        Clock.utcOffsetMinutes = { platform.utcOffsetMinutes() }
        Net.charsetDecoder = { bytes, charset -> platform.decode(bytes, charset) }
        library = Library(platform.storage, work)
        library.load()
        playback = Playback(platform.player, library, work)
        playback.engine = platform.captions
    }

    val screen: Screen get() = stack.last()
    fun tab(s: Screen) { stack.clear(); stack.add(s) }
    fun open(s: Screen) { stack.add(s) }
    fun back(): Boolean = when {
        nowPlaying -> { nowPlaying = false; true }
        stack.size > 1 -> { stack.removeAt(stack.lastIndex); true }
        else -> false
    }
    fun say(text: String) { scope.launch { snackbar.showSnackbar(text) } }
}

private data class Tab(val screen: Screen, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Screen.Home, "Listen Now", Icons.Filled.Home),
    Tab(Screen.Discover, "Discover", Icons.Filled.Explore),
    Tab(Screen.Shelf, "Library", Icons.Filled.LibraryMusic),
    Tab(Screen.Search, "Search", Icons.Filled.Search),
    Tab(Screen.Prefs, "Settings", Icons.Filled.Settings),
)

/**
 * The whole app. [backHandler] lets the platform hook up its back button (Android's, for example).
 */
@Composable
fun App(platform: Platform, backHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit = { _, _ -> }) {
    val scope = rememberCoroutineScope()
    val app = remember { AppState(platform, scope) }
    LaunchedEffect(Unit) { app.library.startSetup() }
    val settings by app.library.settings.collectAsState()
    var intro by remember { mutableStateOf(app.library.settings.value.intro) }
    val lang = I18n.pick(settings.language, platform.locale())
    I18n.lang = lang
    key(lang) { AfterhoursTheme(settings.theme) {
        backHandler(app.nowPlaying || app.stack.size > 1) { app.back() }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                if (maxWidth >= 720.dp) {
                    Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                        NavigationRail {
                            Spacer(Modifier.height(12.dp))
                            TABS.forEach { t ->
                                NavigationRailItem(selected = app.stack.first() == t.screen, onClick = { app.tab(t.screen) },
                                    icon = { Icon(t.icon, contentDescription = null) }, label = { Text(tr(t.label), maxLines = 1) })
                            }
                        }
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            Box(Modifier.weight(1f)) { Content(app) }
                            MiniPlayer(app)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) { Content(app) }
                        MiniPlayer(app)
                        NavigationBar {
                            TABS.forEach { t ->
                                NavigationBarItem(selected = app.stack.first() == t.screen, onClick = { app.tab(t.screen) },
                                    icon = { Icon(t.icon, contentDescription = null) }, label = { Text(tr(t.label), maxLines = 1) })
                            }
                        }
                    }
                }
                SnackbarHost(app.snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp))
                AnimatedVisibility(visible = app.nowPlaying, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                    NowPlaying(app)
                }
                if (intro) Intro { intro = false }
            }
        }
    } }
}

@Composable
private fun Content(app: AppState) {
    when (val s = app.screen) {
        Screen.Home -> HomeScreen(app)
        Screen.Discover -> DiscoverScreen(app)
        Screen.Shelf -> ShelfScreen(app)
        Screen.Search -> SearchScreen(app)
        Screen.Prefs -> SettingsScreen(app)
        is Screen.ShowPage -> ShowScreen(app, s.sid)
    }
}

@Composable
private fun MiniPlayer(app: AppState) {
    val ep by app.playback.current.collectAsState()
    val st by app.playback.state.collectAsState()
    val e = ep ?: return
    val show = app.library.show(e.sid)
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            if (st.durationMs > 0) {
                LinearProgressIndicator(progress = { (st.positionMs.toFloat() / st.durationMs).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(2.dp))
            }
            Row(Modifier.fillMaxWidth().clickable { app.nowPlaying = true }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(e.image.ifBlank { show?.image.orEmpty() }, show?.title ?: e.title, Modifier.size(44.dp), corner = 8.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(e.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(show?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { app.playback.skip(-15) }) { Icon(Icons.Filled.FastRewind, contentDescription = tr("Back 15 seconds")) }
                IconButton(onClick = { app.playback.toggle() }) {
                    Icon(if (st.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = tr(if (st.playing) "Pause" else "Play"))
                }
            }
        }
    }
}
