@file:OptIn(InternalResourceApi::class)

package app.afterhours.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String = "composeResources/app.afterhours.resources/"

@delegate:ResourceContentHash(1_649_960_638)
internal val Res.drawable.afterhours_icon: DrawableResource by lazy {
      DrawableResource("drawable:afterhours_icon", setOf(
        ResourceItem(setOf(), "${MD}drawable/afterhours_icon.png", -1, -1),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainDrawable0Resources(map: MutableMap<String, DrawableResource>) {
  map.put("afterhours_icon", Res.drawable.afterhours_icon)
}
