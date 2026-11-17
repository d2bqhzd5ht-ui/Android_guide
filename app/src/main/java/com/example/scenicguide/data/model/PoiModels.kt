package com.example.scenicguide.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * POI 类别枚举，与后端 poi.ts 的 VALID_CATEGORIES 保持一致：
 * attraction（景点）/ facility（设施）/ landmark（地标）/ food（餐饮）/ other（其他）
 */
@Serializable
enum class PoiCategory(val displayName: String) {
    attraction("景点"),
    facility("设施"),
    landmark("地标"),
    food("餐饮"),
    other("其他"),
}

/**
 * 后端 GET /api/v1/poi 返回的列表项（精简字段）：
 * SELECT id, name, category, description, location, status FROM poi
 */
@Serializable
data class PoiListItem(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val description: String? = null,
    val location: String? = null,
    val status: Int = 1,
)

/**
 * 后端 GET /api/v1/poi/{id} 返回的完整记录（SELECT *）
 */
@Serializable
data class PoiDetail(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val description: String? = null,
    val location: String? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val status: Int = 1,
    @SerialName("created_at")
    val createdAt: String? = null,
)

/** category 字符串 → 友好显示名 */
fun String?.categoryDisplayName(): String {
    if (this == null) return "其他"
    return PoiCategory.entries.firstOrNull { it.name == this }?.displayName ?: this
}
