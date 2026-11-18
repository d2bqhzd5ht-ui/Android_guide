package com.example.scenicguide.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scenicguide.data.model.Banner
import com.example.scenicguide.data.model.HomeContent
import com.example.scenicguide.data.model.PoiBrief
import com.example.scenicguide.data.model.QuickEntry
import com.example.scenicguide.data.model.RouteBrief
import com.example.scenicguide.ui.components.ErrorView
import com.example.scenicguide.ui.components.LoadingView
import com.example.scenicguide.ui.rememberAppContainer

private val bannerBrushes = listOf(
    listOf(Color(0xFF0E7C66), Color(0xFF4BB39A)),
    listOf(Color(0xFF1F5F8B), Color(0xFF5AA9D6)),
    listOf(Color(0xFF8B5E1F), Color(0xFFD6A85A)),
)

private val hexPattern = Regex("#[0-9a-fA-F]{6}")

/** 后端 quickEntries[].path 的目标路由（与后台配置保持一致） */
private const val LOST_FOUND_PATH = "/lost-found"
private const val CHAT_PATH = "/interaction"
private const val POI_PATH = "/poi"
private const val POI_LIST_PATH = "/poi_list"

/** 后端 banners.color 是 CSS 串（如 linear-gradient(135deg, #1989fa, #4db8ff)），取其中的色值来画渐变 */
private fun bannerBrush(css: String, fallback: List<Color>): Brush {
    val colors = hexPattern.findAll(css)
        .map { Color(("FF" + it.value.drop(1)).toLong(16)) }
        .toList()
    return when {
        colors.size >= 2 -> Brush.linearGradient(listOf(colors[0], colors[1]))
        colors.size == 1 -> Brush.linearGradient(listOf(colors[0], colors[0].copy(alpha = 0.7f)))
        else -> Brush.linearGradient(fallback)
    }
}

/** 快捷入口的 color 是单个十六进制色值 */
private fun hexColor(hex: String, fallback: Color): Color =
    hexPattern.find(hex)?.let { Color(("FF" + it.value.drop(1)).toLong(16)) } ?: fallback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenChat: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenLostFound: () -> Unit,
    onOpenPoiList: () -> Unit = {},
) {
    val container = rememberAppContainer()
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(container.homeRepository, container.authRepository),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("景区导览", fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(onClick = onOpenProfile) {
                        Text("我的")
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            is HomeViewModel.UiState.Loading -> LoadingView(
                text = "正在加载首页…",
                modifier = Modifier.padding(padding),
            )

            is HomeViewModel.UiState.Error -> ErrorView(
                message = current.message,
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(padding),
            )

            is HomeViewModel.UiState.Success -> HomeContentView(
                content = current.content,
                onOpenChat = onOpenChat,
                onOpenLostFound = onOpenLostFound,
                onOpenPoiList = onOpenPoiList,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun HomeContentView(
    content: HomeContent,
    onOpenChat: () -> Unit,
    onOpenLostFound: () -> Unit,
    onOpenPoiList: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "发现身边的风景",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "AI 导览随时为你讲解",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item { BannerPager(content.banners) }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenChat),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI 智能问答",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "边逛边问，流式回答随问随到",
                            color = Color(0xFFCDEFE5),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(text = "去提问 ›", color = Color.White)
                }
            }
        }

        if (content.quickEntries.isNotEmpty()) {
            item { SectionTitle("快捷入口") }
            item {
                QuickEntryRow(
                    entries = content.quickEntries,
                    onEntryClick = { entry ->
                        when (entry.path) {
                            LOST_FOUND_PATH -> onOpenLostFound()
                            CHAT_PATH -> onOpenChat()
                            POI_PATH, POI_LIST_PATH -> onOpenPoiList()
                            else -> Unit
                        }
                    },
                )
            }
        }

        if (content.hotPois.isNotEmpty()) {
            item { SectionTitle("热门景点") }
            item { HotPoiRow(content.hotPois) }
        }

        if (content.recommendedRoutes.isNotEmpty()) {
            item { SectionTitle("推荐路线") }
            items(content.recommendedRoutes) { route -> RouteCard(route) }
        }

        if (content.banners.isEmpty() && content.quickEntries.isEmpty() &&
            content.hotPois.isEmpty() && content.recommendedRoutes.isEmpty()
        ) {
            item {
                Text(
                    text = "后端返回的首页内容为空",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun BannerPager(banners: List<Banner>) {
    if (banners.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("暂无轮播内容", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { banners.size })

    Column {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) { page ->
            val banner = banners[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bannerBrush(banner.color, bannerBrushes[page % bannerBrushes.size])),
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                ) {
                    Text(
                        text = banner.title.ifBlank { "景区推荐" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            banners.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun QuickEntryRow(entries: List<QuickEntry>, onEntryClick: (QuickEntry) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        entries.take(4).forEachIndexed { index, entry ->
            val tint = hexColor(entry.color, bannerBrushes[index % bannerBrushes.size].first())
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEntryClick(entry) },
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = entry.label.take(1).ifBlank { "·" },
                        color = tint,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun HotPoiRow(pois: List<PoiBrief>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(pois) { poi ->
            Card(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .size(width = 170.dp, height = 110.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = poi.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (poi.category.isNotBlank()) {
                        Text(
                            text = poi.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = poi.description.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteCard(route: RouteBrief) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = route.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            route.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            val meta = buildList {
                if (route.theme.isNotBlank()) add(route.theme)
                if (route.estimatedMinutes > 0) add("约 ${route.estimatedMinutes} 分钟")
            }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
