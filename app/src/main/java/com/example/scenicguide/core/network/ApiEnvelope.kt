package com.example.scenicguide.core.network

import kotlinx.serialization.Serializable

/**
 * 后端统一返回包络：{ "code": 0, "message": "ok", "data": ... }
 * code == 0 表示成功，其余见 [ApiError.of]。
 */
@Serializable
data class ApiEnvelope<T>(
    val code: Int = -1,
    val message: String = "",
    val data: T? = null,
)
