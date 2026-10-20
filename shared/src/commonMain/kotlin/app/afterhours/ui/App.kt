package app.afterhours.ui

import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.ImageLoader
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
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
    /** Translation language packs on this device (desktop, with Argos Translate installed). */
    val packs: LanguagePacks? get() = null
    /** Where yt-dlp is, when video sites can be played as audio (desktop). */
    val videoTool: String? get() = null
    /** Room the window's own buttons take at the top (macOS's, over the sidebar). */
    val chromeTop: Dp get() = 0.dp
    /** True when the app draws its own minimise, maximise and close buttons (a borderless window). */
    val windowButtons: Boolean get() = false
    fun minimizeWindow() {}
    fun toggleMaximizeWindow() {}
    fun closeWindow() {}
    /** Where dragging moves the window. */
    @Composable fun DragArea(modifier: Modifier) {}
    /** The edges of a borderless window, for resizing it. */
    @Composable fun ResizeEdges() {}
    /** Keeps an image (a cover) in the app’s own folder, and returns its address. */
    suspend fun saveImage(name: String, bytes: ByteArray): String? = null
    /** Lets the listener choose audio files to import (MP3, Ogg, FLAC and more). */
    suspend fun openAudio(): List<LocalAudio> = emptyList()
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
    data object Stations : Screen
    /** One episode from a search, before deciding: its cover, show, notes, and Play. */
    data class EpisodePreview(val ep: Episode, val show: String, val feedUrl: String = "") : Screen
    /** A show to look at before adding it: its feed, or a directory search (and website) to find one. */
    data class Preview(val title: String, val feedUrl: String = "", val term: String = "", val country: String = "us", val site: String = "", val image: String = "") : Screen
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

    // ---- updates: a new release on GitHub ----
    private val _update = kotlinx.coroutines.flow.MutableStateFlow<Release?>(null)
    val update: StateFlow<Release?> = _update
    var updateNote by mutableStateOf("")

    /** Looks for a newer release, at most about once a day unless [force]d. */
    fun checkUpdates(force: Boolean) {
        val s = library.settings.value
        if (!force && (!s.checkUpdates || Clock.now() - s.lastUpdateCheck < 20 * 3_600_000L)) return
        val repo = s.updateRepo.ifBlank { AppInfo.REPO }
        if (repo.isBlank()) { if (force) updateNote = "Add the GitHub repository AfterhoursNex is released from."; return }
        updateNote = if (force) "Checking…" else ""
        work.launch {
            try {
                val r = Updates.latest(repo, platform.name)
                library.updateSettings { it.copy(lastUpdateCheck = Clock.now()) }
                _update.value = r?.takeIf { Updates.newer(it.version, AppInfo.VERSION) && (force || it.version != s.skippedVersion) }
                updateNote = if (_update.value == null) "AfterhoursNex is up to date." else ""
            } catch (e: Exception) {
                updateNote = e.message ?: "Couldn’t check for updates."
            }
        }
    }

    fun skipUpdate() {
        val r = _update.value ?: return
        library.updateSettings { it.copy(skippedVersion = r.version) }
        _update.value = null
    }
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
        Paired.link = library.settings.value.companion
        Covers.attach(platform.storage)
        work.launch { library.settings.collect { s -> Paired.link = s.companion; playback.engine = platform.captions } }
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
    fun say(text: String) { scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(text) } }

    /** A message with an Undo button. */
    fun sayUndo(text: String, undo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(text, actionLabel = tr("Undo"), duration = androidx.compose.material3.SnackbarDuration.Long) ==
                androidx.compose.material3.SnackbarResult.ActionPerformed) undo()
        }
    }
}

private data class Tab(val screen: Screen, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Screen.Home, "Listen Now", WebIcons.Home),
    Tab(Screen.Discover, "Discover", WebIcons.Sparkle),
    Tab(Screen.Shelf, "Library", WebIcons.Stack),
    Tab(Screen.Stations, "Radio", WebIcons.Radio),
    Tab(Screen.Search, "Search", WebIcons.Search),
)

/**
 * The whole app, laid out like the Afterhours web app: a sidebar and a floating player bar on wide windows,
 * a tab bar and a mini player on phones. [backHandler] lets the platform hook up its back button (Android's, for example).
 */
@Composable
fun App(platform: Platform, backHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit = { _, _ -> }) {
    setSingletonImageLoaderFactory { ctx -> ImageLoader.Builder(ctx).memoryCache { MemoryCache.Builder().maxSizePercent(ctx, 0.04).build() }.build() }
    val scope = rememberCoroutineScope()
    val app = remember { AppState(platform, scope) }
    LaunchedEffect(Unit) { app.library.startSetup(); app.checkUpdates(force = false) }
    val settings by app.library.settings.collectAsState()
    var intro by remember { mutableStateOf(app.library.settings.value.intro) }
    val lang = I18n.pick(settings.language, platform.locale())
    I18n.lang = lang
    key(lang) { AfterhoursTheme(settings.theme) {
        backHandler(app.nowPlaying || app.stack.size > 1) { app.back() }
        val backdrop = remember { Backdrop() }
        CompositionLocalProvider(LocalGlass provides settings.glass.coerceIn(0f, 1f), LocalBackdrop provides if (settings.lessEffects) null else backdrop) {
        val playing by app.playback.current.collectAsState()
        val d36 = with(LocalDensity.current) { 36.dp.roundToPx() }
        val glass = settings.glass.coerceIn(0f, 1f)
        val glow = if (settings.lessEffects) Color.Transparent else look.accent.copy(alpha = (if (look.dark) 0.04f else 0.02f) + 0.08f * glass)
        BoxWithConstraints(Modifier.fillMaxSize().background(look.bg)
            .background(Brush.radialGradient(listOf(glow, Color.Transparent), center = Offset(0f, 0f), radius = 1500f))
            .then(if (platform.windowButtons) Modifier.border(0.6.dp, look.sep) else Modifier)) {
            if (maxWidth >= 720.dp) {
                Row(Modifier.fillMaxSize()) {
                    Sidebar(app, Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)))
                    Box(Modifier.width(0.6.dp).fillMaxHeight().background(look.sep))
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                    TitleBar(app)
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical))) {
                        val pageWidth = maxWidth
                        Box(Modifier.fillMaxSize().backdropSource(backdrop), contentAlignment = Alignment.TopCenter) {
                            Page(app, compact = false, width = pageWidth, bottom = if (playing != null) 104.dp else 0.dp)
                        }
                        androidx.compose.animation.AnimatedVisibility(playing != null, Modifier.align(Alignment.BottomCenter),
                            enter = slideInVertically(tween(560, easing = Motion.EaseOut)) { it + d36 } + fadeIn(tween(320)),
                            exit = slideOutVertically(tween(260, easing = Motion.EaseIn)) { it + d36 } + fadeOut(tween(200))) { PlayerBar(app, Modifier) }
                    }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    TitleBar(app)
                    var tabHeight by remember { mutableStateOf(0) }
                    val tabSpace = with(LocalDensity.current) { tabHeight.toDp() }
                    // the tab bar and mini player float over the page, frosted, as on iOS
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
                        val pageWidth = maxWidth
                        Box(Modifier.fillMaxSize().backdropSource(backdrop)) {
                            Page(app, compact = true, width = pageWidth, bottom = tabSpace + if (playing != null) 80.dp else 0.dp)
                        }
                        androidx.compose.animation.AnimatedVisibility(playing != null, Modifier.align(Alignment.BottomCenter).padding(bottom = tabSpace),
                            enter = slideInVertically(tween(520, easing = Motion.EaseOut)) { it + d36 } + fadeIn(tween(300)),
                            exit = slideOutVertically(tween(240, easing = Motion.EaseIn)) { it + d36 } + fadeOut(tween(200))) { MiniPlayer(app, Modifier) }
                        Box(Modifier.align(Alignment.BottomCenter).onSizeChanged { tabHeight = it.height }) { TabBar(app) }
                    }
                }
            }
            SnackbarHost(app.snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp)) { data -> GlassSnackbar(data) }
            androidx.compose.animation.AnimatedVisibility(visible = app.nowPlaying,
                enter = fadeIn(tween(280, easing = Motion.EaseOut)) + slideInVertically(tween(520, easing = Motion.EaseOut)) { d36 } +
                    scaleIn(tween(520, easing = Motion.EaseOut), initialScale = 0.985f),
                exit = fadeOut(tween(200, easing = Motion.EaseIn)) + slideOutVertically(tween(240, easing = Motion.EaseIn)) { d36 } +
                    scaleOut(tween(240, easing = Motion.EaseIn), targetScale = 0.985f)) {
                NowPlaying(app)
            }
            if (intro) Intro { intro = false }
            platform.ResizeEdges()
        }
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
            if (targetState.drills() && !initialState.drills())
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
        Screen.Stations -> RadioScreen(app)
        is Screen.Preview -> PreviewScreen(app, s)
        is Screen.EpisodePreview -> EpisodePreviewScreen(app, s)
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
    Column(modifier.width(248.dp).fillMaxHeight().background(look.side.copy(alpha = 1f - 0.45f * LocalGlass.current)).padding(horizontal = 12.dp)) {
        Box(Modifier.fillMaxWidth().height(app.platform.chromeTop)) { app.platform.DragArea(Modifier.fillMaxSize()) }
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
        TABS.filter { it.screen != Screen.Search }.forEach { t -> NavItem(tr(t.label), t.icon, root == t.screen) { app.tab(t.screen) } }
        Text(tr("Your shows"), style = Type.navHead, color = look.text3, modifier = Modifier.padding(start = 10.dp, top = 22.dp, bottom = 6.dp))
        ShowList(app, shows, current, Modifier.weight(1f))
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
        .shadow(22.dp, shape, ambientColor = Color(0x42000000), spotColor = Color(0x42000000)).frosted(shape, look.pop.copy(alpha = 0.8f - 0.42f * LocalGlass.current))
        .border(0.6.dp, look.sep, shape).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(0.32f).clip(RoundedCornerShape(10.dp)).clickable { app.nowPlaying = true }.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(e.image.ifBlank { show?.art.orEmpty() }, show?.name ?: e.title, Modifier.size(48.dp), corner = 8.dp, style = if (e.image.isBlank()) show?.coverStyle.orEmpty() else "")
            Column(Modifier.padding(start = 12.dp)) {
                Text(e.title, style = Type.cardTitle.copy(fontSize = 14.sp), color = look.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(show?.name.orEmpty(), style = Type.cardSub.copy(fontSize = 12.sp), color = look.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(Modifier.weight(0.4f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBox(tr("Back 15 seconds"), { app.playback.skip(-15) }) { SkipIcon(true, 15, look.text, 22.dp) }
                PlayCircle(st.playing, st.buffering, 36.dp, look.text, look.bg) { app.playback.toggle() }
                IconBox(tr("Forward 30 seconds"), { app.playback.skip(30) }) { SkipIcon(false, 30, look.text, 22.dp) }
            }
            val prep by app.playback.preparing.collectAsState()
            val p = prep
            if (p != null) Text(tr(p.first), style = Type.cardSub.copy(fontSize = 12.sp), color = if (p.second == -2f) MaterialTheme.colorScheme.error else look.text2, maxLines = 1)
            else Scrubber(st.positionMs, st.durationMs, { app.playback.seek(it) }, look.fill2, look.text2, look.text3, Modifier.fillMaxWidth(), inline = true)
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
        .shadow(18.dp, shape, ambientColor = Color(0x42000000), spotColor = Color(0x42000000)).frosted(shape, look.pop.copy(alpha = 0.8f - 0.42f * LocalGlass.current))
        .border(0.6.dp, look.sep, shape).clickable { app.nowPlaying = true }) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(e.image.ifBlank { show?.art.orEmpty() }, show?.name ?: e.title, Modifier.size(46.dp), corner = 8.dp, style = if (e.image.isBlank()) show?.coverStyle.orEmpty() else "")
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(e.title, style = Type.cardTitle.copy(fontSize = 16.sp), color = look.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(show?.name.orEmpty(), style = Type.cardSub, color = look.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    Column(Modifier.fillMaxWidth().frosted(androidx.compose.ui.graphics.RectangleShape, look.bg.copy(alpha = 0.76f - 0.4f * LocalGlass.current))) {
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

/** The top of the window: a strip to drag it by, and on Windows and Linux the window's own buttons. */
@Composable
private fun TitleBar(app: AppState) {
    val p = app.platform
    if (!p.windowButtons && p.chromeTop == 0.dp) return
    Row(Modifier.fillMaxWidth().height(if (p.windowButtons) 34.dp else p.chromeTop), verticalAlignment = Alignment.CenterVertically) {
        p.DragArea(Modifier.weight(1f).fillMaxHeight())
        if (p.windowButtons) {
            WindowButton(danger = false, onClick = { p.minimizeWindow() }) { c -> drawLine(c, Offset(size.width * 0.3f, size.height * 0.5f), Offset(size.width * 0.7f, size.height * 0.5f), 1.4f) }
            WindowButton(danger = false, onClick = { p.toggleMaximizeWindow() }) { c ->
                drawRect(c, Offset(size.width * 0.33f, size.height * 0.3f), androidx.compose.ui.geometry.Size(size.width * 0.34f, size.height * 0.4f), style = Stroke(1.4f))
            }
            WindowButton(danger = true, onClick = { p.closeWindow() }) { c ->
                drawLine(c, Offset(size.width * 0.34f, size.height * 0.3f), Offset(size.width * 0.66f, size.height * 0.7f), 1.4f)
                drawLine(c, Offset(size.width * 0.66f, size.height * 0.3f), Offset(size.width * 0.34f, size.height * 0.7f), 1.4f)
            }
        }
    }
}

@Composable
private fun WindowButton(danger: Boolean, onClick: () -> Unit, glyph: androidx.compose.ui.graphics.drawscope.DrawScope.(Color) -> Unit) {
    val ia = remember { MutableInteractionSource() }
    val hovered by ia.collectIsHoveredAsState()
    val bg by animateColorAsState(when { hovered && danger -> Color(0xFFE81123); hovered -> look.fill; else -> look.fill.copy(alpha = 0f) }, tween(140), label = "window")
    val fg = if (hovered && danger) Color.White else look.text2
    Box(Modifier.width(46.dp).fillMaxHeight().background(bg).hoverable(ia).clickable(ia, null, onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(14.dp)) { glyph(fg) }
    }
}

/** Pages that open from a list with the gentle zoom (the web app's drillIn). */
private fun Screen.drills() = this is Screen.ShowPage || this is Screen.Preview || this is Screen.EpisodePreview
