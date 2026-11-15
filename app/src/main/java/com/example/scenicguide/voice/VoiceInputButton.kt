package com.example.scenicguide.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.io.File

/**
 * 问答页语音入口：点击开始录音，再次点击结束并上传识别。
 * 识别成功后通过 [onRecognized] 把文字交给调用方，失败通过 [onError] 提示。
 */
@Composable
fun VoiceInputButton(
    voiceClient: VoiceClient,
    enabled: Boolean,
    onRecognized: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember(context) { VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var recognizing by remember { mutableStateOf(false) }

    fun startRecording() {
        val outputFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        try {
            recorder.start(outputFile)
            recording = true
            VoiceForegroundService.startForRecording(context)
        } catch (error: Exception) {
            recorder.cancel()
            VoiceForegroundService.stop(context)
            onError(error.message ?: "录音启动失败")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            onError("需要录音权限才能语音提问")
        }
    }

    fun stopAndRecognize() {
        val audioFile = try {
            recorder.stop()
        } catch (error: RuntimeException) {
            null
        }
        recording = false
        VoiceForegroundService.stop(context)
        if (audioFile == null) {
            onError("没有录到声音，请重新录制")
            return
        }

        recognizing = true
        scope.launch {
            try {
                onRecognized(voiceClient.transcribe(audioFile))
            } catch (error: Exception) {
                onError(error.message ?: "语音识别失败")
            } finally {
                audioFile.delete()
                recognizing = false
            }
        }
    }

    DisposableEffect(context) {
        onDispose {
            recorder.cancel()
            VoiceForegroundService.stop(context)
        }
    }

    TextButton(
        modifier = modifier,
        enabled = enabled && !recognizing,
        onClick = {
            when {
                recording -> stopAndRecognize()
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED -> startRecording()

                else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
    ) {
        when {
            recognizing -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )

            recording -> Text("结束")
            else -> Text("语音")
        }
    }
}
