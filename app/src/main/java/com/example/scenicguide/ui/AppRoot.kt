@file:Suppress("DEPRECATION")

package com.example.scenicguide.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.scenicguide.ScenicGuideApplication
import com.example.scenicguide.core.di.AppContainer
import com.example.scenicguide.ui.chat.ChatScreen
import com.example.scenicguide.ui.home.HomeScreen
import com.example.scenicguide.ui.login.LoginScreen
import com.example.scenicguide.ui.login.RegisterScreen
import com.example.scenicguide.ui.lostfound.LostFoundDetailScreen
import com.example.scenicguide.ui.lostfound.LostFoundEditScreen
import com.example.scenicguide.ui.lostfound.LostFoundListScreen
import com.example.scenicguide.ui.lostfound.LostFoundPublishScreen
import com.example.scenicguide.ui.poi.PoiDetailScreen
import com.example.scenicguide.ui.poi.PoiListScreen
import com.example.scenicguide.ui.profile.ProfileScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val CHAT = "chat"
    const val PROFILE = "profile"
    const val LOST_FOUND = "lost_found"
    const val LOST_FOUND_DETAIL = "lost_found/{itemId}"
    const val LOST_FOUND_PUBLISH = "lost_found_publish"
    const val POI_LIST = "poi_list"
    const val POI_DETAIL = "poi/{poiId}"
    const val LOST_FOUND_EDIT = "lost_found/{itemId}/edit"

    fun lostFoundDetail(itemId: Int) = "lost_found/$itemId"
    fun lostFoundEdit(itemId: Int) = "lost_found/$itemId/edit"
    fun poiDetail(poiId: Int) = "poi/$poiId"
}

private data class GuideTab(
    val route: String?,
    val label: String,
    val icon: ImageVector,
)

private val topLevelRoutes = setOf(Routes.HOME, Routes.CHAT, Routes.POI_LIST, Routes.PROFILE)

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

    // 任意接口返回 40101 / 40102 → 清登录态 + 提示 + 回登录页
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        container.sessionManager.unauthorized.collect {
            Toast.makeText(context, "登录已过期，请重新登录", Toast.LENGTH_SHORT).show()
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val showBottomBar = !imeVisible && currentRoute != null && currentRoute in topLevelRoutes

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (!sessionReady) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = "西湖导览",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = if (session != null) Routes.HOME else Routes.LOGIN,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        composable(Routes.LOGIN) {
                            LoginScreen(
                                onLoggedIn = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                },
                                onGoRegister = { navController.navigate(Routes.REGISTER) },
                            )
                        }
                        composable(Routes.REGISTER) {
                            RegisterScreen(
                                onRegistered = {
                                    // 注册成功 → 回登录页，用户手动输入新账号登录
                                    navController.popBackStack()
                                },
                                onBackToLogin = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.HOME) {
                            HomeScreen(
                                onOpenChat = { navController.navigate(Routes.CHAT) },
                                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                                onOpenLostFound = { navController.navigate(Routes.LOST_FOUND) },
                                onOpenPoiList = { navController.navigate(Routes.POI_LIST) },
                                onOpenPoiDetail = { poiId ->
                                    navController.navigate(Routes.poiDetail(poiId))
                                },
                            )
                        }
                        composable(Routes.LOST_FOUND) {
                            LostFoundListScreen(
                                onBack = { navController.popBackStack() },
                                onOpenDetail = { itemId ->
                                    navController.navigate(Routes.lostFoundDetail(itemId))
                                },
                                onPublish = { navController.navigate(Routes.LOST_FOUND_PUBLISH) },
                            )
                        }
                        composable(
                            route = Routes.LOST_FOUND_DETAIL,
                            arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
                        ) { entry ->
                            val itemId = entry.arguments?.getInt("itemId") ?: 0
                            LostFoundDetailScreen(
                                itemId = itemId,
                                onBack = { navController.popBackStack() },
                                onEdit = { id -> navController.navigate(Routes.lostFoundEdit(id)) },
                                onDeleted = { navController.popBackStack() },
                            )
                        }
                        composable(
                            route = Routes.LOST_FOUND_EDIT,
                            arguments = listOf(navArgument("itemId") { type = NavType.IntType }),
                        ) { entry ->
                            val itemId = entry.arguments?.getInt("itemId") ?: 0
                            LostFoundEditScreen(
                                itemId = itemId,
                                onBack = { navController.popBackStack() },
                                onSaved = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.LOST_FOUND_PUBLISH) {
                            LostFoundPublishScreen(
                                onBack = { navController.popBackStack() },
                                onPublished = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.CHAT) {
                            ChatScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.PROFILE) {
                            ProfileScreen(
                                onBack = { navController.popBackStack() },
                                onLoggedOut = {
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(Routes.POI_LIST) {
                            PoiListScreen(
                                onBack = { navController.popBackStack() },
                                onOpenDetail = { poiId ->
                                    navController.navigate(Routes.poiDetail(poiId))
                                },
                            )
                        }
                        composable(
                            route = Routes.POI_DETAIL,
                            arguments = listOf(navArgument("poiId") { type = NavType.IntType }),
                        ) { entry ->
                            val poiId = entry.arguments?.getInt("poiId") ?: 0
                            PoiDetailScreen(
                                poiId = poiId,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
                if (showBottomBar) {
                    GuideBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onRoutesUnavailable = {
                            Toast.makeText(context, "游玩攻略暂未开放", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onRoutesUnavailable: () -> Unit,
) {
    val tabs = listOf(
        GuideTab(Routes.HOME, "首页", Icons.Outlined.Home),
        GuideTab(Routes.CHAT, "AI 导览", Icons.Outlined.Send),
        GuideTab(null, "游玩攻略", Icons.Outlined.List),
        GuideTab(Routes.POI_LIST, "景点", Icons.Outlined.Place),
        GuideTab(Routes.PROFILE, "我的", Icons.Outlined.Person),
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEach { tab ->
            val route = tab.route
            NavigationBarItem(
                selected = route != null && currentRoute == route,
                onClick = {
                    if (route == null) onRoutesUnavailable() else onNavigate(route)
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}
