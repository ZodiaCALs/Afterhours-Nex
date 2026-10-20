package app.afterhours.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Cover art for the shows Discover suggests: looked up in Apple's directory as their cards appear, then remembered. */
object Covers {
    private val known = mutableMapOf<String, String>()
    private var storage: Storage? = null

    fun attach(s: Storage) {
        storage = s
        runCatching { Json.parseToJsonElement(s.read("covers.json") ?: "{}").jsonObject.forEach { (k, v) -> known[k] = v.jsonPrimitive.content } }
    }

    fun cached(key: String): String = known[key].orEmpty()

    suspend fun find(p: Pick): String {
        known[p.key]?.let { return it }
        val hit = runCatching { Apple.search(p.term.ifBlank { p.title }, p.countries.firstOrNull() ?: "us", 5).firstOrNull { related(it.title, p.title) } }.getOrNull()
        val art = hit?.artwork.orEmpty()
        if (art.isNotBlank()) {
            known[p.key] = art
            runCatching { storage?.write("covers.json", JsonObject(known.mapValues { JsonPrimitive(it.value) }).toString()) }
        }
        return art
    }
}
