package com.example.scenicguide.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit / OkHttp 装配。SSE 走单独的 client（readTimeout = 0），见 ChatSseClient。
 */
object NetworkModule {

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        coerceInputValues = true
    }

    private val jsonMediaType = "application/json".toMediaType()

    fun createOkHttp(tokenProvider: () -> String?): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenProvider))
            .addInterceptor(TraceLoggingInterceptor())
            .build()

    fun createRetrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(jsonMediaType))
            .build()
}
