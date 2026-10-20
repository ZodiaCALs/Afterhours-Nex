package app.afterhours.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.afterhours.core.*
import kotlinx.coroutines.launch

/** Caption status and actions in Now Playing: what's happening, and loading, making or stopping captions. */
@Composable
fun CaptionBlock(app: AppState) {
    val cap by app.playback.captions.collectAsState()
    val scope = rememberCoroutineScope()
    val engine = app.platform.captions
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        when {
            cap.working -> {
                Text(tr(cap.status), style = Type.cardSub, color = Color.White.copy(alpha = 0.72f))
                Box(Modifier.padding(top = 8.dp).widthIn(max = 320.dp).fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))) {
                    if (cap.progress >= 0f) Box(Modifier.fillMaxWidth(cap.progress.coerceIn(0f, 1f)).fillMaxHeight().background(Color.White))
                }
            }
            cap.error.isNotBlank() -> Text(tr(cap.error), style = Type.cardSub, color = Color(0xFFFF8A8A))
            cap.status.isNotBlank() && cap.cues.isEmpty() -> Text(tr(cap.status), style = Type.cardSub, color = Color.White.copy(alpha = 0.6f))
        }
        Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassPill(tr("Load captions"), WebIcons.File) {
                scope.launch {
                    val f = app.platform.openFile(listOf("vtt", "srt", "json")) ?: return@launch
                    app.say(tr(if (app.playback.useCaptionFile(f.name, f.bytes)) "Loaded captions from ${f.name}." else "That file has no timed captions I can read."))
                }
            }
            if (engine != null) when {
                cap.working && cap.source == "auto" -> GlassPill(tr("Stop"), Icons.Filled.Stop) { app.playback.stopCaptions() }
                cap.cues.isEmpty() -> GlassPill(tr("Caption this episode"), WebIcons.Wave) { app.playback.autoCaption() }
                cap.source == "auto" -> GlassPill(tr("Redo captions"), WebIcons.Refresh) { app.playback.autoCaption() }
            }
        }
        if (cap.source.isNotBlank() && cap.cues.isNotEmpty()) {
            Text(tr(when (cap.source) { "feed" -> "Captions from the show’s transcript"; "file" -> "Captions from your file"; else -> "Auto captions, made on this computer" }),
                style = Type.meta, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun GlassPill(text: String, icon: ImageVector?, onClick: () -> Unit) {
    val ia = remember { MutableInteractionSource() }
    Row(Modifier.pressable(ia).height(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).hoverFill(ia, CircleShape, Color.White.copy(alpha = 0.1f))
        .clickable(ia, null, onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, style = Type.pill, color = Color.White, maxLines = 1)
    }
}

/**
 * The captions as the web app rolls them: every line in a list whose edges fade out, the spoken one bright and
 * full-size, the others dimmed and a touch smaller. The list glides each new line to a third of the way down;
 * scroll it yourself and it waits until you've been still for a few seconds. Tap a line to go there.
 */
@Composable
fun Lyrics(app: AppState, cues: List<Cue>, modifier: Modifier = Modifier, compact: Boolean = false) {
    val state = app.playback.state.collectAsState()
    val active by remember(cues) { derivedStateOf { Captions.at(cues, state.value.positionMs) } }
    val settings by app.library.settings.collectAsState()
    val version by app.translator.version.collectAsState()
    val target = if (settings.translateTo == "auto") I18n.lang else settings.translateTo
    LaunchedEffect(active.coerceAtLeast(0), settings.translate, target, cues) {
        if (settings.translate) app.translateAhead(cues, active.coerceAtLeast(0), target, settings)
    }
    val list = rememberLazyListState()
    val dragged by list.interactionSource.collectIsDraggedAsState()
    var wasDragged by remember { mutableStateOf(false) }
    var heldUntil by remember { mutableStateOf(0L) }
    LaunchedEffect(dragged) {
        if (dragged) wasDragged = true else if (wasDragged) { heldUntil = Clock.now() + 3500; wasDragged = false }
    }
    BoxWithConstraints(modifier.fadingEdges(0.14f, if (compact) 0.7f else 0.78f)) {
        val lead = maxHeight * (if (compact) 0.3f else 0.32f)
        LaunchedEffect(active, cues) {
            if (active < 0 || dragged || Clock.now() < heldUntil) return@LaunchedEffect
            val info = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == active }
            // item offsets start below the top padding, so 0 puts the line exactly at the lead
            if (info != null) list.animateScrollBy(info.offset.toFloat(), tween(680, easing = Motion.EaseOut)) else list.scrollToItem(active)
        }
        LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = lead, bottom = maxHeight * (if (compact) 0.6f else 0.45f))) {
            itemsIndexed(cues, key = { i, _ -> i }) { i, cue ->
                val speaker = cue.speaker.isNotBlank() && cue.speaker != cues.getOrNull(i - 1)?.speaker
                val translated = if (settings.translate && version >= 0) app.translator.get(cue.text.trim(), target)?.takeIf { it != cue.text.trim() } else null
                LyricLine(cue, speaker, i == active, compact, translated) { app.playback.seek(cue.start) }
            }
        }
    }
}

@Composable
private fun LyricLine(cue: Cue, speaker: Boolean, active: Boolean, compact: Boolean, translated: String?, onClick: () -> Unit) {
    val ia = remember { MutableInteractionSource() }
    val hovered by ia.collectIsHoveredAsState()
    val color by animateColorAsState(when { active -> Color.White; hovered -> Color.White.copy(alpha = 0.58f); else -> Color.White.copy(alpha = 0.32f) },
        tween(420, easing = Motion.EaseStd), label = "lyric")
    val scale by animateFloatAsState(if (active) 1f else 0.975f, tween(520, easing = Motion.EaseOut), label = "lyric")
    val second by animateColorAsState(if (active) Color.White.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.24f), tween(420, easing = Motion.EaseStd), label = "lyric")
    Column(Modifier.fillMaxWidth().padding(bottom = if (compact) 18.dp else 22.dp).animateContentSize(tween(420, easing = Motion.EaseOut))
        .graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(0f, 0.5f) }
        .hoverable(ia).clickable(ia, null, onClick = onClick)) {
        if (speaker) Text(cue.speaker, style = Type.meta.copy(fontSize = 13.sp, letterSpacing = 0.02.em), color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(bottom = 4.dp))
        Text(cue.text, color = color, modifier = Modifier.widthIn(max = 600.dp),
            style = TextStyle(fontSize = if (compact) 22.sp else 30.sp, lineHeight = if (compact) 28.sp else 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.015).em))
        if (translated != null) Text(translated, color = second, modifier = Modifier.padding(top = 6.dp).widthIn(max = 600.dp),
            style = TextStyle(fontSize = if (compact) 16.sp else 19.sp, lineHeight = if (compact) 21.sp else 25.sp, fontWeight = FontWeight.SemiBold))
    }
}

/** Fades the top and bottom of a scrolling list into the background, like the web's mask-image. */
private fun Modifier.fadingEdges(top: Float, bottom: Float): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(Brush.verticalGradient(0f to Color.Transparent, top to Color.Black, bottom to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
    }

@Composable
fun SwitchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Toggle(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChoiceRow(title: String, sub: String?, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(title)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState())) { Choice(options, selected, onSelect) }
    }
}

/** Settings › Captions, including Pro mode. */
@Composable
fun CaptionSettings(app: AppState) {
    val s by app.library.settings.collectAsState()
    val engine = app.platform.captions
    SectionTitle(tr("Captions"), null)
    SwitchRow(tr("Show captions"), tr("The current line in Now Playing."), s.captions) { on -> app.library.updateSettings { it.copy(captions = on) } }
    if (engine == null) {
        Text(tr("Auto captions need the desktop app, on Windows, macOS or Linux. Shows’ own transcripts and caption files work here."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        return
    }
    SwitchRow(tr("Auto captions"), tr("When an episode has no captions, recognise the speech on this computer with Whisper. The speech model downloads once."), s.autoCaptions) { on ->
        app.library.updateSettings { it.copy(autoCaptions = on) }
    }
    if (!s.pro) {
        ChoiceRow(tr("Recognition quality"), tr("Automatic uses base for most shows and small for Japanese and Chinese ones."),
            listOf("auto" to tr("Automatic"), "base" to tr("Fast"), "small" to tr("Accurate"), "medium" to tr("Most accurate")), s.captionModel) { v -> app.library.updateSettings { it.copy(captionModel = v) } }
    }
    SwitchRow(tr("Pro mode"), tr("Choose the speech model and fine-tune how it listens."), s.pro) { on -> app.library.updateSettings { it.copy(pro = on) } }
    if (s.pro) {
        val o = s.asr
        fun set(change: (AsrOptions) -> AsrOptions) = app.library.updateSettings { it.copy(asr = change(it.asr)) }
        ChoiceRow(tr("Model"), tr("Bigger models are more accurate and slower."), engine.models.map { (name, size) -> name to "$name · $size" + if (engine.hasModel(name)) " ✓" else "" }, o.model) { v -> set { it.copy(model = v) } }
        ChoiceRow(tr("Spoken language"), tr("Automatic detects it from the episode."), listOf("auto" to tr("Automatic"), "en" to tr("English"), "ja" to tr("日本語"), "zh" to tr("中文"), "ko" to tr("한국어"),
            "fr" to tr("Français"), "de" to tr("Deutsch"), "es" to tr("Español"), "it" to tr("Italiano"), "pt" to tr("Português")), o.language) { v -> set { it.copy(language = v) } }
        SwitchRow(tr("Translate into English"), tr("Write the captions in English, whatever language is spoken."), o.translate) { on -> set { it.copy(translate = on) } }
        ChoiceRow(tr("Processor threads"), null, listOf("0" to tr("Automatic"), "2" to tr("2"), "4" to tr("4"), "8" to tr("8"), "12" to tr("12")), o.threads.toString()) { v -> set { it.copy(threads = v.toInt()) } }
        ChoiceRow(tr("Beam size"), tr("How many guesses the model weighs at once. 1 is fastest; 5 is more careful."), listOf("1" to tr("1"), "2" to tr("2"), "5" to tr("5"), "8" to tr("8")), o.beam.toString()) { v -> set { it.copy(beam = v.toInt()) } }
        ChoiceRow(tr("Temperature"), tr("Above 0 adds variety. 0 gives the steadiest results."), listOf("0.0" to tr("0"), "0.2" to tr("0.2"), "0.4" to tr("0.4")), o.temperature.toString()) { v -> set { it.copy(temperature = v.toFloat()) } }
        ChoiceRow(tr("No-speech threshold"), tr("How sure the model must be that a stretch is silent before skipping it."), listOf("0.4" to tr("0.4"), "0.6" to tr("0.6"), "0.8" to tr("0.8")), o.noSpeech.toString()) { v -> set { it.copy(noSpeech = v.toFloat()) } }
        ChoiceRow(tr("Longest line"), null, listOf("42" to tr("42 characters"), "64" to tr("64"), "84" to tr("84"), "120" to tr("120")), o.maxChars.toString()) { v -> set { it.copy(maxChars = v.toInt()) } }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(tr("Vocabulary hint"))
            Text(tr("Names and words to expect, such as hosts, guests or places."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = o.prompt, onValueChange = { v -> set { it.copy(prompt = v.take(400)) } }, singleLine = true,
                placeholder = { Text(tr("e.g. Agatha Christie, Hercule Poirot")) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        Row(Modifier.padding(horizontal = 8.dp)) {
            TextButton(onClick = { app.library.updateSettings { it.copy(asr = AsrOptions()) }; app.say(tr("Pro settings reset.")) }) { Text(tr("Reset to defaults")) }
            TextButton(onClick = { if (app.playback.current.value == null) app.say(tr("Start an episode first.")) else app.playback.autoCaption() }) { Text(tr("Re-caption this episode")) }
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(tr("Saved auto captions"))
            Text(tr("Kept on this computer, so they load instantly next time."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { val n = app.library.clearSavedCaptions(); app.say(tr(if (n > 0) "Deleted $n saved captions." else "There were no saved captions.")) }) { Text(tr("Delete")) }
    }
}
