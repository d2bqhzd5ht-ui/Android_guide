package com.example.scenicguide.voice

import android.media.MediaPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface TtsPlayerState {
    data object Idle : TtsPlayerState
    data class Synthesizing(val current: Int, val total: Int) : TtsPlayerState
    data class Playing(val current: Int, val total: Int) : TtsPlayerState
    data object Completed : TtsPlayerState
    data class Failed(val message: String) : TtsPlayerState
}

/**
 * 语音播报：长文本自动分段，逐段合成到临时文件、逐段播放，全部播完才提示完成。
 * 只操作 MediaPlayer 和临时文件，不涉及 UI，方便问答页直接调用。
 */
class TtsPlayer(
    private val client: VoiceClient,
    private val cacheDir: File,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<TtsPlayerState>(TtsPlayerState.Idle)
    val state: StateFlow<TtsPlayerState> = _state.asStateFlow()

    private var job: Job? = null
    private var player: MediaPlayer? = null
    private var currentFile: File? = null

    fun speak(text: String) {
        val segments = TtsTextSplitter.split(text)
        stop()
        if (segments.isEmpty()) {
            _state.value = TtsPlayerState.Failed("没有可播报的内容")
            return
        }

        job = scope.launch {
            try {
                segments.forEachIndexed { index, segment ->
                    _state.value = TtsPlayerState.Synthesizing(index + 1, segments.size)
                    val file = File(cacheDir, "tts_${System.currentTimeMillis()}_$index.audio")
                    currentFile = file
                    client.synthesizeToFile(segment, file)
                    ensureActive()

                    _state.value = TtsPlayerState.Playing(index + 1, segments.size)
                    play(file)

                    file.delete()
                    currentFile = null
                }
                _state.value = TtsPlayerState.Completed
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                currentFile?.delete()
                currentFile = null
                _state.value = TtsPlayerState.Failed(error.message ?: "语音播报失败")
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        client.cancelTts()
        releasePlayer()
        currentFile?.delete()
        currentFile = null
        _state.value = TtsPlayerState.Idle
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private suspend fun play(file: File) = suspendCancellableCoroutine { continuation ->
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer

        try {
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.setOnPreparedListener { it.start() }
            mediaPlayer.setOnCompletionListener {
                releasePlayer()
                if (continuation.isActive) continuation.resume(Unit)
            }
            mediaPlayer.setOnErrorListener { _, what, extra ->
                releasePlayer()
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        VoiceException("音频播放失败（what=$what, extra=$extra）"),
                    )
                }
                true
            }
            continuation.invokeOnCancellation { releasePlayer() }
            mediaPlayer.prepareAsync()
        } catch (error: Exception) {
            releasePlayer()
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }

    private fun releasePlayer() {
        player?.let { mediaPlayer ->
            mediaPlayer.setOnPreparedListener(null)
            mediaPlayer.setOnCompletionListener(null)
            mediaPlayer.setOnErrorListener(null)
            runCatching { mediaPlayer.stop() }
            runCatching { mediaPlayer.release() }
        }
        player = null
    }
}
