package app.afterhours.core

import kotlinx.serialization.Serializable

/** A show on the shelf. Episodes come from its feed, or from its website when there's no working feed. */
@Serializable
data class Show(
    val sid: String,
    val title: String,
    val author: String = "",
    val image: String = "",
    val feedUrl: String = "",
    val pageUrl: String = "",
    val source: String = "rss",          // "rss" or "web"
    val cat: String = "mine",
    val preset: String? = null,
    val status: String = "ready",        // "pending", "ready" or "missing"
    val description: String = "",
    val link: String = "",
    val addedAt: Long = 0,
)

@Serializable
data class Episode(
    val id: String,
    val sid: String,
    val title: String,
    val url: String,
    val date: Long = 0,                  // milliseconds since 1970, 0 when unknown
    val duration: Long = 0,              // seconds, 0 when unknown
    val image: String = "",
    val desc: String = "",
    val guid: String = "",
    val transcript: String = "",         // a transcript the feed links to, for captions later
)

@Serializable
data class Progress(val t: Long = 0, val d: Long = 0, val at: Long = 0, val done: Boolean = false)

@Serializable
data class Settings(
    val speed: Float = 1f,
    val autoNext: Boolean = true,
    val theme: String = "system",        // "system", "light" or "dark"
    val sort: String = "newest",         // "newest" or "oldest"
    val intro: Boolean = true,           // the opening animation
    val captions: Boolean = true,        // show the current caption line
    val autoCaptions: Boolean = true,    // caption episodes that have none (where the platform can)
    val captionModel: String = "auto",   // "auto", "base", "small" or "medium"
    val pro: Boolean = false,            // use the detailed options below
    val asr: AsrOptions = AsrOptions(),
    val language: String = "system",    // "system", "en", "zh-Hans" or "ja"
)

data class FeedInfo(
    val title: String,
    val author: String,
    val image: String,
    val description: String,
    val link: String,
    val episodes: List<Episode>,
)

class FetchException(message: String) : Exception(message)

object AppInfo {
    const val NAME = "AfterhoursNex"
    const val VERSION = "1.0.0"
    const val SLOGAN = "How many times do I have left to see you again in my life?"
}

/** The time, supplied by the platform so the core stays platform-free (and tests can set it). */
object Clock {
    var now: () -> Long = { 0L }
    var utcOffsetMinutes: () -> Int = { 0 }
}

/** A short, stable id: the same feed item always gets the same id. */
fun stableId(prefix: String, text: String): String {
    var h = 0x811c9dc5.toInt()
    for (c in text) { h = h xor c.code; h *= 0x01000193 }
    return prefix + (h.toLong() and 0xffffffffL).toString(36)
}

fun episodeId(sid: String, key: String) = stableId("e-", "$sid|$key")
