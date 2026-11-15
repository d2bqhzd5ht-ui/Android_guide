package com.example.scenicguide.data.remote

import com.example.scenicguide.core.network.ApiEnvelope
import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET

interface HomeApi {

    /** 一次拿全：banners / quickEntries / hotPois / recommendedRoutes */
    @GET("api/v1/home")
    suspend fun home(): ApiEnvelope<JsonObject>
}
