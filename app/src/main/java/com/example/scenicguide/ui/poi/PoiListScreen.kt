package com.example.scenicguide.ui.poi

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.scenicguide.data.model.PoiCategory
import com.example.scenicguide.data.model.PoiListItem
import com.example.scenicguide.data.model.categoryDisplayName
import com.example.scenicguide.ui.components.ErrorView
import com.example.scenicguide.ui.rememberAppContainer

/** 每个类别配一个主题渐变色 */
private val categoryBrushes: Map<String, Brush> = mapOf(
    PoiCategory.attraction.name to Brush.linearGradient(
        listOf(Color(0xFF0E7C66), Color(0xFF4BB39A)),
    ),
    PoiCategory.facility.name to Brush.linearGradient(
        listOf(Color(0xFF1F5F8B), Color(0xFF5AA9D6)),
    ),
    PoiCategory.landmark.name to Brush.linearGradient(
        listOf(Color(0xFF8B5E1F), Color(0xFFD6A85A)),
    ),
    PoiCategory.food.name to Brush.linearGradient(
        listOf(Color(0xFFB83280), Color(0xFFE879A8)),
    ),
    PoiCategory.other.name to Brush.linearGradient(
        listOf(Color(0xFF4A5568), Color(0xFF8A9BB5)),
    ),
)

private fun brushFor(category: String?): Brush =
    category?.let { categoryBrushes[it] }
        ?: Brush.linearGradient(listOf(Color(0xFF0E7C66), Color(0xFF4BB39A)))

private const val ALL_CATEGORY_LABEL = "全部"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiListScreen(
    onBack: () -> Unit,
    onOpenDetail: (poiId: Int) -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: PoiListViewModel = viewModel(
        factory = PoiListViewModel.factory(container.poiRepository),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("景点列表", fontWeight = FontWeight.SemiBold) },
            )
        },
    ) { padding ->
        when (val current = state) {
            is PoiListViewModel.UiState.Loading -> PoiListLoading(
                modifier = Modifier.padding(padding),
            )

            is PoiListViewModel.UiState.Error -> ErrorView(
                message = current.message,
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(padding),
            )

            is PoiListViewModel.UiState.Success -> PoiListContent(
                pois = current.pois,
                selectedCategory = selectedCategory,
                onSelectCategory = viewModel::selectCategory,
                onOpenDetail = onOpenDetail,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun PoiListContent(
    pois: List<PoiListItem>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    onOpenDetail: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 类别筛选条
        CategoryFilterRow(
            selectedCategory = selectedCategory,
            onSelect = onSelectCategory,
        )

        if (pois.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "这个类别下暂无景点",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "换个筛选条件试试吧",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(pois, key = { it.id }) { poi ->
                    PoiCard(
                        poi = poi,
                        onClick = { onOpenDetail(poi.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: String?,
    onSelect: (String?) -> Unit,
) {
    val filters: List<Pair<String?, String>> = buildList {
        add(null to ALL_CATEGORY_LABEL)
        PoiCategory.entries.forEach { add(it.name to it.displayName) }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filters) { (key, label) ->
            val selected = selectedCategory == key
            FilterChip(
                selected = selected,
                onClick = { onSelect(key) },
                label = { Text(label) },
                shape = RoundedCornerShape(999.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun PoiCard(
    poi: PoiListItem,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // 左侧缩略图：有 image_url 则加载，否则 fallback 到类别渐变色块
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brushFor(poi.category)),
                contentAlignment = Alignment.Center,
            ) {
                val url = poi.imageUrl?.takeIf { it.isNotBlank() }
                if (url != null) {
                    AsyncImage(
                        model = url,
                        contentDescription = poi.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = poi.name.take(1).ifBlank { "·" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poi.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // 类别标签
                Text(
                    text = poi.category.categoryDisplayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )

                poi.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                poi.location?.takeIf { it.isNotBlank() }?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        Text(
                            text = "📍",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 骨架屏：加载时显示，体验比 LoadingView 更好
// ---------------------------------------------------------------------------

private const val SHIMMER_COLOR = 0xFFE0E0E0
private const val SHIMMER_HIGHLIGHT = 0xFFF5F5F5

/** 无限循环的 shimmer 渐变，从左到右扫过 */
@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-offset",
    )
    return Brush.linearGradient(
        colors = listOf(
            Color(SHIMMER_COLOR),
            Color(SHIMMER_HIGHLIGHT),
            Color(SHIMMER_COLOR),
        ),
        start = androidx.compose.ui.geometry.Offset(offset, 0f),
        end = androidx.compose.ui.geometry.Offset(offset + 1f, 0f),
    )
}

@Composable
private fun PoiListLoading(
    modifier: Modifier = Modifier,
) {
    val shimmer = shimmerBrush()
    Column(modifier = modifier.fillMaxSize()) {
        // 类别筛选占位（5 个灰色圆角块）
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(5) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(shimmer),
                )
            }
        }

        // 卡片骨架
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(5) {
                SkeletonCard(shimmer)
            }
        }
    }
}

@Composable
private fun SkeletonCard(shimmer: Brush) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // 左侧缩略图占位
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmer),
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 标题
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmer),
                )
                // 类别标签
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                        .padding(top = 6.dp),
                )
                // 描述两行
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                        .padding(top = 10.dp),
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer),
                )
            }
        }
    }
}
