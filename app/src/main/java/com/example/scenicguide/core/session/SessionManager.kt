package com.example.scenicguide.core.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 登录态的唯一入口：
 * - 内存里缓一份 token 给 OkHttp 拦截器同步读取；
 * - DataStore 负责持久化，供下次冷启动自动登录；
 * - 401（40101 / 40102）统一走 [onUnauthorized]，清 token 并广播给 UI 回登录页。
 */
class SessionManager(
    private val preferences: UserPreferences,
    private val scope: CoroutineScope,
) {
    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorized: SharedFlow<Unit> = _unauthorized.asSharedFlow()

    @Volatile
    private var cachedToken: String? = null

    fun currentToken(): String? = cachedToken

    suspend fun restore() {
        val restored = preferences.session.first()
        cachedToken = restored?.token
        _session.value = restored
    }

    suspend fun save(session: Session) {
        preferences.save(session)
        cachedToken = session.token
        _session.value = session
    }

    suspend fun logout() {
        preferences.clear()
        cachedToken = null
        _session.value = null
    }

    /** 任何接口拿到 40101 / 40102 时调用 */
    fun onUnauthorized() {
        cachedToken = null
        _session.value = null
        scope.launch { preferences.clear() }
        _unauthorized.tryEmit(Unit)
    }
}
