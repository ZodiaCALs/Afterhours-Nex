package app.afterhours.core

/** The interface language: "en", "zh-Hans" or "ja". Podcast titles and other content are never translated. */
object I18n {
    var lang: String = "en"

    /** The language to use for a setting of "system", "en", "zh-Hans" or "ja", given the device's language. */
    fun pick(setting: String, system: String): String = when (setting) {
        "en", "zh-Hans", "ja" -> setting
        else -> system.lowercase().let { if (it.startsWith("zh")) "zh-Hans" else if (it.startsWith("ja")) "ja" else "en" }
    }

    internal fun lookup(key: String): String? = when (lang) {
        "zh-Hans" -> STRINGS[key]?.first
        "ja" -> STRINGS[key]?.second
        else -> null
    }
}

class Pat(val re: Regex, val zh: String, val ja: String)

/** Interface text in the chosen language. Text without a translation (such as a show's title) passes through. */
fun tr(text: String): String = translate(text, 0)

private fun translate(text: String, depth: Int): String {
    if (I18n.lang == "en" || text.isBlank()) return text
    val key = text.trim()
    val out = I18n.lookup(key) ?: if (depth < 3) PATTERNS.firstNotNullOfOrNull { p ->
        p.re.matchEntire(key)?.let { m ->
            var s = if (I18n.lang == "ja") p.ja else p.zh
            m.groupValues.drop(1).forEachIndexed { i, v -> s = s.replace("{${i + 1}}", translate(v, depth + 1)) }
            s
        }
    } else null
    return if (out == null) text else text.replace(key, out)
}
