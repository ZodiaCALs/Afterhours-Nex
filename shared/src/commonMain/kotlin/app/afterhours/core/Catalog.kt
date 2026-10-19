package app.afterhours.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Category(val id: String, val label: String, val short: String = "", val keywords: List<List<String>> = emptyList())

@Serializable
data class LangPicks(val label: String, val keywords: List<List<String>> = emptyList())

@Serializable
data class Pick(
    val key: String, val cat: String, val title: String, val author: String = "", val artist: String = "",
    val term: String = "", val countries: List<String> = listOf("us"), val why: String = "", val site: String = "",
)

@Serializable
data class CatalogData(val categories: List<Category>, val langs: Map<String, LangPicks> = emptyMap(), val presets: List<Pick>, val extras: List<Pick> = emptyList())

/** The 30 shows from the recommendation poster, the related picks, and the topics. */
object Catalog {
    val data: CatalogData by lazy { Json { ignoreUnknownKeys = true }.decodeFromString(CatalogData.serializer(), CATALOG_JSON) }
    fun category(id: String): Category? = data.categories.firstOrNull { it.id == id }
    fun pick(key: String): Pick? = data.presets.firstOrNull { it.key == key } ?: data.extras.firstOrNull { it.key == key }
}
