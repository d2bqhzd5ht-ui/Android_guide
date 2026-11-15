package com.example.scenicguide.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * 录音封装：AAC / MP4，44.1kHz，128kbps。
 * 只负责录音本身，权限申请和前台服务由调用方处理。
 */
class VoiceRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null

    val isRecording: Boolean
        get() = mediaRecorder != null

    fun start(outputFile: File) {
        check(mediaRecorder == null) { "Recording is already in progress" }

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(44_100)
            recorder.setAudioEncodingBitRate(128_000)
            recorder.setOutputFile(outputFile.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            currentFile = outputFile
        } catch (error: Exception) {
            recorder.release()
            outputFile.delete()
            throw error
        }
    }

    fun stop(): File {
        val recorder = mediaRecorder ?: error("Recording has not started")
        val outputFile = currentFile ?: error("Recording file is missing")

        try {
            recorder.stop()
            return outputFile
        } catch (error: RuntimeException) {
            outputFile.delete()
            throw error
        } finally {
            recorder.release()
            mediaRecorder = null
            currentFile = null
        }
    }

    fun cancel() {
        val recorder = mediaRecorder ?: return
        try {
            recorder.stop()
        } catch (_: RuntimeException) {
            // 页面销毁时优先保证麦克风资源被释放。
        } finally {
            recorder.release()
            mediaRecorder = null
            currentFile?.delete()
            currentFile = null
        }
    }
}
