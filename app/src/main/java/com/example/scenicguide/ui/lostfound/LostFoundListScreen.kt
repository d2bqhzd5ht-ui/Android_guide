package com.example.scenicguide.ui.lostfound

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scenicguide.data.model.LostFoundItem
import com.example.scenicguide.data.model.formatLostFoundTime
import com.example.scenicguide.ui.components.EmptyView
import com.example.scenicguide.ui.components.ErrorView
import com.example.scenicguide.ui.components.GuideCard
import com.example.scenicguide.ui.components.GuideTopBar
import com.example.scenicguide.ui.components.LoadingView
import com.example.scenicguide.ui.rememberAppContainer

@Composable
fun LostFoundListScreen(
    onBack: () -> Unit,
    onOpenDetail: (Int) -> Unit,
    onPublish: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: LostFoundListViewModel = viewModel(
        factory = LostFoundListViewModel.factory(container.lostFoundRepository),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    // 从详情页 / 发布页返回时自动刷新，保证状态和新增数据最新
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GuideTopBar(
                title = "失物招领",
                subtitle = "湖山有爱，让失物与主人重逢",
                onBack = onBack,
                actions = {
                    TextButton(
                        onClick = onPublish,
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("发布")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TypeFilterRow(selected = query.type, onSelect = viewModel::setType)
                StatusFilterRow(selected = query.status, onSelect = viewModel::setStatus)
            }

            when (val current = state) {
                is LostFoundListViewModel.UiState.Loading -> LoadingView(
                    text = "正在加载失物招领…",
                    modifier = Modifier.weight(1f),
                )

                is LostFoundListViewModel.UiState.Error -> ErrorView(
                    message = current.message,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.weight(1f),
                )

                is LostFoundListViewModel.UiState.Success -> if (current.items.isEmpty()) {
                    EmptyLostFound(
                        onPublish = onPublish,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    ItemList(
                        state = current,
                        onOpenDetail = onOpenDetail,
                        onLoadMore = viewModel::loadMore,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeFilterRow(selected: String?, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            null to "全部",
            LostFoundItem.TYPE_LOST to "寻物启事",
            LostFoundItem.TYPE_FOUND to "招领启事",
        ).forEach { (value, label) ->
            LostFoundFilterChip(
                label = label,
                selected = selected == value,
                onClick = { onSelect(value) },
            )
        }
    }
}

@Composable
private fun StatusFilterRow(selected: String?, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            null to "全部状态",
            LostFoundItem.STATUS_OPEN to "进行中",
            LostFoundItem.STATUS_RESOLVED to "已解决",
            LostFoundItem.STATUS_EXPIRED to "已过期",
        ).forEach { (value, label) ->
            LostFoundFilterChip(
                label = label,
                selected = selected == value,
                onClick = { onSelect(value) },
            )
        }
    }
}

@Composable
private fun LostFoundFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.height(48.dp),
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
private fun ItemList(
    state: LostFoundListViewModel.UiState.Success,
    onOpenDetail: (Int) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(state.items, key = { it.id }) { item ->
            LostFoundCard(item = item, onClick = { onOpenDetail(item.id) })
        }

        if (state.hasMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.loadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        TextButton(
                            onClick = onLoadMore,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            Text("加载更多")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LostFoundCard(item: LostFoundItem, onClick: () -> Unit) {
    GuideCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.type == LostFoundItem.TYPE_FOUND) {
                InfoBadge(
                    label = item.typeLabel,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                InfoBadge(
                    label = item.typeLabel,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            StatusBadge(status = item.status, label = item.statusLabel)
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp),
        )

        val meta = listOfNotNull(
            item.category.takeIf { it.isNotBlank() && it != LostFoundItem.CATEGORY_OTHER },
            item.location?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Text(
            text = formatLostFoundTime(item.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun StatusBadge(status: String, label: String) {
    when (status) {
        LostFoundItem.STATUS_RESOLVED -> InfoBadge(
            label = label,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )

        LostFoundItem.STATUS_EXPIRED -> InfoBadge(
            label = label,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )

        else -> InfoBadge(
            label = label,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun InfoBadge(
    label: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EmptyLostFound(onPublish: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        EmptyView(
            message = "当前筛选条件下没有失物招领信息",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        Button(
            onClick = onPublish,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("发布失物招领")
        }
    }
}
