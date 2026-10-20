package app.afterhours.core

import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

// ---- video sites, heard as audio ----

/** YouTube, Bilibili and other video sites, played as audio. A platform sets [Video.bridge] when it can reach them. */
interface VideoBridge {
    val available: Boolean
    /** True for links it can open: a video, a playlist or a channel. */
    fun handles(url: String): Boolean
    /** A channel or playlist as a show, its videos as episodes. */
    suspend fun feed(url: String, sid: String): FeedInfo
    /** The audio of one video, ready to play: a local file, as a file: address. */
    suspend fun audio(url: String, status: (String, Float) -> Unit): String
    /** Videos about a topic. */
    suspend fun search(query: String, limit: Int): List<VideoHit>
    /** Whether the app installs, updates and removes the video tool itself (desktop). */
    val manageable: Boolean get() = false
    val installedByApp: Boolean get() = false
    suspend fun install(progress: (Float) -> Unit): Boolean = false
    suspend fun update(): String = ""
    fun uninstall(): Boolean = false
}

@Serializable
data class VideoHit(val url: String, val title: String, val channel: String, val durationSec: Long, val thumb: String, val site: String)

object Video {
    var bridge: VideoBridge? = null
    fun handles(url: String) = bridge?.let { it.available && it.handles(url) } == true
    fun episode(h: VideoHit) = Episode(id = stableId("v-", h.url), sid = "video", title = h.title, url = h.url, duration = h.durationSec, image = h.thumb, desc = h.channel)
}

// ---- radio: public stations from the Radio Browser directory ----

@Serializable
data class Station(
    val stationuuid: String = "", val name: String = "", val url: String = "", val url_resolved: String = "", val homepage: String = "",
    val favicon: String = "", val tags: String = "", val country: String = "", val countrycode: String = "", val state: String = "",
    val language: String = "", val codec: String = "", val bitrate: Int = 0, val hls: Int = 0, val lastcheckok: Int = 1, val clickcount: Int = 0,
)

object Radio {
    private val servers = listOf("https://de1.api.radio-browser.info", "https://nl1.api.radio-browser.info", "https://at1.api.radio-browser.info", "https://de2.api.radio-browser.info")
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val cache = mutableMapOf<String, List<Station>>()

    /** The most listened-to stations in a country (ISO code such as "JP"), or matching [name]. */
    suspend fun stations(country: String, name: String = "", limit: Int = 60): List<Station> {
        val key = "$country|$name"
        cache[key]?.let { return it }
        val q = buildString {
            append("/json/stations/search?hidebroken=true&order=clickcount&reverse=true&limit=$limit")
            if (country.isNotBlank()) append("&countrycode=").append(country)
            if (name.isNotBlank()) append("&name=").append(name.encodeURLParameter())
        }
        var last: Exception? = null
        for (s in servers) {
            try {
                val f = Net.fetch(s + q, 4 shl 20)
                val list = json.decodeFromString<List<Station>>(Net.decode(f.bytes, f.type))
                    .filter { it.lastcheckok == 1 && (it.url_resolved.ifBlank { it.url }).startsWith("http") }
                    .distinctBy { it.name.trim().lowercase() }
                cache[key] = list
                return list
            } catch (e: Exception) { last = e }
        }
        throw FetchException(last?.message ?: "The radio directory didn’t answer.")
    }

    fun episode(s: Station) = Episode(id = "radio:" + s.stationuuid, sid = "radio", title = s.name.trim(), url = s.url_resolved.ifBlank { s.url },
        image = s.favicon, desc = listOf(s.state, s.tags.split(',').take(3).joinToString(", ")).filter { it.isNotBlank() }.joinToString(" · "))
}

// ---- topic search: single episodes as well as shows ----

data class EpisodeHit(val title: String, val show: String, val url: String, val image: String, val date: Long, val durationSec: Long,
                      val desc: String, val feedUrl: String, val collectionId: Long, val score: Double)

object Topics {
    /** Episodes about a topic from Apple's podcast directory, best matches first. */
    suspend fun episodes(query: String, country: String = "us", limit: Int = 40): List<EpisodeHit> {
        val f = Net.fetch("https://itunes.apple.com/search?media=podcast&entity=podcastEpisode&limit=$limit&country=$country&term=${query.encodeURLParameter()}", 4 shl 20)
        val results = Json.parseToJsonElement(Net.decode(f.bytes, f.type)).jsonObject["results"]?.jsonArray ?: return emptyList()
        return results.mapNotNull { r ->
            val o = r.jsonObject
            fun s(k: String) = o[k]?.jsonPrimitive?.contentOrNull.orEmpty()
            val url = s("episodeUrl").ifBlank { return@mapNotNull null }
            val title = s("trackName"); val show = s("collectionName"); val desc = stripHtml(s("description")).take(600)
            EpisodeHit(title, show, url, s("artworkUrl600").ifBlank { s("artworkUrl160") }, parseDate(s("releaseDate")),
                (o["trackTimeMillis"]?.jsonPrimitive?.longOrNull ?: 0L) / 1000, desc, s("feedUrl"),
                o["collectionId"]?.jsonPrimitive?.longOrNull ?: 0L, score(query, "$title $title $show $desc"))
        }.distinctBy { it.url }.sortedByDescending { it.score }
    }

    fun episode(h: EpisodeHit) = Episode(id = stableId("t-", h.url), sid = "topic:" + h.collectionId, title = h.title, url = h.url,
        date = h.date, duration = h.durationSec, image = h.image, desc = h.desc)

    /** How well [text] matches [query]: shared words, or for Chinese and Japanese, shared pairs of characters. */
    fun score(query: String, text: String): Double {
        val q = terms(query); if (q.isEmpty()) return 0.0
        val t = terms(text).toSet()
        return q.count { it in t || t.any { w -> w.length > 3 && it.length > 3 && (w.startsWith(it) || it.startsWith(w)) } }.toDouble() / q.size
    }

    private fun terms(s: String): List<String> {
        val lower = s.lowercase()
        val words = Regex("[\\p{L}\\p{N}]+").findAll(lower).map { it.value }.toMutableList()
        val cjk = lower.filter { it in '\u3040'..'\u30ff' || it in '\u4e00'..'\u9fff' }
        for (i in 0 until cjk.length - 1) words.add(cjk.substring(i, i + 2))
        return words.filter { it.length > 1 && it !in STOP }
    }
    private val STOP = setOf("the", "a", "an", "of", "and", "or", "in", "on", "to", "for", "with", "about", "review", "reviews", "podcast", "episode")
}

// ---- preview: a show's feed, without adding it ----

object Peek {
    suspend fun feed(url: String): FeedInfo {
        val sid = stableId("p-", url)
        if (Video.handles(url)) return Video.bridge!!.feed(url, sid)
        val f = Net.fetch(url)
        val info = Feeds.parse(f.text, sid, f.finalUrl)
        return info.copy(episodes = info.episodes.map { it.copy(image = it.image.ifBlank { info.image }) })
    }
}
