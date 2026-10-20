package app.afterhours.core

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

/**
 * A light caption translator: it translates only the lines around the one being spoken, one at a time, with a free
 * service (MyMemory, no account needed) or your own LibreTranslate server, and remembers every line it has translated.
 */
class Translator(private val storage: Storage) {
    private val tables = mutableMapOf<String, MutableMap<String, String>>()
    private val busy = mutableSetOf<String>()
    private val lock = Mutex()
    private val _version = MutableStateFlow(0)
    /** Goes up whenever new translations arrive, so captions can redraw. */
    val version: StateFlow<Int> = _version.asStateFlow()

    fun get(line: String, target: String): String? = tables.getOrPut(target) { load(target) }[line]

    /** Translates any of [lines] not yet known into [target] ("en", "zh-Hans", "ja", …). */
    suspend fun ensure(lines: List<String>, target: String, s: Settings) {
        val todo = lock.withLock {
            val table = tables.getOrPut(target) { load(target) }
            lines.map { it.trim() }.filter { it.isNotEmpty() && it !in table && busy.add("$target\u0000$it") }
        }
        var added = false
        for (line in todo) {
            val from = guessLanguage(line)
            val result = if (from.take(2) == target.take(2)) line else runCatching { translate(line, from, target, s) }.getOrNull()
            lock.withLock {
                busy.remove("$target\u0000$line")
                if (!result.isNullOrBlank()) { tables.getOrPut(target) { load(target) }[line] = result; added = true }
            }
            if (added) _version.value++
        }
        if (added) lock.withLock { save(target) }
    }

    fun clear(): Int {
        val names = storage.list("tr-")
        names.forEach { storage.delete(it) }
        tables.clear(); _version.value++
        return names.size
    }

    private suspend fun translate(text: String, from: String, to: String, s: Settings): String? =
        if (s.translator == "libre" && s.libreUrl.startsWith("http")) libre(text, to, s.libreUrl, s.libreKey) else myMemory(text, from, to)

    private suspend fun myMemory(text: String, from: String, to: String): String? {
        val pair = "${mmCode(from)}|${mmCode(to)}"
        val f = Net.fetch("https://api.mymemory.translated.net/get?q=${text.encodeURLParameter()}&langpair=${pair.encodeURLParameter()}", 1 shl 20)
        val json = Json.parseToJsonElement(Net.decode(f.bytes, f.type)).jsonObject
        if ((json["responseStatus"]?.jsonPrimitive?.intOrNull ?: 200) != 200) return null
        return json["responseData"]?.jsonObject?.get("translatedText")?.jsonPrimitive?.contentOrNull?.let { decodeEntities(it).trim() }
    }

    private suspend fun libre(text: String, to: String, server: String, key: String): String? {
        val body = buildJsonObject {
            put("q", text); put("source", "auto"); put("target", if (to.startsWith("zh")) "zh" else to.take(2)); put("format", "text")
            if (key.isNotBlank()) put("api_key", key)
        }
        val reply = Net.client.post(server.trimEnd('/') + "/translate") { contentType(ContentType.Application.Json); setBody(body.toString()) }.bodyAsText()
        return Json.parseToJsonElement(reply).jsonObject["translatedText"]?.jsonPrimitive?.contentOrNull?.trim()
    }

    private fun mmCode(lang: String) = when { lang.startsWith("zh") -> "zh-CN"; else -> lang.take(2) }

    private fun load(target: String): MutableMap<String, String> = runCatching {
        Json.parseToJsonElement(storage.read("tr-$target.json") ?: "{}").jsonObject.mapValues { it.value.jsonPrimitive.content }.toMutableMap()
    }.getOrElse { mutableMapOf() }

    private fun save(target: String) {
        val table = tables[target] ?: return
        storage.write("tr-$target.json", JsonObject(table.mapValues { JsonPrimitive(it.value) }).toString())
    }

    companion object {
        /** A caption line's language, from its script: Japanese kana, Chinese characters, Hangul, or else English. */
        fun guessLanguage(text: String): String = when {
            text.any { it in '\u3040'..'\u30ff' } -> "ja"
            text.any { it in '\u4e00'..'\u9fff' } -> "zh-Hans"
            text.any { it in '\uac00'..'\ud7af' } -> "ko"
            else -> "en"
        }
    }
}

/** Something the app keeps that can be cleaned: its size, how many files, and whether "Clean cache" includes it. */
data class CacheItem(val id: String, val label: String, val bytes: Long, val files: Int, val note: String, val everyday: Boolean)

/** "12.3 MB". */
fun fmtBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024L * 1024 -> "${(bytes / 102.4).toLong() / 10.0} KB"
    bytes < 1024L * 1024 * 1024 -> "${(bytes / 104857.6).toLong() / 10.0} MB"
    else -> "${(bytes / 107374182.4).toLong() / 10.0} GB"
}
