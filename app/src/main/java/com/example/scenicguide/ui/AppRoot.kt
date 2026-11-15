package com.example.scenicguide.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.scenicguide.ScenicGuideApplication
import com.example.scenicguide.core.di.AppContainer
import com.example.scenicguide.ui.chat.ChatScreen
import com.example.scenicguide.ui.home.HomeScreen
import com.example.scenicguide.ui.login.LoginScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val CHAT = "chat"
}

@Composable
fun rememberAppContainer(): AppContainer {
    val app = LocalContext.current.applicationContext as ScenicGuideApplication
    return remember { app.container }
}

@Composable
fun AppRoot() {
    val container = rememberAppContainer()
    val navController = rememberNavController()

    val session by container.sessionManager.session.collectAsStateWithLifecycle()
    var sessionReady by remember { mutableStateOf(false) }

    // 冷启动：先恢复 DataStore 里的登录态，再决定进首页还是登录页
    LaunchedEffect(Unit) {
        container.restoreSession()
        sessionReady = true
        if (container.sessionManager.currentToken() != null) {
            container.authRepository.validateSession()
        }
    }

    // 任意接口返回 40101 / 40102 → 清登录态 + 回登录页
    LaunchedEffect(Unit) {
        container.sessionManager.unauthorized.collect {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (!sessionReady) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = "景区 AI 导览",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        } else {
            NavHost(
                navController = navController,
                startDestination = if (session != null) Routes.HOME else Routes.LOGIN,
            ) {
                composable(Routes.LOGIN) {
                    LoginScreen(
                        onLoggedIn = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenChat = { navController.navigate(Routes.CHAT) },
                        onLoggedOut = {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.CHAT) {
                    ChatScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
