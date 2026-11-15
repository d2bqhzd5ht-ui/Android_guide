package com.example.scenicguide.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 字段按后端源码定死（whoisthe4th/backend/src/routes/home.ts）：
 * `GET /api/v1/home` → { code, message, data: { banners, quickEntries, hotPois, recommendedRoutes, hotProducts, stats } }
 *
 * - banners / quickEntries 来自 digital_human_config 键值对里的 JSON，字段名固定为
 *   { id, title, image, color } 与 { icon, label, path, color }
 * - hotPois 来自 poi 表：{ id, name, category, description }
 * - recommendedRoutes 来自 route 表：{ id, name, theme, description, estimated_minutes }
 * 保留默认值只为兜住「后台把配置改成缺字段」的情况，不再做 camelCase/snake_case 猜测。
 */
@Serializable
data class Banner(
    val id: Int = 0,
    val title: String = "",
    /** 图片地址，后端默认给空串 */
    val image: String = "",
    /** CSS 渐变串，如 "linear-gradient(135deg, #1989fa, #4db8ff)" */
    val color: String = "",
)

@Serializable
data class QuickEntry(
    /** antd 图标名（如 service-o），Compose 侧暂无对应图标，保留字段备用 */
    val icon: String = "",
    val label: String = "",
    /** 目标路由，如 /interaction、/poi */
    val path: String = "",
    /** 十六进制色值，如 #1989fa */
    val color: String = "",
)

@Serializable
data class PoiBrief(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val description: String? = null,
)

@Serializable
data class RouteBrief(
    val id: Int = 0,
    val name: String = "",
    /** 路线主题，如「人文古迹」 */
    val theme: String = "",
    val description: String? = null,
    @SerialName("estimated_minutes")
    val estimatedMinutes: Int = 0,
)

@Serializable
data class HomeStats(
    val totalInteractions: Int = 0,
    val todayInteractions: Int = 0,
    /** 0~1 之间的小数，后端四舍五入到两位 */
    val positiveRate: Double = 0.0,
)

@Serializable
data class HomeContent(
    val banners: List<Banner> = emptyList(),
    val quickEntries: List<QuickEntry> = emptyList(),
    val hotPois: List<PoiBrief> = emptyList(),
    val recommendedRoutes: List<RouteBrief> = emptyList(),
    val stats: HomeStats = HomeStats(),
)
