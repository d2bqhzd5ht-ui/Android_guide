package com.example.scenicguide.voice

import com.example.scenicguide.core.network.ApiEnvelope
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VoiceApi {

    /** 语音识别：multipart/form-data，字段名固定为 audio。 */
    @Multipart
    @POST("api/v1/asr-tts/asr")
    suspend fun transcribe(@Part audio: MultipartBody.Part): ApiEnvelope<AsrData>
}

@Serializable
data class AsrData(
    val text: String = "",
)

/** 语音链路的业务异常，message 可直接展示给用户。 */
class VoiceException(message: String, cause: Throwable? = null) : Exception(message, cause)
