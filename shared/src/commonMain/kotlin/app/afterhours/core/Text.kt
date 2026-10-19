package app.afterhours.core

private val NAMED = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to "\u00a0",
    "rsquo" to "’", "lsquo" to "‘", "rdquo" to "”", "ldquo" to "“", "hellip" to "…", "mdash" to "—", "ndash" to "–",
    "copy" to "©", "reg" to "®", "trade" to "™", "laquo" to "«", "raquo" to "»", "middot" to "·", "bull" to "•",
    "eacute" to "é", "egrave" to "è", "aacute" to "á", "agrave" to "à", "iacute" to "í", "oacute" to "ó", "uacute" to "ú",
    "auml" to "ä", "ouml" to "ö", "uuml" to "ü", "ccedil" to "ç", "ntilde" to "ñ", "szlig" to "ß",
)
private val ENTITY = Regex("&(#[xX][0-9a-fA-F]{1,6}|#[0-9]{1,7}|[a-zA-Z][a-zA-Z0-9]{1,9});")

private fun codePoint(cp: Int): String = when {
    cp < 0 || cp > 0x10FFFF -> ""
    cp < 0x10000 -> Char(cp).toString()
    else -> { val v = cp - 0x10000; charArrayOf(Char(0xD800 + (v shr 10)), Char(0xDC00 + (v and 0x3FF))).concatToString() }
}

fun decodeEntities(s: String): String = if ('&' !in s) s else ENTITY.replace(s) { m ->
    val e = m.groupValues[1]
    when {
        e.startsWith("#x") || e.startsWith("#X") -> e.substring(2).toIntOrNull(16)?.let { codePoint(it) } ?: m.value
        e.startsWith("#") -> e.substring(1).toIntOrNull()?.let { codePoint(it) } ?: m.value
        else -> NAMED[e] ?: m.value
    }
}

private val BLOCKS = Regex("(?is)<(script|style)\\b.*?</\\1>")
private val BREAKS = Regex("(?i)<br\\s*/?>|</(p|div|li|h[1-6]|blockquote)>")
private val TAGS = Regex("<[^>]+>")

/** Plain text from a bit of HTML, keeping paragraph breaks. */
fun stripHtml(html: String): String {
    if (html.isBlank()) return ""
    val text = decodeEntities(html.replace(BLOCKS, " ").replace(BREAKS, "\n").replace(TAGS, " "))
    return text.lines().map { it.replace(Regex("[ \\t\\u00a0]+"), " ").trim() }
        .joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
}

/** One line of plain text: for titles and names. */
fun clean(s: String?): String = if (s.isNullOrBlank()) "" else decodeEntities(s.replace(Regex("(?s)<!\\[CDATA\\[(.*?)]]>"), "$1").replace(TAGS, " ")).replace(Regex("\\s+"), " ").trim()

private fun isCjk(c: Char) = c in '\u3040'..'\u30ff' || c in '\u3400'..'\u9fff' || c in '\uac00'..'\ud7af' || c in '\uf900'..'\ufaff'
private fun squash(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
private val STOP = setOf("the", "and", "with", "from", "podcast", "podcasts", "show", "radio", "official", "episodes", "episode", "home", "feed", "rss")
private fun words(s: String) = s.lowercase().split(Regex("[^\\p{L}\\p{Nd}]+")).filter { it.length >= 3 && it !in STOP }.toSet()

/** Whether two titles are about the same show: shared words, one inside the other, or shared CJK pairs. */
fun related(a: String, b: String): Boolean {
    val na = squash(a); val nb = squash(b)
    if (na.isEmpty() || nb.isEmpty()) return false
    if (na.contains(nb) || nb.contains(na)) return true
    if (words(a).any { it in words(b) }) return true
    var shared = 0
    for (i in 0 until na.length - 1) if (isCjk(na[i]) && nb.contains(na.substring(i, i + 2))) shared++
    return shared >= 2
}

private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
private val ZONES = mapOf("UT" to 0, "UTC" to 0, "GMT" to 0, "Z" to 0, "EST" to -300, "EDT" to -240, "CST" to -360, "CDT" to -300,
    "MST" to -420, "MDT" to -360, "PST" to -480, "PDT" to -420, "BST" to 60, "CET" to 60, "CEST" to 120, "JST" to 540, "AEST" to 600)
private val ISO = Regex("(\\d{4})-(\\d{2})-(\\d{2})(?:[T ](\\d{2}):(\\d{2})(?::(\\d{2}))?(?:\\.\\d+)?)?\\s*(Z|[+-]\\d{2}:?\\d{2})?")
private val RFC = Regex("(\\d{1,2})\\s+([A-Za-z]{3})[A-Za-z]*\\.?,?\\s+(\\d{2,4})(?:\\s+(\\d{1,2}):(\\d{2})(?::(\\d{2}))?)?\\s*([A-Za-z]{1,5}|[+-]\\d{2}:?\\d{2})?")

/** Milliseconds since 1970 for a feed date (RFC 822 or ISO 8601), or 0. */
fun parseDate(s: String?): Long {
    if (s.isNullOrBlank()) return 0
    val t = s.trim()
    RFC.find(t)?.let { m ->
        val g = m.groupValues
        val mo = MONTHS.indexOf(g[2].lowercase().take(3)) + 1
        if (mo > 0) {
            var y = g[3].toInt(); if (y < 100) y += if (y < 70) 2000 else 1900
            return epochMs(y, mo, g[1].toInt(), g[4].toIntOrNull() ?: 0, g[5].toIntOrNull() ?: 0, g[6].toIntOrNull() ?: 0, zone(g[7]))
        }
    }
    ISO.find(t)?.let { m ->
        val g = m.groupValues
        return epochMs(g[1].toInt(), g[2].toInt(), g[3].toInt(), g[4].toIntOrNull() ?: 0, g[5].toIntOrNull() ?: 0, g[6].toIntOrNull() ?: 0, zone(g[7]))
    }
    return 0
}

private fun zone(z: String): Int {
    if (z.isEmpty()) return 0
    ZONES[z.uppercase()]?.let { return it }
    val m = Regex("([+-])(\\d{2}):?(\\d{2})").matchEntire(z) ?: return 0
    val minutes = m.groupValues[2].toInt() * 60 + m.groupValues[3].toInt()
    return if (m.groupValues[1] == "-") -minutes else minutes
}

/** Days-from-civil (proleptic Gregorian), then to milliseconds in UTC. */
fun epochMs(y: Int, mo: Int, d: Int, h: Int = 0, mi: Int = 0, sec: Int = 0, offsetMinutes: Int = 0): Long {
    val yy = if (mo <= 2) y - 1 else y
    val era = (if (yy >= 0) yy else yy - 399) / 400
    val yoe = yy - era * 400
    val mp = (mo + 9) % 12
    val doy = (153 * mp + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    val days = era * 146097L + doe - 719468
    return ((days * 86400 + h * 3600 + mi * 60 + sec) - offsetMinutes * 60L) * 1000
}

/** Seconds from "1:02:03", "62:03" or "3723". */
fun parseDuration(s: String?): Long {
    if (s.isNullOrBlank()) return 0
    val parts = s.trim().split(':')
    if (parts.size == 1) return parts[0].toDoubleOrNull()?.toLong() ?: 0
    var total = 0L
    for (p in parts) total = total * 60 + (p.trim().toDoubleOrNull()?.toLong() ?: return 0)
    return total
}

fun fmtClock(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    fun two(n: Long) = if (n < 10) "0$n" else "$n"
    return if (h > 0) "$h:${two(m)}:${two(sec)}" else "$m:${two(sec)}"
}

fun fmtLength(seconds: Long): String {
    if (seconds <= 0) return ""
    val m = (seconds + 30) / 60
    val h = m / 60; val r = m % 60; val mm = m.coerceAtLeast(1)
    return when (I18n.lang) {
        "zh-Hans" -> if (m < 60) "$mm 分钟" else if (r > 0) "$h 小时 $r 分钟" else "$h 小时"
        "ja" -> if (m < 60) "${mm}分" else if (r > 0) "${h}時間${r}分" else "${h}時間"
        else -> if (m < 60) "$mm min" else if (r > 0) "$h h $r min" else "$h h"
    }
}

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val WEEKDAYS = listOf("Thursday", "Friday", "Saturday", "Sunday", "Monday", "Tuesday", "Wednesday")
private val WEEKDAYS_ZH = listOf("星期四", "星期五", "星期六", "星期日", "星期一", "星期二", "星期三")
private val WEEKDAYS_JA = listOf("木曜日", "金曜日", "土曜日", "日曜日", "月曜日", "火曜日", "水曜日")

/** "Today", a weekday within the last week, "12 Mar" this year, or "12 Mar 2024". */
fun fmtDate(ms: Long, now: Long = Clock.now()): String {
    if (ms <= 0) return ""
    val off = Clock.utcOffsetMinutes() * 60_000L
    val day = (ms + off).floorDiv(86_400_000L)
    val today = (now + off).floorDiv(86_400_000L)
    val cjk = I18n.lang == "zh-Hans" || I18n.lang == "ja"
    if (day == today) return when (I18n.lang) { "zh-Hans" -> "今天"; "ja" -> "今日"; else -> "Today" }
    if (day in (today - 6) until today) {
        val i = day.mod(7L).toInt()
        return when (I18n.lang) { "zh-Hans" -> WEEKDAYS_ZH[i]; "ja" -> WEEKDAYS_JA[i]; else -> WEEKDAYS[i] }
    }
    val (y, m, d) = civil(day)
    val (ty, _, _) = civil(today)
    if (cjk) return if (y == ty) "${m}月${d}日" else "${y}年${m}月${d}日"
    return if (y == ty) "$d ${MONTH_NAMES[m - 1]}" else "$d ${MONTH_NAMES[m - 1]} $y"
}

private fun civil(days: Long): Triple<Int, Int, Int> {
    val z = days + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
    val y = (yoe + era * 400 + if (m <= 2) 1 else 0).toInt()
    return Triple(y, m, d)
}

/** Resolve a link against the page it's on. */
fun resolveUrl(base: String, href: String): String {
    val h = href.trim()
    if (h.isEmpty()) return base
    if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(h)) return h
    val m = Regex("^(https?:)//([^/?#]*)([^?#]*)").find(base) ?: return h
    val (scheme, host, path) = m.destructured
    return when {
        h.startsWith("//") -> scheme + h
        h.startsWith("/") -> "$scheme//$host${normalizePath(h)}"
        h.startsWith("?") -> "$scheme//$host$path$h"
        h.startsWith("#") -> base.substringBefore('#') + h
        else -> "$scheme//$host${normalizePath(path.substringBeforeLast('/', "") + "/" + h)}"
    }
}

private fun normalizePath(p: String): String {
    val cut = p.indexOfAny(charArrayOf('?', '#')).let { if (it < 0) p.length else it }
    val segments = p.substring(0, cut).split('/')
    val out = ArrayList<String>()
    for ((i, s) in segments.withIndex()) {
        when (s) {
            "." -> if (i == segments.lastIndex) out.add("")
            ".." -> { if (out.size > 1) out.removeAt(out.lastIndex); if (i == segments.lastIndex) out.add("") }
            else -> out.add(s)
        }
    }
    val joined = out.joinToString("/")
    return (if (joined.startsWith("/")) joined else "/$joined") + p.substring(cut)
}

fun hostOf(url: String): String = Regex("^https?://([^/?#:]+)").find(url)?.groupValues?.get(1)?.lowercase()?.removePrefix("www.") ?: ""
