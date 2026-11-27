package com.example.scenicguide.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scenicguide.data.model.Banner
import com.example.scenicguide.data.model.HomeContent
import com.example.scenicguide.data.model.PoiBrief
import com.example.scenicguide.data.model.QuickEntry
import com.example.scenicguide.data.model.RouteBrief
import com.example.scenicguide.ui.components.ErrorView
import com.example.scenicguide.ui.components.GuideCard
import com.example.scenicguide.ui.components.GuideTopBar
import com.example.scenicguide.ui.components.LoadingView
import com.example.scenicguide.ui.components.PoiImage
import com.example.scenicguide.ui.components.SectionTitle
import com.example.scenicguide.ui.rememberAppContainer

/** 后端 quickEntries[].path 的目标路由（与后台配置保持一致） */
private const val LOST_FOUND_PATH = "/lost-found"
private const val CHAT_PATH = "/interaction"
private const val POI_PATH = "/poi"
private const val POI_LIST_PATH = "/poi_list"
private const val POIS_PATH = "/pois"
private const val ROUTES_PATH = "/routes"

@Composable
fun HomeScreen(
    onOpenChat: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenLostFound: () -> Unit,
    onOpenPoiList: () -> Unit,
    onOpenPoiDetail: (Int) -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(container.homeRepository, container.authRepository),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GuideTopBar(
                title = "西湖导览",
                subtitle = "WEST LAKE",
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
                onOpenPoiDetail = onOpenPoiDetail,
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
    onOpenPoiList: () -> Unit,
    onOpenPoiDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val onRoutesUnavailable = {
        Toast.makeText(context, "游玩攻略暂未开放", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "发现身边的风景",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "从容游览，随时了解西湖故事",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        item { BannerPager(content.banners) }

        item {
            GuideCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenChat,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.shapes.medium,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(text = "AI 智能导览", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "边逛边问，了解景点与游览建议",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Text(
                        text = "去提问",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
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
                            POI_PATH, POI_LIST_PATH, POIS_PATH -> onOpenPoiList()
                            ROUTES_PATH -> onRoutesUnavailable()
                            else -> Toast.makeText(
                                context,
                                "该入口暂未开放",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                )
            }
        }

        if (content.hotPois.isNotEmpty()) {
            item { SectionTitle("热门景点") }
            item { HotPoiRow(content.hotPois, onOpenPoiDetail) }
        }

        if (content.recommendedRoutes.isNotEmpty()) {
            item { SectionTitle("推荐路线") }
            items(content.recommendedRoutes) { route ->
                RouteCard(route = route, onClick = onRoutesUnavailable)
            }
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
private fun BannerPager(banners: List<Banner>) {
    if (banners.isEmpty()) {
        GuideCard(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "暂无轮播内容",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                .height(196.dp),
        ) { page ->
            val banner = banners[page]
            GuideCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
            ) {
                PoiImage(
                    imageUrl = banner.image,
                    contentDescription = banner.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                )
                Text(
                    text = banner.title.ifBlank { "西湖景区推荐" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
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
                                MaterialTheme.colorScheme.secondaryContainer
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
        entries.take(4).forEach { entry ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEntryClick(entry) }
                    .padding(vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.shapes.medium,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = quickEntryIcon(entry.path),
                        contentDescription = entry.label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

private fun quickEntryIcon(path: String): ImageVector = when (path) {
    LOST_FOUND_PATH -> Icons.Outlined.Search
    CHAT_PATH -> Icons.Outlined.Send
    POI_PATH, POI_LIST_PATH, POIS_PATH -> Icons.Outlined.Place
    ROUTES_PATH -> Icons.Outlined.List
    else -> Icons.Outlined.Info
}

@Composable
private fun HotPoiRow(pois: List<PoiBrief>, onOpenPoiDetail: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(pois) { poi ->
            GuideCard(
                modifier = Modifier
                    .width(210.dp)
                    .height(132.dp),
                onClick = { onOpenPoiDetail(poi.id) },
                contentPadding = PaddingValues(14.dp),
            ) {
                Text(
                    text = poi.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (poi.category.isNotBlank()) {
                    Text(
                        text = poi.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = poi.description.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun RouteCard(route: RouteBrief, onClick: () -> Unit) {
    GuideCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(text = route.name, style = MaterialTheme.typography.titleSmall)
        route.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
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
