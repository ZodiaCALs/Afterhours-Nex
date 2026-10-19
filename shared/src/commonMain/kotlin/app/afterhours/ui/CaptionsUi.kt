package app.afterhours.ui

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
import androidx.compose.ui.window.Dialog
import app.afterhours.core.*
import kotlinx.coroutines.launch

/** The caption line in Now Playing, with the transcript and caption actions. */
@Composable
fun CaptionBlock(app: AppState, positionMs: Long) {
    val cap by app.playback.captions.collectAsState()
    val settings by app.library.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var transcript by remember { mutableStateOf(false) }
    val engine = app.platform.captions
    Column(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val i = Captions.at(cap.cues, positionMs)
        when {
            settings.captions && i >= 0 -> {
                val cue = cap.cues[i]
                if (cue.speaker.isNotBlank()) Text(cue.speaker, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(cue.text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            cap.working -> {
                Text(tr(cap.status), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                if (cap.progress >= 0f) LinearProgressIndicator(progress = { cap.progress }, modifier = Modifier.width(240.dp).padding(top = 6.dp))
                else LinearProgressIndicator(Modifier.width(240.dp).padding(top = 6.dp))
            }
            cap.error.isNotBlank() -> Text(tr(cap.error), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            cap.status.isNotBlank() -> Text(tr(cap.status), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            if (cap.cues.isNotEmpty()) TextButton(onClick = { transcript = true }) { Text(tr("Transcript")) }
            TextButton(onClick = {
                scope.launch {
                    val f = app.platform.openFile(listOf("vtt", "srt", "json")) ?: return@launch
                    app.say(tr(if (app.playback.useCaptionFile(f.name, f.bytes)) "Loaded captions from ${f.name}." else "That file has no timed captions I can read."))
                }
            }) { Text(tr("Load captions")) }
            if (engine != null) {
                when {
                    cap.working && cap.source == "auto" -> TextButton(onClick = { app.playback.stopCaptions() }) { Text(tr("Stop")) }
                    cap.cues.isEmpty() -> TextButton(onClick = { app.playback.autoCaption() }) { Text(tr("Caption this episode")) }
                    cap.source == "auto" -> TextButton(onClick = { app.playback.autoCaption() }) { Text(tr("Redo captions")) }
                }
            }
        }
        if (cap.source.isNotBlank() && cap.cues.isNotEmpty()) {
            Text(tr(when (cap.source) { "feed" -> tr("Captions from the show’s transcript"); "file" -> tr("Captions from your file"); else -> tr("Auto captions, made on this computer") }),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (transcript) TranscriptDialog(app, cap.cues, positionMs) { transcript = false }
}

@Composable
private fun TranscriptDialog(app: AppState, cues: List<Cue>, positionMs: Long, onClose: () -> Unit) {
    val list = rememberLazyListState()
    val current = Captions.at(cues, positionMs)
    LaunchedEffect(current) { if (current >= 0) list.animateScrollToItem((current - 2).coerceAtLeast(0)) }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Transcript"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text(tr("Done")) }
                }
                LazyColumn(state = list, contentPadding = PaddingValues(12.dp)) {
                    itemsIndexed(cues) { i, cue ->
                        val on = i == current
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(if (on) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                            .clickable { app.playback.seek(cue.start) }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(tr(fmtClock(cue.start / 1000) + if (cue.speaker.isNotBlank()) " · " + cue.speaker else ""), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(cue.text, style = MaterialTheme.typography.bodyMedium, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwitchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
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
