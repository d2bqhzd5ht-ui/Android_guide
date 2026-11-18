package com.example.scenicguide.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 失物招领数据模型，字段以 docs/交接.md 1.2 节为准：
 * { id, type, title, category?, description?, location?, image_url?, contact?,
 *   publisher_name?, user_id?, status, created_at?, updated_at? }
 *
 * 后端没有统一格式化时间字段，这里保留原始字符串，展示前由 [formatLostFoundTime] 处理。
 */
@Serializable
data class LostFoundItem(
    val id: Int = 0,
    /** lost = 寻物启事，found = 招领启事 */
    val type: String = TYPE_LOST,
    val title: String = "",
    val category: String? = null,
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
    }
}

/**
 * 分页列表。接口文档没写死分页字段名（只写了「分页列表」），
 * 这里同时兜住 list / records / items 三种常见命名，取第一个非空的。
 */
@Serializable
data class LostFoundPage(
    val list: List<LostFoundItem> = emptyList(),
    val records: List<LostFoundItem> = emptyList(),
    val items: List<LostFoundItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 20,
) {
    val entries: List<LostFoundItem>
        get() = list.ifEmpty { records.ifEmpty { items } }
}

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
