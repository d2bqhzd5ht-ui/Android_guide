package com.example.scenicguide.voice

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Dispatcher
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okio.BufferedSource
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/**
 * 语音链路客户端：
 * - ASR 走 Retrofit multipart，复用网络基座的拦截器；
 * - TTS 是 GET + SSE（meta / audio / done / error），readTimeout 必须为 0，
 *   且使用独立 Dispatcher，方便停止时只取消语音请求。
 */
class VoiceClient(
    baseClient: OkHttpClient,
    baseUrl: String,
    private val api: VoiceApi,
) {
    private val baseUrl = baseUrl.trimEnd('/')

    private val sseClient: OkHttpClient = baseClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .dispatcher(Dispatcher())
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun transcribe(audio: File): String {
        if (!audio.isFile || audio.length() == 0L) {
            throw VoiceException("录音文件为空，请重新录制")
        }
        val part = MultipartBody.Part.createFormData(
            "audio",
            audio.name,
            audio.asRequestBody("audio/mp4".toMediaType()),
        )
        val envelope = api.transcribe(part)
        if (envelope.code != 0) {
            throw VoiceException(
                envelope.message.ifBlank { "语音识别失败（code=${envelope.code}）" },
            )
        }
        val text = envelope.data?.text?.trim().orEmpty()
        if (text.isEmpty()) throw VoiceException("没有识别到内容，请再说一次")
        return text
    }

    /**
     * 合成一段文字（不超过 500 字）并按 seq 顺序写入 [outputFile]。
     * 收到 done 且分片完整才返回，异常时删除未完成的文件。
     */
    suspend fun synthesizeToFile(text: String, outputFile: File) = withContext(Dispatchers.IO) {
        if (text.isBlank()) throw VoiceException("合成文本不能为空")
        if (text.length > TtsTextSplitter.MAX_LENGTH) {
            throw VoiceException("单次合成不能超过 ${TtsTextSplitter.MAX_LENGTH} 字")
        }

        val url = (baseUrl + TTS_PATH).toHttpUrl().newBuilder()
            .addQueryParameter("text", text)
            .addQueryParameter("lastSeq", "0")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .get()
            .build()

        try {
            sseClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val bodyText = runCatching { response.body.string() }.getOrDefault("")
                    throw VoiceException(
                        extractError(bodyText, "语音合成失败（HTTP ${response.code}）"),
                    )
                }
                response.header("X-Trace-Id")?.let { Log.d(TAG, "TTS 已连接，X-Trace-Id=$it") }
                FileOutputStream(outputFile).buffered().use { output ->
                    readAudioStream(response.body.source(), output)
                }
            }
        } catch (error: Exception) {
            outputFile.delete()
            throw error
        }
    }

    /** 停止当前语音合成请求（只影响语音链路的独立 Dispatcher）。 */
    fun cancelTts() {
        sseClient.dispatcher.cancelAll()
    }

    private fun readAudioStream(source: BufferedSource, output: OutputStream) {
        var eventType: String? = null
        val eventData = StringBuilder()
        val pendingChunks = mutableMapOf<Int, String>()
        var lastWrittenSeq = 0
        var completed = false

        fun dispatch(type: String, raw: String) {
            when (type) {
                "meta" -> Log.d(TAG, "TTS meta：$raw")

                "audio" -> {
                    val element = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
                        ?: throw VoiceException("音频分片格式错误")
                    val seq = element["seq"]?.jsonPrimitive?.int
                        ?: throw VoiceException("音频分片缺少 seq")
                    val chunk = element["chunk"]?.jsonPrimitive?.content
                        ?: throw VoiceException("音频分片缺少 chunk")
                    if (seq <= lastWrittenSeq) return

                    pendingChunks[seq] = chunk
                    while (true) {
                        val next = pendingChunks.remove(lastWrittenSeq + 1) ?: break
                        val bytes = runCatching { Base64.decode(next, Base64.DEFAULT) }
                            .getOrElse { throw VoiceException("音频分片解码失败", it) }
                        output.write(bytes)
                        lastWrittenSeq++
                    }
                }

                "done" -> {
                    if (pendingChunks.isNotEmpty()) throw VoiceException("音频分片序号不连续")
                    if (lastWrittenSeq == 0) throw VoiceException("没有收到音频数据")
                    Log.d(TAG, "TTS 接收完成，共 $lastWrittenSeq 个分片")
                    completed = true
                }

                "error" -> {
                    val element = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
                    val message = element?.get("message")?.jsonPrimitive?.content.orEmpty()
                    val code = element?.get("code")?.jsonPrimitive?.int
                    throw VoiceException(
                        when {
                            message.isNotBlank() && code != null -> "$message（code=$code）"
                            message.isNotBlank() -> message
                            else -> "语音合成失败"
                        },
                    )
                }
            }
        }

        while (!completed) {
            val line = source.readUtf8Line() ?: break
            when {
                line.isEmpty() -> {
                    val type = eventType
                    if (type != null && eventData.isNotEmpty()) {
                        dispatch(type, eventData.toString())
                    }
                    eventType = null
                    eventData.setLength(0)
                }

                line.startsWith(":") -> Unit
                line.startsWith("event:") -> eventType = line.removePrefix("event:").trim()
                line.startsWith("data:") -> {
                    if (eventData.isNotEmpty()) eventData.append('\n')
                    eventData.append(line.removePrefix("data:").trim())
                }
            }
        }

        if (!completed) throw VoiceException("语音合成连接中断，请重试")
    }

    private fun extractError(bodyText: String, fallback: String): String {
        val element = runCatching { json.parseToJsonElement(bodyText).jsonObject }.getOrNull()
            ?: return fallback
        val message = element["message"]?.jsonPrimitive?.content.orEmpty()
        val code = element["code"]?.jsonPrimitive?.int
        return when {
            message.isNotBlank() && code != null -> "$message（code=$code）"
            message.isNotBlank() -> message
            else -> fallback
        }
    }

    private companion object {
        const val TAG = "VoiceClient"
        const val TTS_PATH = "/api/v1/tts/stream"
    }
}
