package com.example.scenicguide.core.network.sse

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 手写 SSE 客户端。原因：/chat/stream 是 POST，Android 没有内置 EventSource，
 * Retrofit 的 Gson/kotlinx converter 也处理不了流式响应。
 *
 * 规则（见 docs/交接.md 3.2）：
 * - readTimeout 必须为 0，否则 25 秒心跳间隔内没数据会被判超时；
 * - 冒号开头是心跳注释，直接忽略；
 * - 事件以空行分隔，字段是 event / data / id；
 * - error 事件之后还会补发一个空 done，调用方要做可空处理。
 */
class ChatSseClient(
    baseClient: OkHttpClient,
    private val baseUrl: String,
    private val tokenProvider: () -> String?,
) {
    private val client: OkHttpClient = baseClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun stream(requestJson: String): Flow<ChatStreamEvent> = channelFlow {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/v1/chat/stream")
            .header("Accept", "text/event-stream")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .apply {
                tokenProvider()?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
            }
            .build()

        val call = client.newCall(request)

        suspend fun dispatch(event: String, raw: String) {
            when (event) {
                "token" -> {
                    val content = runCatching {
                        json.parseToJsonElement(raw).jsonObject["content"]?.jsonPrimitive?.content
                    }.getOrNull()
                    if (!content.isNullOrEmpty()) send(ChatStreamEvent.Token(content))
                }

                "done" -> {
                    val full = runCatching {
                        json.parseToJsonElement(raw).jsonObject["fullContent"]?.jsonPrimitive?.content
                    }.getOrNull()
                    send(ChatStreamEvent.Done(full))
                }

                "error" -> {
                    val message = runCatching {
                        json.parseToJsonElement(raw).jsonObject["message"]?.jsonPrimitive?.content
                    }.getOrNull()
                    send(ChatStreamEvent.Failed(message ?: "问答服务异常"))
                }

                else -> Unit
            }
        }

        val job = launch(Dispatchers.IO) {
            try {
                call.execute().use { response ->
                    response.header("X-Session-Id")?.takeIf { it.isNotBlank() }
                        ?.let { send(ChatStreamEvent.SessionStarted(it)) }

                    if (!response.isSuccessful) {
                        val bodyText = runCatching { response.body.string() }.getOrNull().orEmpty()
                        val message = runCatching {
                            json.parseToJsonElement(bodyText).jsonObject["error"]?.jsonPrimitive?.content
                        }.getOrNull() ?: "请求失败（HTTP ${response.code}）"
                        send(ChatStreamEvent.Failed(message))
                        return@use
                    }

                    val source = response.body.source()
                    var event: String? = null
                    val data = StringBuilder()

                    while (isActive) {
                        val line = source.readUtf8Line() ?: break
                        when {
                            line.isEmpty() -> {
                                val name = event
                                if (name != null && data.isNotEmpty()) dispatch(name, data.toString())
                                event = null
                                data.clear()
                            }

                            line.startsWith(":") -> Unit
                            line.startsWith("event:") -> event = line.removePrefix("event:").trim()
                            line.startsWith("data:") -> {
                                if (data.isNotEmpty()) data.append('\n')
                                data.append(line.removePrefix("data:").trim())
                            }
                        }
                    }
                    // 流末尾没有空行兜底
                    val name = event
                    if (name != null && data.isNotEmpty()) dispatch(name, data.toString())
                }
            } catch (e: IOException) {
                if (isActive) send(ChatStreamEvent.Failed("连接中断：${e.message ?: "请稍后重试"}"))
            }
        }

        awaitClose {
            job.cancel()
            call.cancel()
        }
    }
}
