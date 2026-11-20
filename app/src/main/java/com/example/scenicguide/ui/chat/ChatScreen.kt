package com.example.scenicguide.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scenicguide.ui.rememberAppContainer
import com.example.scenicguide.voice.TtsPlayerState
import com.example.scenicguide.voice.VoiceInputButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    /**
     * 语音入口插槽（A 的 VoiceInputButton 从这里进来）：
     * `voiceInput = { onText -> VoiceInputButton(voiceClient, onRecognized = onText, ...) }`
     * 识别出的文字只会回填到输入框，不自动发送。传 null 时使用内置的 VoiceInputButton。
     */
    voiceInput: (@Composable (onRecognized: (String) -> Unit) -> Unit)? = null,
) {
    val container = rememberAppContainer()
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.factory(container.chatRepository),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ttsState by container.ttsPlayer.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var voiceError by remember { mutableStateOf<String?>(null) }
    val speaking = ttsState is TtsPlayerState.Synthesizing ||
        ttsState is TtsPlayerState.Playing

    // 离开页面（返回/切屏被销毁）时中断流式请求和语音播报
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stop()
            container.ttsPlayer.stop()
        }
    }

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI 问答", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = when {
                                speaking && ttsState is TtsPlayerState.Synthesizing -> "正在合成语音…"
                                speaking -> "正在播报语音"
                                state.streaming -> "正在流式回答…"
                                else -> "景区知识随时问"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("‹ 返回") }
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                (voiceError ?: state.error)?.let { message ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                voiceError = null
                                viewModel.dismissError()
                            },
                        ) { Text("知道了") }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.input,
                        onValueChange = viewModel::onInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("问问景区的事…") },
                        maxLines = 4,
                        trailingIcon = {
                            // 识别结果只回填输入框，由用户确认后再发送
                            val onRecognized: (String) -> Unit = { text ->
                                voiceError = null
                                viewModel.onInputChange(text)
                            }
                            if (voiceInput != null) {
                                voiceInput(onRecognized)
                            } else {
                                VoiceInputButton(
                                    voiceClient = container.voiceClient,
                                    enabled = !state.streaming,
                                    onRecognized = onRecognized,
                                    onError = { voiceError = it },
                                )
                            }
                        },
                    )
                    if (state.streaming) {
                        Button(
                            onClick = viewModel::stop,
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Text("停止")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.send() },
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Text("发送")
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "试试这样问",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "“这个景区有什么典故？”\n“带孩子逛一天怎么安排路线？”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                val lastAssistantId = state.messages.lastOrNull { !it.fromUser }?.id
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.messages) { message ->
                        ChatBubble(
                            message = message,
                            showSpeak = message.id == lastAssistantId &&
                                !message.streaming &&
                                message.content.isNotBlank(),
                            speaking = speaking,
                            onSpeak = { container.ttsPlayer.speak(message.content) },
                            onStopSpeak = { container.ttsPlayer.stop() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatViewModel.Message,
    showSpeak: Boolean,
    speaking: Boolean,
    onSpeak: () -> Unit,
    onStopSpeak: () -> Unit,
) {
    val isUser = message.fromUser
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (isUser) 14.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 14.dp,
                ),
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(0.85f),
            ) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    when {
                        message.content.isEmpty() && message.streaming -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "思考中…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }

                        else -> Text(
                            text = if (message.streaming) message.content + "▌" else message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (showSpeak) {
            TextButton(onClick = if (speaking) onStopSpeak else onSpeak) {
                Text(if (speaking) "停止播报" else "朗读回答")
            }
        }
    }
}
