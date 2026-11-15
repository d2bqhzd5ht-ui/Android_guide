package com.example.scenicguide.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 统一给请求带上 JWT。token 从内存里的会话取出，避免在 OkHttp 线程里阻塞读 DataStore。
 */
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
