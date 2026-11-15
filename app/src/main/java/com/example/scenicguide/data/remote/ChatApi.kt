package com.example.scenicguide.data.remote

import com.example.scenicguide.core.network.ApiEnvelope
import com.example.scenicguide.data.model.ChatStopRequest
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApi {

    /** 停止对话（规划态接口，失败不影响本地中断） */
    @POST("api/v1/chat/stop")
    suspend fun stop(@Body body: ChatStopRequest): ApiEnvelope<JsonElement>
}
