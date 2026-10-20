package app.afterhours.desktop

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.awt.Cursor
import java.awt.MouseInfo
import java.awt.Rectangle

/** Edges and corners to resize a borderless window by, on every side, as the system's own windows do. */
@Composable
fun ResizeHandles(window: ComposeWindow) {
    Box(Modifier.fillMaxSize()) {
        Edge(Modifier.align(Alignment.CenterStart).width(5.dp).fillMaxHeight(), Cursor.W_RESIZE_CURSOR, window, l = true)
        Edge(Modifier.align(Alignment.CenterEnd).width(5.dp).fillMaxHeight(), Cursor.E_RESIZE_CURSOR, window, r = true)
        Edge(Modifier.align(Alignment.TopCenter).height(4.dp).fillMaxWidth(), Cursor.N_RESIZE_CURSOR, window, t = true)
        Edge(Modifier.align(Alignment.BottomCenter).height(5.dp).fillMaxWidth(), Cursor.S_RESIZE_CURSOR, window, b = true)
        Edge(Modifier.align(Alignment.TopStart).size(10.dp), Cursor.NW_RESIZE_CURSOR, window, l = true, t = true)
        Edge(Modifier.align(Alignment.TopEnd).size(10.dp), Cursor.NE_RESIZE_CURSOR, window, r = true, t = true)
        Edge(Modifier.align(Alignment.BottomStart).size(12.dp), Cursor.SW_RESIZE_CURSOR, window, l = true, b = true)
        Edge(Modifier.align(Alignment.BottomEnd).size(14.dp), Cursor.SE_RESIZE_CURSOR, window, r = true, b = true)
    }
}

@Composable
private fun Edge(modifier: Modifier, cursor: Int, window: ComposeWindow, l: Boolean = false, r: Boolean = false, t: Boolean = false, b: Boolean = false) {
    Box(modifier.pointerHoverIcon(PointerIcon(Cursor(cursor))).pointerInput(Unit) {
        var start = java.awt.Point()
        var from = Rectangle()
        detectDragGestures(onDragStart = { start = MouseInfo.getPointerInfo().location; from = window.bounds }) { change, _ ->
            change.consume()
            val p = MouseInfo.getPointerInfo()?.location ?: return@detectDragGestures
            val dx = p.x - start.x; val dy = p.y - start.y
            val minW = window.minimumSize.width; val minH = window.minimumSize.height
            var x = from.x; var y = from.y; var w = from.width; var h = from.height
            if (r) w = maxOf(minW, from.width + dx)
            if (b) h = maxOf(minH, from.height + dy)
            if (l) { w = maxOf(minW, from.width - dx); x = from.x + from.width - w }
            if (t) { h = maxOf(minH, from.height - dy); y = from.y + from.height - h }
            window.setBounds(x, y, w, h)
        }
    })
}
