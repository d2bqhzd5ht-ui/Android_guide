package com.example.scenicguide.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scenicguide.core.network.sse.ChatStreamEvent
import com.example.scenicguide.data.model.ChatTurn
import com.example.scenicguide.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    data class Message(
        val id: Long,
        val fromUser: Boolean,
        val content: String,
        val streaming: Boolean = false,
    )

    data class UiState(
        val messages: List<Message> = emptyList(),
        val input: String = "",
        val streaming: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var nextId = 0L
    private var streamJob: Job? = null
    private var sessionId: String? = null

    fun onInputChange(value: String) = _state.update { it.copy(input = value) }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun send(text: String = _state.value.input) {
        val content = text.trim()
        val current = _state.value
        if (content.isEmpty() || current.streaming) return

        // 多轮上下文：把已有对话按 role 带上去
        val history = current.messages
            .filter { it.content.isNotBlank() }
            .map { ChatTurn(role = if (it.fromUser) "user" else "assistant", content = it.content) } +
            ChatTurn(role = "user", content = content)

        val userMessageId = nextId++
        val replyId = nextId++
        _state.update {
            it.copy(
                messages = it.messages +
                    Message(id = userMessageId, fromUser = true, content = content) +
                    Message(id = replyId, fromUser = false, content = "", streaming = true),
                input = "",
                streaming = true,
                error = null,
            )
        }

        streamJob = viewModelScope.launch {
            try {
                repository.stream(history).collect { event ->
                    when (event) {
                        is ChatStreamEvent.SessionStarted -> sessionId = event.sessionId
                        is ChatStreamEvent.Token -> appendToken(replyId, event.content)
                        is ChatStreamEvent.Done -> finishReply(replyId, event.fullContent)
                        is ChatStreamEvent.Failed -> {
                            finishReply(replyId, null)
                            _state.update { it.copy(streaming = false, error = event.message) }
                        }
                    }
                }
            } finally {
                markAllFinished()
            }
        }
    }

    /** 用户主动中断：取消本地流 + 通知后端停止生成 */
    fun stop() {
        streamJob?.cancel()
        streamJob = null
        markAllFinished()
        sessionId?.let { id ->
            viewModelScope.launch { repository.stop(id) }
        }
    }

    private fun appendToken(id: Long, token: String) {
        _state.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == id) message.copy(content = message.content + token) else message
                },
            )
        }
    }

    private fun finishReply(id: Long, fullContent: String?) {
        _state.update { state ->
            state.copy(
                streaming = false,
                messages = state.messages.map { message ->
                    if (message.id != id) {
                        message
                    } else {
                        // error 之后补发的空 done 只有 {}，fullContent 为 null，保持已拼好的增量文本
                        message.copy(
                            content = fullContent?.takeIf { it.isNotBlank() } ?: message.content,
                            streaming = false,
                        )
                    }
                },
            )
        }
    }

    private fun markAllFinished() {
        _state.update { state ->
            state.copy(
                streaming = false,
                messages = state.messages.map { if (it.streaming) it.copy(streaming = false) else it },
            )
        }
    }

    companion object {
        fun factory(repository: ChatRepository) = viewModelFactory {
            initializer { ChatViewModel(repository) }
        }
    }
}
