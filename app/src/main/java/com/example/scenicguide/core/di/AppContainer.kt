package com.example.scenicguide.core.di

import android.content.Context
import com.example.scenicguide.BuildConfig
import com.example.scenicguide.core.network.NetworkModule
import com.example.scenicguide.core.network.sse.ChatSseClient
import com.example.scenicguide.core.session.SessionManager
import com.example.scenicguide.core.session.UserPreferences
import com.example.scenicguide.data.remote.AuthApi
import com.example.scenicguide.data.remote.ChatApi
import com.example.scenicguide.data.remote.HomeApi
import com.example.scenicguide.data.repository.AuthRepository
import com.example.scenicguide.data.repository.ChatRepository
import com.example.scenicguide.data.repository.HomeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 手写依赖容器（最小骨架阶段不引入 Hilt，等 D 的骨架定下来再决定换不换）。
 */
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sessionManager = SessionManager(UserPreferences(context), appScope)

    private val okHttpClient = NetworkModule.createOkHttp { sessionManager.currentToken() }

    private val retrofit = NetworkModule.createRetrofit(BuildConfig.BASE_URL, okHttpClient)

    private val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    private val homeApi: HomeApi = retrofit.create(HomeApi::class.java)
    private val chatApi: ChatApi = retrofit.create(ChatApi::class.java)

    private val sseClient = ChatSseClient(
        baseClient = okHttpClient,
        baseUrl = BuildConfig.BASE_URL,
        tokenProvider = { sessionManager.currentToken() },
    )

    val authRepository = AuthRepository(authApi, sessionManager)
    val homeRepository = HomeRepository(homeApi)
    val chatRepository = ChatRepository(chatApi, sseClient)

    suspend fun restoreSession() = sessionManager.restore()
}
