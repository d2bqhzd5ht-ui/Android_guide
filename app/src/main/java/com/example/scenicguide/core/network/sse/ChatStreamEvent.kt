package com.example.scenicguide.core.network.sse

/** 问答 SSE 解析后的事件，UI 只消费这四类 */
sealed interface ChatStreamEvent {
    /** 响应头 X-Session-Id，用于 /chat/stop */
    data class SessionStarted(val sessionId: String) : ChatStreamEvent

    /** 增量文本 */
    data class Token(val content: String) : ChatStreamEvent

    /** 正常结束；error 之后补发的空 done 会让 [fullContent] 为 null */
    data class Done(val fullContent: String?) : ChatStreamEvent

    /** 异常结束 */
    data class Failed(val message: String) : ChatStreamEvent
}
