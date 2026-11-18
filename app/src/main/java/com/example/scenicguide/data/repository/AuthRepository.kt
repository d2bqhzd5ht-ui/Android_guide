package com.example.scenicguide.data.repository

import com.example.scenicguide.core.network.ApiError
import com.example.scenicguide.core.network.ApiResult
import com.example.scenicguide.core.network.apiCall
import com.example.scenicguide.core.session.Session
import com.example.scenicguide.core.session.SessionManager
import com.example.scenicguide.core.session.SessionUser
import com.example.scenicguide.data.model.LoginRequest
import com.example.scenicguide.data.model.toSessionUser
import com.example.scenicguide.data.remote.AuthApi

class AuthRepository(
    private val api: AuthApi,
    private val sessionManager: SessionManager,
) {

    suspend fun login(username: String, password: String): ApiResult<Session> {
        val result = apiCall { api.login(LoginRequest(username.trim(), password)) }
        return when (result) {
            is ApiResult.Failure -> {
                if (result.error.unauthorized) sessionManager.onUnauthorized()
                result
            }

            is ApiResult.Success -> {
                val data = result.data
                if (data.token.isBlank()) {
                    return ApiResult.Failure(ApiError(-1, "登录返回缺少 token"))
                }
                val user: SessionUser = data.user?.toSessionUser(username.trim())
                    ?: SessionUser(id = "", username = username.trim(), nickname = null, role = null)
                val session = Session(token = data.token, user = user)
                sessionManager.save(session)
                ApiResult.Success(session)
            }
        }
    }

    /** 冷启动自动登录时校验 token；401 会触发统一的登出广播 */
    suspend fun validateSession() {
        val result = apiCall { api.me() }
        if (result is ApiResult.Failure && result.error.unauthorized) {
            sessionManager.onUnauthorized()
        }
    }

    suspend fun logout() = sessionManager.logout()

    /** 个人中心刷新资料：拉取 /auth/me 并同步本地登录态 */
    suspend fun refreshProfile(): ApiResult<SessionUser> {
        val result = apiCall { api.me() }
        return when (result) {
            is ApiResult.Failure -> {
                if (result.error.unauthorized) sessionManager.onUnauthorized()
                result
            }

            is ApiResult.Success -> {
                val current = sessionManager.session.value
                val user = result.data.toSessionUser(current?.user?.username.orEmpty())
                if (current != null) sessionManager.save(current.copy(user = user))
                ApiResult.Success(user)
            }
        }
    }
}
