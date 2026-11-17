package com.example.scenicguide.data.remote

import com.example.scenicguide.core.network.ApiEnvelope
import com.example.scenicguide.data.model.PoiDetail
import com.example.scenicguide.data.model.PoiListItem
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PoiApi {

    /**
     * 景点列表
     * @param category 可选类别筛选：attraction / facility / landmark / food / other
     * @param all 是否包含下架项（传 "1" 则 status=0 的也返回）
     */
    @GET("api/v1/poi")
    suspend fun listPois(
        @Query("category") category: String? = null,
        @Query("all") all: String? = null,
    ): ApiEnvelope<List<PoiListItem>>

    /**
     * 景点详情
     */
    @GET("api/v1/poi/{id}")
    suspend fun getPoiDetail(@Path("id") id: Int): ApiEnvelope<PoiDetail>
}
