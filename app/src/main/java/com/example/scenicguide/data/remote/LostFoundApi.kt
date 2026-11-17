package com.example.scenicguide.data.remote

import com.example.scenicguide.core.network.ApiEnvelope
import com.example.scenicguide.data.model.LostFoundDraft
import com.example.scenicguide.data.model.LostFoundItem
import com.example.scenicguide.data.model.LostFoundPage
import com.example.scenicguide.data.model.LostFoundStatusRequest
import com.example.scenicguide.data.model.LostFoundStatusResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 失物招领接口（docs/交接.md 1.2 节）。
 * 路径注意：真实实现是 `/api/v1/lost-found`，不是 openapi.yaml 里可能出现的别名。
 */
interface LostFoundApi {

    @GET("api/v1/lost-found")
    suspend fun list(
        @Query("type") type: String?,
        @Query("category") category: String?,
        @Query("status") status: String?,
        @Query("keyword") keyword: String?,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ApiEnvelope<LostFoundPage>

    @GET("api/v1/lost-found/{id}")
    suspend fun detail(@Path("id") id: Int): ApiEnvelope<LostFoundItem>

    @POST("api/v1/lost-found")
    suspend fun create(@Body draft: LostFoundDraft): ApiEnvelope<LostFoundItem>

    /** 改状态只能走 PATCH；Retrofit 没有 @PATCH 注解，用 @HTTP 声明。响应只有 { id, status } */
    @HTTP(method = "PATCH", path = "api/v1/lost-found/{id}/status", hasBody = true)
    suspend fun updateStatus(
        @Path("id") id: Int,
        @Body body: LostFoundStatusRequest,
    ): ApiEnvelope<LostFoundStatusResult>
}
