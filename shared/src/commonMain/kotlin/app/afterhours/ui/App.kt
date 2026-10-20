package app.afterhours.ui

import coil3.SingletonImageLoader
import coil3.PlatformContext
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.afterhours.core.*
import app.afterhours.resources.Res
import app.afterhours.resources.afterhours_icon
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource


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
    /** Caches only this platform keeps (on desktop: converted audio and speech models). */
    fun cacheItems(): List<CacheItem> = emptyList()
    fun clearCache(id: String): Boolean = false
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
    /** The playing episode's id, and whether it's playing: they change rarely, so lists can follow them cheaply. */
    val currentId: StateFlow<String?>
    val playingNow: StateFlow<Boolean>
    val stack = mutableStateListOf<Screen>(Screen.Home)
    var nowPlaying by mutableStateOf(false)
    var searchTerm by mutableStateOf("")
    val snackbar = SnackbarHostState()
    val translator = Translator(platform.storage)

    /** Translates the lines around the one being spoken, in the background. */
    fun translateAhead(cues: List<Cue>, active: Int, target: String, s: Settings) {
        if (cues.isEmpty()) return
        val from = (active - 1).coerceAtLeast(0)
        val lines = cues.subList(from.coerceAtMost(cues.size), (from + 6).coerceAtMost(cues.size)).map { it.text }
        work.launch { translator.ensure(lines, target, s) }
    }

    /** Everything that can be cleaned, with its size. */
    fun cacheItems(ctx: PlatformContext): List<CacheItem> {
        val st = platform.storage
        fun stored(id: String, label: String, prefix: String, note: String, everyday: Boolean): CacheItem {
            val names = st.list(prefix)
            return CacheItem(id, label, names.sumOf { st.bytes(it) }, names.size, note, everyday)
        }
        val art = runCatching { SingletonImageLoader.get(ctx).diskCache?.size }.getOrNull() ?: 0L
        return listOf(
            CacheItem("art", "Cover art", art, -1, "Downloaded again as you browse.", true),
            stored("episodes", "Episode lists", "eps-", "Downloaded again from each show’s feed.", true),
            stored("translations", "Caption translations", "tr-", "Translated again when needed.", true),
            stored("captions", "Saved captions", "cap-", "Transcripts, your caption files and auto captions, kept so they load instantly.", false),
        ) + platform.cacheItems()
    }

    fun clearCache(id: String, ctx: PlatformContext): Boolean = when (id) {
        "art" -> runCatching { SingletonImageLoader.get(ctx).let { it.diskCache?.clear(); it.memoryCache?.clear() } }.isSuccess
        "episodes" -> { library.clearEpisodeCache(); true }
        "translations" -> { translator.clear(); true }
        "captions" -> { library.clearSavedCaptions(); true }
        else -> platform.clearCache(id)
    }

    init {
        Clock.now = { platform.now() }
        Clock.utcOffsetMinutes = { platform.utcOffsetMinutes() }
        Net.charsetDecoder = { bytes, charset -> platform.decode(bytes, charset) }
        library = Library(platform.storage, work)
        library.load()
        playback = Playback(platform.player, library, work)
        playback.engine = platform.captions
        currentId = playback.current.map { it?.id }.distinctUntilChanged().stateIn(work, SharingStarted.Eagerly, playback.current.value?.id)
        playingNow = playback.state.map { it.playing }.distinctUntilChanged().stateIn(work, SharingStarted.Eagerly, false)
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
    Tab(Screen.Home, "Listen Now", WebIcons.Home),
    Tab(Screen.Discover, "Discover", WebIcons.Sparkle),
    Tab(Screen.Shelf, "Library", WebIcons.Stack),
    Tab(Screen.Search, "Search", WebIcons.Search),
)

/**
 * The whole app, laid out like the Afterhours web app: a sidebar and a floating player bar on wide windows,
 * a tab bar and a mini player on phones. [backHandler] lets the platform hook up its back button (Android's, for example).
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
        val playing by app.playback.current.collectAsState()
        val d36 = with(LocalDensity.current) { 36.dp.roundToPx() }
        BoxWithConstraints(Modifier.fillMaxSize().background(look.bg)) {
            if (maxWidth >= 720.dp) {
                Row(Modifier.fillMaxSize()) {
                    Sidebar(app, Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)))
                    BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical))) {
                        val pageWidth = maxWidth
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Page(app, compact = false, width = pageWidth, bottom = if (playing != null) 104.dp else 0.dp)
                        }
                        androidx.compose.animation.AnimatedVisibility(playing != null, Modifier.align(Alignment.BottomCenter),
                            enter = slideInVertically(tween(560, easing = Motion.EaseOut)) { it + d36 } + fadeIn(tween(320)),
                            exit = slideOutVertically(tween(260, easing = Motion.EaseIn)) { it + d36 } + fadeOut(tween(200))) { PlayerBar(app, Modifier) }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
                        Page(app, compact = true, width = maxWidth, bottom = if (playing != null) 80.dp else 0.dp)
                        androidx.compose.animation.AnimatedVisibility(playing != null, Modifier.align(Alignment.BottomCenter),
                            enter = slideInVertically(tween(520, easing = Motion.EaseOut)) { it + d36 } + fadeIn(tween(300)),
                            exit = slideOutVertically(tween(240, easing = Motion.EaseIn)) { it + d36 } + fadeOut(tween(200))) { MiniPlayer(app, Modifier) }
                    }
                    TabBar(app)
                }
            }
            SnackbarHost(app.snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp))
            androidx.compose.animation.AnimatedVisibility(visible = app.nowPlaying,
                enter = fadeIn(tween(280, easing = Motion.EaseOut)) + slideInVertically(tween(520, easing = Motion.EaseOut)) { d36 } +
                    scaleIn(tween(520, easing = Motion.EaseOut), initialScale = 0.985f),
                exit = fadeOut(tween(200, easing = Motion.EaseIn)) + slideOutVertically(tween(240, easing = Motion.EaseIn)) { d36 } +
                    scaleOut(tween(240, easing = Motion.EaseIn), targetScale = 0.985f)) {
                NowPlaying(app)
            }
            if (intro) Intro { intro = false }
        }
    } }
}

/** A page, with the web app's margins: 4% of the page's width, between 20 and 48 points. */
@Composable
private fun Page(app: AppState, compact: Boolean, width: Dp, bottom: Dp) {
    val rise = with(LocalDensity.current) { 22.dp.roundToPx() }
    val w = width.coerceAtMost(1320.dp)
    CompositionLocalProvider(LocalGutter provides (w * 0.04f).coerceIn(20.dp, 48.dp), LocalCompact provides compact, LocalBottomSpace provides bottom) {
        AnimatedContent(targetState = app.screen, label = "page", modifier = Modifier.fillMaxHeight().widthIn(max = 1320.dp).fillMaxWidth(), transitionSpec = {
            if (targetState is Screen.ShowPage && initialState !is Screen.ShowPage)
                (fadeIn(tween(380, easing = Motion.EaseOut)) + scaleIn(tween(380, easing = Motion.EaseOut), initialScale = 0.965f, transformOrigin = TransformOrigin(0.5f, 0f)))
                    .togetherWith(fadeOut(tween(120, easing = Motion.EaseIn)))
            else (fadeIn(tween(460, easing = Motion.EaseOut)) + slideInVertically(tween(460, easing = Motion.EaseOut)) { rise })
                .togetherWith(fadeOut(tween(110, easing = Motion.EaseIn)))
        }) { screen -> Content(app, screen) }
    }
}

@Composable
private fun Content(app: AppState, screen: Screen) {
    when (val s = screen) {
        Screen.Home -> HomeScreen(app)
        Screen.Discover -> DiscoverScreen(app)
        Screen.Shelf -> ShelfScreen(app)
        Screen.Search -> SearchScreen(app)
        Screen.Prefs -> SettingsScreen(app)
        is Screen.ShowPage -> ShowScreen(app, s.sid)
    }
}

/** The web app's sidebar: brand, search, the three pages, your shows, auto captions and Settings. */
@Composable
private fun Sidebar(app: AppState, modifier: Modifier) {
    val shows by app.library.shows.collectAsState()
    val settings by app.library.settings.collectAsState()
    var term by remember { mutableStateOf("") }
    val root = app.stack.first()
    val current = app.screen
    Column(modifier.width(248.dp).fillMaxHeight().background(look.side).padding(horizontal = 12.dp)) {
        Row(Modifier.padding(start = 8.dp, top = 20.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(Res.drawable.afterhours_icon), contentDescription = null, modifier = Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)))
            Spacer(Modifier.width(9.dp))
            val accent = look.accent
            Text(buildAnnotatedString { append("Afterhours"); withStyle(SpanStyle(color = accent)) { append("Nex") } }, style = Type.brand, color = look.text)
        }
        SearchBox(term, { term = it }, tr("Search or paste a link"), onSubmit = {
            if (term.isNotBlank()) { app.searchTerm = term.trim(); app.tab(Screen.Search) }
        }, modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp))
        Spacer(Modifier.height(12.dp))
        TABS.take(3).forEach { t -> NavItem(tr(t.label), t.icon, root == t.screen) { app.tab(t.screen) } }
        Text(tr("Your shows"), style = Type.navHead, color = look.text3, modifier = Modifier.padding(start = 10.dp, top = 22.dp, bottom = 6.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(shows, key = { it.sid }) { s ->
                val on = (current as? Screen.ShowPage)?.sid == s.sid
                val ia = remember { MutableInteractionSource() }
                Row(Modifier.fillMaxWidth().height(33.dp).pressable(ia, 0.98f).clip(RoundedCornerShape(8.dp)).background(if (on) look.fill2 else Color.Transparent)
                    .hoverFill(ia, RoundedCornerShape(8.dp), look.fill)
                    .clickable(ia, null) { if (app.screen is Screen.ShowPage) app.stack.removeAt(app.stack.lastIndex); app.open(Screen.ShowPage(s.sid)) }
                    .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Artwork(s.image, s.title, Modifier.size(24.dp), corner = 6.dp, shadow = false)
                    Spacer(Modifier.width(10.dp))
                    Text(s.title, style = Type.nav.copy(fontWeight = FontWeight.Normal), color = if (s.status == "ready") look.text else look.text2,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Separator(Modifier.padding(vertical = 8.dp))
        Row(Modifier.fillMaxWidth().height(40.dp).padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(WebIcons.Wave, contentDescription = null, tint = look.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(tr("Auto captions"), style = Type.nav.copy(fontSize = 13.5.sp), color = look.text, modifier = Modifier.weight(1f))
            Toggle(settings.autoCaptions, { on -> app.library.updateSettings { it.copy(autoCaptions = on) } }, Modifier.scale(0.78f))
        }
        NavItem(tr("Settings"), WebIcons.Gear, root == Screen.Prefs, muted = true) { app.tab(Screen.Prefs) }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, muted: Boolean = false, onClick: () -> Unit) {
    val ia = remember { MutableInteractionSource() }
    val bg by animateColorAsState(if (selected) look.fill2 else look.fill2.copy(alpha = 0f), tween(220, easing = Motion.EaseStd), label = "nav")
    val bar by animateDpAsState(if (selected) 16.dp else 0.dp, tween(420, easing = Motion.EaseOut), label = "nav")
    Box(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Row(Modifier.fillMaxWidth().height(36.dp).pressable(ia, 0.98f).clip(RoundedCornerShape(8.dp)).background(bg).hoverFill(ia, RoundedCornerShape(8.dp), look.fill)
            .clickable(ia, null, onClick = onClick).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (muted) look.text2 else look.accent, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(11.dp))
            Text(label, style = Type.nav, color = look.text, maxLines = 1)
        }
        if (bar > 0.dp) Box(Modifier.align(Alignment.CenterStart).offset(x = (-5).dp).size(3.dp, bar).clip(RoundedCornerShape(2.dp)).background(look.accent))
    }
}

/** The floating player bar of the desktop web app. */
@Composable
private fun PlayerBar(app: AppState, modifier: Modifier) {
    val ep by app.playback.current.collectAsState()
    val st by app.playback.state.collectAsState()
    val settings by app.library.settings.collectAsState()
    val e = ep ?: return
    val show = app.library.show(e.sid)
    val shape = RoundedCornerShape(16.dp)
    Row(modifier.padding(horizontal = 20.dp, vertical = 14.dp).widthIn(max = 1180.dp).fillMaxWidth().height(72.dp)
        .shadow(22.dp, shape, ambientColor = Color(0x42000000), spotColor = Color(0x42000000)).clip(shape).background(look.pop)
        .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(0.32f).clip(RoundedCornerShape(10.dp)).clickable { app.nowPlaying = true }.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(e.image.ifBlank { show?.image.orEmpty() }, show?.title ?: e.title, Modifier.size(48.dp), corner = 8.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text(e.title, style = Type.cardTitle.copy(fontSize = 14.sp), color = look.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(show?.title.orEmpty(), style = Type.cardSub.copy(fontSize = 12.sp), color = look.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(Modifier.weight(0.4f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBox(tr("Back 15 seconds"), { app.playback.skip(-15) }) { SkipIcon(true, 15, look.text, 22.dp) }
                PlayCircle(st.playing, st.buffering, 36.dp, look.text, look.bg) { app.playback.toggle() }
                IconBox(tr("Forward 30 seconds"), { app.playback.skip(30) }) { SkipIcon(false, 30, look.text, 22.dp) }
            }
            Scrubber(st.positionMs, st.durationMs, { app.playback.seek(it) }, look.fill2, look.text2, look.text3, Modifier.fillMaxWidth(), inline = true)
        }
        Row(Modifier.weight(0.28f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            SpeedButton(app, st.speed, look.text)
            RoundButton(WebIcons.Cc, tr("Captions"), { app.library.updateSettings { it.copy(captions = !it.captions) } }, size = 32.dp,
                bg = Color.Transparent, tint = if (settings.captions) look.accent else look.text2)
            SleepButton(app, look.text2)
            RoundButton(WebIcons.Expand, tr("Open Now Playing"), { app.nowPlaying = true }, size = 32.dp, bg = Color.Transparent, tint = look.text2)
        }
    }
}

/** The phone's mini player, floating above the tab bar. */
@Composable
private fun MiniPlayer(app: AppState, modifier: Modifier) {
    val ep by app.playback.current.collectAsState()
    val st by app.playback.state.collectAsState()
    val e = ep ?: return
    val show = app.library.show(e.sid)
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.padding(horizontal = 8.dp, vertical = 6.dp).fillMaxWidth().height(64.dp)
        .shadow(18.dp, shape, ambientColor = Color(0x42000000), spotColor = Color(0x42000000)).clip(shape).background(look.pop).clickable { app.nowPlaying = true }) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(e.image.ifBlank { show?.image.orEmpty() }, show?.title ?: e.title, Modifier.size(46.dp), corner = 8.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(e.title, style = Type.cardTitle.copy(fontSize = 16.sp), color = look.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(show?.title.orEmpty(), style = Type.cardSub, color = look.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            PlayCircle(st.playing, st.buffering, 40.dp, look.text, look.bg) { app.playback.toggle() }
            Spacer(Modifier.width(6.dp))
            IconBox(tr("Forward 30 seconds"), { app.playback.skip(30) }) { SkipIcon(false, 30, look.text, 26.dp) }
        }
        if (st.durationMs > 0) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth((st.positionMs.toFloat() / st.durationMs).coerceIn(0f, 1f)).height(2.dp).background(look.accent))
    }
}

/** The phone's tab bar: translucent, with the selected tab in the accent colour and a small bar above it. */
@Composable
private fun TabBar(app: AppState) {
    val root = app.stack.first()
    Column(Modifier.fillMaxWidth().background(look.bar)) {
        Separator()
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)).height(56.dp)) {
            TABS.forEach { t ->
                val on = root == t.screen
                val ia = remember { MutableInteractionSource() }
                val c by animateColorAsState(if (on) look.accent else look.text2, tween(240, easing = Motion.EaseStd), label = "tab")
                val w by animateDpAsState(if (on) 26.dp else 0.dp, tween(420, easing = Motion.EaseOut), label = "tab")
                Box(Modifier.weight(1f).fillMaxHeight().pressable(ia, 0.92f).clickable(ia, null) { app.tab(t.screen) }, contentAlignment = Alignment.Center) {
                    if (w > 0.dp) Box(Modifier.align(Alignment.TopCenter).size(w, 3.dp).clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)).background(look.accent))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(t.icon, contentDescription = null, tint = c, modifier = Modifier.size(23.dp))
                        Text(tr(t.label), style = Type.tab, color = c, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
    }
}
