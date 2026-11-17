package com.example.scenicguide.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 失物招领数据模型，字段与后端 `SELECT *` 结果一致（全 snake_case）：
 * { id, type, category, title, description?, location?, image_url?, contact?,
 *   publisher_name?, user_id?, status, created_at, updated_at? }
 *
 * 后端没有统一格式化时间字段，这里保留原始字符串，展示前由 [formatLostFoundTime] 处理。
 */
@Serializable
data class LostFoundItem(
    val id: Int = 0,
    /** lost = 寻物启事，found = 招领启事 */
    val type: String = TYPE_LOST,
    val title: String = "",
    /** 后端默认 "other"，不会是 null */
    val category: String = CATEGORY_OTHER,
    val description: String? = null,
    val location: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val contact: String? = null,
    @SerialName("publisher_name") val publisherName: String? = null,
    @SerialName("user_id") val userId: String? = null,
    /** open / resolved / expired */
    val status: String = STATUS_OPEN,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    val typeLabel: String get() = if (type == TYPE_FOUND) "招领" else "寻物"

    val statusLabel: String get() = when (status) {
        STATUS_RESOLVED -> "已解决"
        STATUS_EXPIRED -> "已过期"
        else -> "进行中"
    }

    companion object {
        const val TYPE_LOST = "lost"
        const val TYPE_FOUND = "found"

        const val STATUS_OPEN = "open"
        const val STATUS_RESOLVED = "resolved"
        const val STATUS_EXPIRED = "expired"

        const val CATEGORY_OTHER = "other"
    }
}

/**
 * 列表响应：`data = { list, pagination: { page, size, total, totalPages } }`，
 * 分页信息嵌在 pagination 里，不再是平铺字段。
 */
@Serializable
data class LostFoundPage(
    val list: List<LostFoundItem> = emptyList(),
    val pagination: LostFoundPagination = LostFoundPagination(),
)

@Serializable
data class LostFoundPagination(
    val page: Int = 1,
    val size: Int = 10,
    val total: Int = 0,
    @SerialName("totalPages") val totalPages: Int = 0,
)

/** 新建 / 更新时的请求体，type 与 title 必填，其余可空（后端会忽略空值）。 */
@Serializable
data class LostFoundDraft(
    val type: String,
    val title: String,
    val category: String? = null,
    val description: String? = null,
    val location: String? = null,
    val contact: String? = null,
    @SerialName("publisher_name") val publisherName: String? = null,
    @SerialName("user_id") val userId: String? = null,
)

@Serializable
data class LostFoundStatusRequest(val status: String)

/** PATCH /status 只返回 { id, status }，不是完整记录 */
@Serializable
data class LostFoundStatusResult(
    val id: Int = 0,
    val status: String = "",
)

/**
 * 后端时间字段原样返回（可能是 `2026-11-15T13:28:00.000Z`，也可能是 `2026-11-15 13:28:00`），
 * 统一截成 `yyyy-MM-dd HH:mm`；解析不了就原样返回。
 */
fun formatLostFoundTime(raw: String?): String {
    val text = raw?.trim().orEmpty()
    if (text.length < 16) return text
    return if (text.length > 10 && text[10] == 'T') {
        "${text.substring(0, 10)} ${text.substring(11, 16)}"
    } else {
        text.substring(0, 16)
    }
}
