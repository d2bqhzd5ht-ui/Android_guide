package com.example.scenicguide.core.network

/**
 * 统一错误模型。业务错误码来自后端文档 docs/交接.md 1.1 节，
 * 本地错误码用负数，避免和后端码段冲突。
 */
data class ApiError(
    val code: Int,
    val message: String,
    val unauthorized: Boolean = false,
) {
    companion object {
        const val CODE_NETWORK = -100
        const val CODE_TIMEOUT = -101
        const val CODE_PARSE = -102
        const val CODE_HTTP = -103

        private val businessMessages = mapOf(
            40001 to "请求参数不合法",
            40002 to "上传文件缺失或类型不支持",
            40003 to "上传文件超出体积上限",
            40101 to "请先登录",
            40102 to "登录已过期，请重新登录",
            40301 to "权限不足",
            50000 to "服务端开小差了，请稍后重试",
            50001 to "语音识别服务暂不可用",
            50002 to "语音合成服务暂不可用",
        )

        /** 把后端 code / 本地 code 映射成带用户可读文案的错误对象 */
        fun of(code: Int, message: String? = null): ApiError {
            val unauthorized = code == 40101 || code == 40102
            val text = message?.takeIf { it.isNotBlank() } ?: businessMessages[code] ?: "未知错误（$code）"
            return ApiError(code = code, message = text, unauthorized = unauthorized)
        }
    }
}
