package com.example.scenicguide.core.session

data class SessionUser(
    val id: String,
    val username: String,
    val nickname: String?,
    val role: String?,
)

data class Session(
    val token: String,
    val user: SessionUser,
)
