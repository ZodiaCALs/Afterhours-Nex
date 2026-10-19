package app.afterhours.core

import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

data class AppleShow(val id: Long, val title: String, val artist: String, val feedUrl: String, val artwork: String, val genre: String)

/** Apple's public podcast directory. It allows about 20 requests a minute, so requests are paced and cached. */
object Apple {
    var base = "https://itunes.apple.com"
    var intervalMs = 3100L
    private val lock = Mutex()
    private var last = 0L
    private val cache = HashMap<String, List<AppleShow>>()

    suspend fun search(term: String, country: String = "us", limit: Int = 20): List<AppleShow> =
        call("/search?media=podcast&entity=podcast&limit=$limit&country=$country&term=" + term.trim().take(120).encodeURLParameter())

    suspend fun lookup(id: Long, country: String = "us"): AppleShow? = call("/lookup?id=$id&country=$country").firstOrNull()

    private suspend fun call(path: String): List<AppleShow> = lock.withLock {
        cache[path]?.let { return@withLock it }
        val wait = intervalMs - (Clock.now() - last)
        if (wait > 0) delay(wait)
        try {
            val list = parse(Net.fetch(base + path, 4 shl 20).text)
            cache[path] = list
            list
        } catch (e: FetchException) {
            val m = e.message.orEmpty()
            if ("(403)" in m || "(429)" in m) throw FetchException("Apple’s podcast directory asked Afterhours to slow down. Try again in a minute.")
            throw e
        } finally {
            last = Clock.now()
        }
    }

    fun parse(json: String): List<AppleShow> {
        val results = try { Json.parseToJsonElement(json).jsonObject["results"]?.jsonArray } catch (e: Exception) { null } ?: return emptyList()
        return results.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            fun str(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull.orEmpty()
            val feed = str("feedUrl")
            if (feed.isBlank()) return@mapNotNull null
            AppleShow((o["collectionId"] as? JsonPrimitive)?.longOrNull ?: 0L, str("collectionName").ifBlank { str("trackName") }, str("artistName"), feed,
                str("artworkUrl600").ifBlank { str("artworkUrl100") }, str("primaryGenreName"))
        }
    }
}
