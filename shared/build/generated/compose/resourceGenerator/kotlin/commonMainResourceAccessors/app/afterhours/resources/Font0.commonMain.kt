@file:OptIn(InternalResourceApi::class)

package app.afterhours.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String = "composeResources/app.afterhours.resources/"

@delegate:ResourceContentHash(-1_528_069_346)
internal val Res.font.liberation_serif_italic: FontResource by lazy {
      FontResource("font:liberation_serif_italic", setOf(
        ResourceItem(setOf(), "${MD}font/liberation_serif_italic.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(-2_051_147_917)
internal val Res.font.liberation_serif_regular: FontResource by lazy {
      FontResource("font:liberation_serif_regular", setOf(
        ResourceItem(setOf(), "${MD}font/liberation_serif_regular.ttf", -1, -1),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainFont0Resources(map: MutableMap<String, FontResource>) {
  map.put("liberation_serif_italic", Res.font.liberation_serif_italic)
  map.put("liberation_serif_regular", Res.font.liberation_serif_regular)
}
