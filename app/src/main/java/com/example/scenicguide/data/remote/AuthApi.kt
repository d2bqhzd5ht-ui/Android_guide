package com.example.scenicguide.data.remote

import com.example.scenicguide.core.network.ApiEnvelope
import com.example.scenicguide.data.model.LoginData
import com.example.scenicguide.data.model.LoginRequest
import com.example.scenicguide.data.model.RegisterData
import com.example.scenicguide.data.model.RegisterRequest
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiEnvelope<LoginData>

    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiEnvelope<RegisterData>

    /** 需要 token，用来在冷启动时校验本地登录态是否还有效 */
    @GET("api/v1/auth/me")
    suspend fun me(): ApiEnvelope<JsonObject>
}
