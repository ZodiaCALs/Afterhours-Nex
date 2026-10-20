package app.afterhours.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/** One caption line; times in milliseconds. */
@Serializable
data class Cue(val start: Long, val end: Long, val text: String, val speaker: String = "")

/** Settings for speech recognition (Pro mode shows them all). */
@Serializable
data class AsrOptions(
    val model: String = "base",        // tiny, base, small, medium, large-v3-turbo, large-v3
    val language: String = "auto",     // or a code such as "en", "ja", "zh"
    val translate: Boolean = false,    // write the captions in English, whatever is spoken
    val threads: Int = 0,              // 0 lets AfterhoursNex decide
    val beam: Int = 1,                 // 1 is fastest; more is more careful
    val temperature: Float = 0f,
    val noSpeech: Float = 0.6f,        // how sure the model must be that a stretch is silent
    val maxChars: Int = 84,            // the longest caption line
    val prompt: String = "",           // names and words to expect
)

data class CaptionState(
    val cues: List<Cue> = emptyList(),
    val source: String = "",           // "feed", "file" or "auto"
    val status: String = "",
    val progress: Float = -1f,         // 0 to 1 while auto captions are made, otherwise -1
    val working: Boolean = false,
    val error: String = "",
)

/** A platform's on-device speech recognition (Whisper on desktop). */
interface CaptionEngine {
    val models: List<Pair<String, String>>          // model name and download size
    fun hasModel(name: String): Boolean
    /**
     * Recognises the speech in the episode at [url] from [startMs] on, calling [onCues] as captions are ready.
     * [status] reports what's happening and the progress (0 to 1, or -1 when unknown). Stops when cancelled.
     */
    suspend fun transcribe(url: String, startMs: Long, durationMs: Long, options: AsrOptions,
                           status: (String, Float) -> Unit, onCues: (List<Cue>) -> Unit)
}

/** Reads captions and transcripts: WebVTT, SubRip, and the podcast namespace's JSON. */
object Captions {
    private val TIMING = Regex("((?:\\d+:)?\\d{1,2}:\\d{2}[.,]\\d{1,3})\\s*-->\\s*((?:\\d+:)?\\d{1,2}:\\d{2}[.,]\\d{1,3})")

    fun parse(text: String, hint: String = ""): List<Cue> {
        val t = text.trimStart('\uFEFF').trim()
        val cues = if (t.startsWith("{") || t.startsWith("[") || hint.contains("json", ignoreCase = true)) json(t) else timed(t)
        return cues.filter { it.text.isNotBlank() && it.end >= it.start }.sortedBy { it.start }
    }

    private fun stamp(s: String): Long {
        var seconds = 0.0
        for (part in s.replace(',', '.').split(':')) seconds = seconds * 60 + (part.toDoubleOrNull() ?: 0.0)
        return (seconds * 1000).toLong()
    }

    private fun clean(s: String) = decodeEntities(s.replace(Regex("<[^>]+>"), "")).replace(Regex("\\s+"), " ").trim()

    private fun timed(t: String): List<Cue> {
        val out = ArrayList<Cue>()
        for (block in t.replace("\r\n", "\n").replace('\r', '\n').split(Regex("\n\\s*\n"))) {
            val lines = block.lines()
            val i = lines.indexOfFirst { TIMING.containsMatchIn(it) }
            if (i < 0) continue
            val m = TIMING.find(lines[i]) ?: continue
            val raw = lines.drop(i + 1).joinToString(" ").trim()
            var speaker = Regex("^<v(?:\\.[^\\s>]+)?\\s+([^>]+)>").find(raw)?.groupValues?.get(1)?.trim().orEmpty()
            var body = clean(raw)
            // A speaker label in capitals, as caption files write them ("HOST: Good evening."); "Today: …" is just a sentence.
            if (speaker.isEmpty()) Regex("^([A-Z][A-Z0-9 .'-]{1,30}):\\s+").find(body)?.let { speaker = it.groupValues[1].trim(); body = body.substring(it.range.last + 1) }
            out.add(Cue(stamp(m.groupValues[1]), stamp(m.groupValues[2]), body, speaker))
        }
        return out
    }

    private fun json(t: String): List<Cue> {
        val root = try { Json.parseToJsonElement(t) } catch (e: Exception) { return emptyList() }
        val segments = ((root as? JsonObject)?.get("segments") as? JsonArray) ?: (root as? JsonArray) ?: return emptyList()
        return segments.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            fun num(k: String) = (o[k] as? JsonPrimitive)?.doubleOrNull
            fun str(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull
            val start = num("startTime") ?: num("start") ?: return@mapNotNull null
            Cue((start * 1000).toLong(), ((num("endTime") ?: num("end") ?: start) * 1000).toLong(), clean(str("body") ?: str("text").orEmpty()), str("speaker").orEmpty())
        }
    }

    /** Joins word-by-word transcripts into readable lines. */
    fun lines(cues: List<Cue>, maxChars: Int = 84, maxMs: Long = 6500): List<Cue> {
        val out = ArrayList<Cue>()
        for (c in cues) {
            val last = out.lastOrNull()
            if (last != null && last.speaker == c.speaker && last.text.length + c.text.length < maxChars && c.end - last.start < maxMs &&
                c.start - last.end < 1500 && !last.text.endsWith(".") && !last.text.endsWith("?") && !last.text.endsWith("!")) {
                out[out.lastIndex] = last.copy(end = c.end, text = last.text + " " + c.text)
            } else out.add(c)
        }
        return out
    }

    /** The cue showing at [ms], or -1. */
    fun at(cues: List<Cue>, ms: Long): Int {
        var lo = 0
        var hi = cues.size - 1
        var found = -1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (cues[mid].start <= ms) { found = mid; lo = mid + 1 } else hi = mid - 1
        }
        return if (found >= 0 && ms <= cues[found].end + 1500) found else -1
    }
}
