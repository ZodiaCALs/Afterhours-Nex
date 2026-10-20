package app.afterhours.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** One shape of an icon: its path, and whether it is filled or drawn as a line (how thick, and how its ends and corners are drawn). */
private class Part(val d: String, val fill: Boolean, val width: Float = 1.7f, val cap: StrokeCap = StrokeCap.Butt, val join: StrokeJoin = StrokeJoin.Miter)

private fun icon(name: String, vararg parts: Part): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
    for (p in parts) addPath(addPathNodes(p.d), fill = if (p.fill) SolidColor(Color.Black) else null, stroke = if (p.fill) null else SolidColor(Color.Black),
        strokeLineWidth = p.width, strokeLineCap = p.cap, strokeLineJoin = p.join)
}.build()

/** The Afterhours web app's icons (its SVG sprite), drawn with the same fine strokes. Tint them like any icon. */
object WebIcons {
    val Globe: ImageVector by lazy {
        icon("globe", Part("M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0", fill = false, width = 1.6f),
            Part("M12 3c2.4 2.6 3.6 5.6 3.6 9s-1.2 6.4-3.6 9c-2.4-2.6-3.6-5.6-3.6-9s1.2-6.4 3.6-9Z", fill = false, width = 1.6f, join = StrokeJoin.Round),
            Part("M3.6 9h16.8M3.6 15h16.8", fill = false, width = 1.6f, cap = StrokeCap.Round))
    }
    val Pin: ImageVector by lazy {
        icon("pin", Part("M9 3.5h6l-1 5 3 3v1.8H7v-1.8l3-3-1-5Z", fill = false, width = 1.6f, join = StrokeJoin.Round),
            Part("M12 13.3V20.5", fill = false, width = 1.6f, cap = StrokeCap.Round))
    }
    val Radio: ImageVector by lazy {
        icon("radio", Part("M5 9h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2Z", fill = false, width = 1.7f, join = StrokeJoin.Round),
            Part("M6.5 9 17 4.5", fill = false, width = 1.7f, cap = StrokeCap.Round),
            Part("M13.5 14.5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0", fill = false, width = 1.7f),
            Part("M6.5 13.2h3.5M6.5 16.2h3.5", fill = false, width = 1.7f, cap = StrokeCap.Round))
    }
    val Play: ImageVector by lazy { icon("play", Part("M8 5.6v12.8a1 1 0 0 0 1.52.85l10.2-6.4a1 1 0 0 0 0-1.7L9.52 4.75A1 1 0 0 0 8 5.6z", fill = true)) }
    val Pause: ImageVector by lazy { icon("pause", Part("M7.5 5H9.1A1.3 1.3 0 0 1 10.4 6.3V17.7A1.3 1.3 0 0 1 9.1 19H7.5A1.3 1.3 0 0 1 6.2 17.7V6.3A1.3 1.3 0 0 1 7.5 5Z", fill = true), Part("M14.9 5H16.5A1.3 1.3 0 0 1 17.8 6.3V17.7A1.3 1.3 0 0 1 16.5 19H14.9A1.3 1.3 0 0 1 13.6 17.7V6.3A1.3 1.3 0 0 1 14.9 5Z", fill = true)) }
    val Prev: ImageVector by lazy { icon("prev", Part("M6 6H6.2A1 1 0 0 1 7.2 7V17A1 1 0 0 1 6.2 18H6A1 1 0 0 1 5 17V7A1 1 0 0 1 6 6Z", fill = true), Part("M18.5 6.9v10.2a.9.9 0 0 1-1.38.76L9.6 12.76a.9.9 0 0 1 0-1.52l7.52-5.1a.9.9 0 0 1 1.38.76z", fill = true)) }
    val Next: ImageVector by lazy { icon("next", Part("M17.8 6H18A1 1 0 0 1 19 7V17A1 1 0 0 1 18 18H17.8A1 1 0 0 1 16.8 17V7A1 1 0 0 1 17.8 6Z", fill = true), Part("M5.5 6.9v10.2a.9.9 0 0 0 1.38.76l7.52-5.1a.9.9 0 0 0 0-1.52L6.88 6.14a.9.9 0 0 0-1.38.76z", fill = true)) }
    val Back: ImageVector by lazy { icon("back", Part("M1 4v6h6M3.51 15a9 9 0 1 0 2.13-9.36L1 10", fill = false, width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Fwd: ImageVector by lazy { icon("fwd", Part("M23 4v6h-6M20.49 15a9 9 0 1 1-2.12-9.36L23 10", fill = false, width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Vol: ImageVector by lazy { icon("vol", Part("M4 9.2h3.3l4.4-3.7a.6.6 0 0 1 1 .46v12.08a.6.6 0 0 1-1 .46L7.3 14.8H4a1 1 0 0 1-1-1v-3.6a1 1 0 0 1 1-1z", fill = true), Part("M15.5 9.2a4 4 0 0 1 0 5.6M18.2 6.6a7.6 7.6 0 0 1 0 10.8", fill = false, width = 1.8f, cap = StrokeCap.Round)) }
    val Mute: ImageVector by lazy { icon("mute", Part("M4 9.2h3.3l4.4-3.7a.6.6 0 0 1 1 .46v12.08a.6.6 0 0 1-1 .46L7.3 14.8H4a1 1 0 0 1-1-1v-3.6a1 1 0 0 1 1-1z", fill = true), Part("M16 9.5l5 5M21 9.5l-5 5", fill = false, width = 1.8f, cap = StrokeCap.Round)) }
    val Cc: ImageVector by lazy { icon("cc", Part("M5.6 5.5H18.4A2.6 2.6 0 0 1 21 8.1V15.9A2.6 2.6 0 0 1 18.4 18.5H5.6A2.6 2.6 0 0 1 3 15.9V8.1A2.6 2.6 0 0 1 5.6 5.5Z", fill = false, width = 1.7f), Part("M10.6 10.3a2.1 2.1 0 1 0 0 3.4M16.6 10.3a2.1 2.1 0 1 0 0 3.4", fill = false, width = 1.7f, cap = StrokeCap.Round)) }
    val Quote: ImageVector by lazy { icon("quote", Part("M5 4.8h14a1.6 1.6 0 0 1 1.6 1.6v8.2a1.6 1.6 0 0 1-1.6 1.6h-7l-4.6 3.6v-3.6H5a1.6 1.6 0 0 1-1.6-1.6V6.4A1.6 1.6 0 0 1 5 4.8z", fill = false, width = 1.7f, join = StrokeJoin.Round), Part("M8.6 8.8h2.6v2.3c0 1.3-.7 2.2-2 2.6l-.4-.8c.6-.3.9-.7.9-1.3H8.6zm4.8 0H16v2.3c0 1.3-.7 2.2-2 2.6l-.4-.8c.6-.3.9-.7.9-1.3h-1.1z", fill = true)) }
    val Moon: ImageVector by lazy { icon("moon", Part("M19.5 14.6A7.6 7.6 0 0 1 9.4 4.5a7.6 7.6 0 1 0 10.1 10.1z", fill = false, width = 1.7f, join = StrokeJoin.Round)) }
    val Search: ImageVector by lazy { icon("search", Part("M4.3 10.5A6.2 6.2 0 1 0 16.7 10.5A6.2 6.2 0 1 0 4.3 10.5Z", fill = false, width = 2.0f), Part("M15.2 15.2l4.6 4.6", fill = false, width = 2.0f, cap = StrokeCap.Round)) }
    val Home: ImageVector by lazy { icon("home", Part("M4 10.6 12 4l8 6.6V19a1 1 0 0 1-1 1h-4.6v-5.6H9.6V20H5a1 1 0 0 1-1-1z", fill = false, width = 1.8f, join = StrokeJoin.Round)) }
    val Sparkle: ImageVector by lazy { icon("sparkle", Part("M11 4l1.7 4.6a2 2 0 0 0 1.2 1.2L18.5 11.5l-4.6 1.7a2 2 0 0 0-1.2 1.2L11 19l-1.7-4.6a2 2 0 0 0-1.2-1.2L3.5 11.5l4.6-1.7a2 2 0 0 0 1.2-1.2z", fill = false, width = 1.7f, join = StrokeJoin.Round), Part("M19 3v3.4M17.3 4.7h3.4", fill = false, width = 1.7f, cap = StrokeCap.Round)) }
    val Stack: ImageVector by lazy { icon("stack", Part("M5 4.5v15M9.5 4.5v15M13.6 5.4l4.8 14.1", fill = false, width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Refresh: ImageVector by lazy { icon("refresh", Part("M19.5 12a7.5 7.5 0 1 1-2.2-5.3M19.5 4.5v4.2h-4.2", fill = false, width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Sort: ImageVector by lazy { icon("sort", Part("M8 5v14M5 16l3 3 3-3M16 19V5M13 8l3-3 3 3", fill = false, width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Trash: ImageVector by lazy { icon("trash", Part("M4.5 7h15M10 7V4.8h4V7M6.5 7l.9 12.2h9.2L17.5 7", fill = false, width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Link: ImageVector by lazy { icon("link", Part("M10 14a4 4 0 0 0 5.66 0l3-3a4 4 0 0 0-5.66-5.66l-1 1M14 10a4 4 0 0 0-5.66 0l-3 3a4 4 0 0 0 5.66 5.66l1-1", fill = false, width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Plus: ImageVector by lazy { icon("plus", Part("M12 5v14M5 12h14", fill = false, width = 2.2f, cap = StrokeCap.Round)) }
    val Check: ImageVector by lazy { icon("check", Part("M5 12.5l4.5 4.5L19 7.5", fill = false, width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Chevleft: ImageVector by lazy { icon("chevleft", Part("M15 5l-7 7 7 7", fill = false, width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Chevright: ImageVector by lazy { icon("chevright", Part("M9 5l7 7-7 7", fill = false, width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Chevdown: ImageVector by lazy { icon("chevdown", Part("M5 9l7 7 7-7", fill = false, width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Expand: ImageVector by lazy { icon("expand", Part("M14 4h6v6M20 4l-6.5 6.5M10 20H4v-6M4 20l6.5-6.5", fill = false, width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Theme: ImageVector by lazy { icon("theme", Part("M4 12A8 8 0 1 0 20 12A8 8 0 1 0 4 12Z", fill = false, width = 1.7f), Part("M12 4a8 8 0 0 1 0 16z", fill = true)) }
    val Import: ImageVector by lazy { icon("import", Part("M12 4v11M7.5 10.5 12 15l4.5-4.5M5 19.5h14", fill = false, width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Export: ImageVector by lazy { icon("export", Part("M12 15V4M7.5 8.5 12 4l4.5 4.5M5 19.5h14", fill = false, width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Infinity: ImageVector by lazy { icon("infinity", Part("M6.8 9.3a2.7 2.7 0 1 0 0 5.4c2.7 0 7.7-5.4 10.4-5.4a2.7 2.7 0 1 1 0 5.4c-2.7 0-7.7-5.4-10.4-5.4z", fill = false, width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Swap: ImageVector by lazy { icon("swap", Part("M5 8h13M15 4.5 18.5 8 15 11.5M19 16H6M9 12.5 5.5 16 9 19.5", fill = false, width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    val Wave: ImageVector by lazy { icon("wave", Part("M4 11v2M7.5 8v8M11 5v14M14.5 9v6M18 7v10M21 11v2", fill = false, width = 1.9f, cap = StrokeCap.Round)) }
    val Gear: ImageVector by lazy { icon("gear", Part("M9 12A3 3 0 1 0 15 12A3 3 0 1 0 9 12Z", fill = false, width = 1.7f), Part("M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z", fill = false, width = 1.7f)) }
    val File: ImageVector by lazy { icon("file", Part("M7 3.5h7l4.5 4.5v12.5H7zM14 3.5V8h4.5", fill = false, width = 1.7f, join = StrokeJoin.Round), Part("M9.5 13h6M9.5 16.5h4", fill = false, width = 1.7f, cap = StrokeCap.Round)) }
}
