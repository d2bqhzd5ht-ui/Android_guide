package com.example.scenicguide.core.network

/** 网络层统一结果类型，UI 只关心 Success / Failure 两支 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data
