package com.example.scenicguide.data.model

import com.example.scenicguide.core.network.string
import com.example.scenicguide.core.session.SessionUser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null,
    val nickname: String? = null,
)

/** 登录返回：{ user: {...}, token } */
@Serializable
data class LoginData(
    val token: String = "",
    val user: JsonObject? = null,
)

@Serializable
data class RegisterData(val id: JsonElement? = null)

@Serializable
data class ChatStopRequest(val sessionId: String, val reason: String = "user_cancel")

@Serializable
data class ChatTurn(val role: String, val content: String)

@Serializable
data class ChatRequest(val messages: List<ChatTurn>)

/** 登录接口里的 user 是任意结构，这里做宽松映射，id 可能是数字也可能是字符串 */
fun JsonObject.toSessionUser(fallbackUsername: String): SessionUser = SessionUser(
    id = string("id").orEmpty(),
    username = string("username") ?: fallbackUsername,
    nickname = string("nickname"),
    role = string("role"),
)
