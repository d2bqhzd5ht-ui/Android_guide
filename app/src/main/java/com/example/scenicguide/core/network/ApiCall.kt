package com.example.scenicguide.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 所有 Retrofit 接口的统一调用入口：
 * 成功（code == 0）转成 [ApiResult.Success]，业务码 / HTTP 码 / IO 异常统一转成 [ApiResult.Failure]。
 */
suspend fun <T> apiCall(block: suspend () -> ApiEnvelope<T>): ApiResult<T> {
    return try {
        val envelope = block()
        if (envelope.code == 0) {
            val data = envelope.data
            if (data == null) {
                ApiResult.Failure(ApiError(-1, "服务端返回数据为空"))
            } else {
                ApiResult.Success(data)
            }
        } else {
            ApiResult.Failure(ApiError.of(envelope.code, envelope.message))
        }
    } catch (e: HttpException) {
        ApiResult.Failure(httpError(e))
    } catch (e: UnknownHostException) {
        ApiResult.Failure(ApiError(ApiError.CODE_NETWORK, "连不上服务器，请确认后端已启动"))
    } catch (e: SocketTimeoutException) {
        ApiResult.Failure(ApiError(ApiError.CODE_TIMEOUT, "请求超时，请检查网络"))
    } catch (e: IOException) {
        ApiResult.Failure(ApiError(ApiError.CODE_NETWORK, "网络异常：${e.message ?: "请稍后重试"}"))
    } catch (e: SerializationException) {
        ApiResult.Failure(ApiError(ApiError.CODE_PARSE, "返回数据解析失败"))
    }
}

/** 后端部分接口出错时返回 HTTP 4xx/5xx，错误体可能是 {code,message} 也可能是 {error} */
private fun httpError(e: HttpException): ApiError {
    val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val traceId = runCatching { e.response()?.headers()?.get("X-Trace-Id") }.getOrNull()
    val parsed = body?.takeIf { it.isNotBlank() }?.let { raw ->
        runCatching {
            val obj = Json.parseToJsonElement(raw) as? JsonObject
            val code = obj?.get("code")?.jsonPrimitive?.content?.toIntOrNull()
            val message = obj?.get("message")?.jsonPrimitive?.content
                ?: obj?.get("error")?.jsonPrimitive?.content
            code to message
        }.getOrNull()
    }
    val code = parsed?.first ?: if (e.code() == 401) 40101 else e.code()
    val traceSuffix = traceId?.takeIf { it.isNotBlank() }?.let { "（traceId: $it）" }.orEmpty()
    val message = ApiError.of(code, parsed?.second).message + traceSuffix
    return ApiError.of(code, message)
}

/** 从任意 JSON 里安全取字符串，兼容数字/布尔字面量 */
fun JsonObject.string(key: String): String? {
    val element = this[key] ?: return null
    return (element as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
}
