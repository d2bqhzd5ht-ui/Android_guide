package com.example.scenicguide.data.repository

import com.example.scenicguide.core.network.NetworkModule
import com.example.scenicguide.core.network.sse.ChatSseClient
import com.example.scenicguide.core.network.sse.ChatStreamEvent
import com.example.scenicguide.data.model.ChatRequest
import com.example.scenicguide.data.model.ChatStopRequest
import com.example.scenicguide.data.model.ChatTurn
import com.example.scenicguide.data.remote.ChatApi
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString

class ChatRepository(
    private val api: ChatApi,
    private val sseClient: ChatSseClient,
) {

    /** SSE 流：调用方取消协程即断开连接 */
    fun stream(turns: List<ChatTurn>): Flow<ChatStreamEvent> {
        val payload = NetworkModule.json.encodeToString(ChatRequest(turns))
        return sseClient.stream(payload)
    }

    /** 通知后端停止生成，失败无所谓，本地已经取消了 */
    suspend fun stop(sessionId: String) {
        runCatching { api.stop(ChatStopRequest(sessionId)) }
    }
}
