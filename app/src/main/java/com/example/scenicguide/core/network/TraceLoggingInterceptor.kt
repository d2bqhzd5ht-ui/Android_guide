package com.example.scenicguide.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 联调日志：把方法、路径、状态码、X-Trace-Id 和耗时打到 Logcat，报错时直接把 traceId 甩给后端。
 */
class TraceLoggingInterceptor(private val tag: String = "ApiTrace") : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val start = System.currentTimeMillis()
        return try {
            val response = chain.proceed(request)
            val cost = System.currentTimeMillis() - start
            Log.d(
                tag,
                "${request.method} ${request.url.encodedPath} -> ${response.code} " +
                    "traceId=${response.header("X-Trace-Id") ?: "-"} ${cost}ms",
            )
            response
        } catch (e: Exception) {
            Log.e(tag, "${request.method} ${request.url.encodedPath} 失败: ${e.message}")
            throw e
        }
    }
}
