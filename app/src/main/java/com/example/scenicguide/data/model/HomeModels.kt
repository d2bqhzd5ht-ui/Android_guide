package com.example.scenicguide.data.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class Banner(
    val id: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
)

data class QuickEntry(
    val id: String,
    val title: String,
    val target: String?,
)

data class PoiBrief(
    val id: String,
    val name: String,
    val category: String?,
    val location: String?,
    val description: String?,
)

data class RouteBrief(
    val id: String,
    val name: String,
    val description: String?,
    val duration: String?,
    val distance: String?,
)

data class HomeContent(
    val banners: List<Banner> = emptyList(),
    val quickEntries: List<QuickEntry> = emptyList(),
    val hotPois: List<PoiBrief> = emptyList(),
    val recommendedRoutes: List<RouteBrief> = emptyList(),
)

/**
 * `GET /api/v1/home` 在 openapi.yaml 里没有收录，只有交接文档给了 4 个字段名，
 * 所以这里做宽松解析：字段名兼容 camelCase / snake_case，缺字段时退化为空列表，不抛异常。
 * 拿到真实返回后按需收紧。
 */
object HomeParser {

    fun parse(root: JsonObject): HomeContent = HomeContent(
        banners = objectsOf(root, "banners", "bannerList", "slides").map { item ->
            Banner(
                id = item.text("id").orEmpty(),
                title = item.text("title", "name").orEmpty(),
                subtitle = item.text("subtitle", "description", "desc"),
                imageUrl = item.text("imageUrl", "image_url", "image", "cover", "url"),
            )
        },
        quickEntries = objectsOf(root, "quickEntries", "quick_entries", "entries").map { item ->
            QuickEntry(
                id = item.text("id").orEmpty(),
                title = item.text("title", "name").orEmpty(),
                target = item.text("target", "route", "path", "link"),
            )
        },
        hotPois = objectsOf(root, "hotPois", "hot_pois", "pois").map { item ->
            PoiBrief(
                id = item.text("id").orEmpty(),
                name = item.text("name", "title").orEmpty(),
                category = item.text("category"),
                location = item.text("location", "address"),
                description = item.text("description", "desc"),
            )
        },
        recommendedRoutes = objectsOf(root, "recommendedRoutes", "recommended_routes", "routes").map { item ->
            RouteBrief(
                id = item.text("id").orEmpty(),
                name = item.text("name", "title").orEmpty(),
                description = item.text("description", "desc"),
                duration = item.text("duration", "estimatedDuration", "estimated_duration"),
                distance = item.text("distance", "totalDistance", "total_distance"),
            )
        },
    )

    private fun objectsOf(root: JsonObject, vararg keys: String): List<JsonObject> {
        val array = keys.firstNotNullOfOrNull { root[it] as? JsonArray } ?: return emptyList()
        return array.filterIsInstance<JsonObject>()
    }

    private fun JsonObject.text(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            (this[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
        }
}
