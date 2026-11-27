package com.example.scenicguide.ui.poi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scenicguide.data.model.PoiCategory
import com.example.scenicguide.data.model.PoiListItem
import com.example.scenicguide.data.model.categoryDisplayName
import com.example.scenicguide.ui.components.EmptyView
import com.example.scenicguide.ui.components.ErrorView
import com.example.scenicguide.ui.components.GuideCard
import com.example.scenicguide.ui.components.GuideTopBar
import com.example.scenicguide.ui.components.LoadingView
import com.example.scenicguide.ui.components.PoiImage
import com.example.scenicguide.ui.rememberAppContainer

private const val ALL_CATEGORY_LABEL = "全部"

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GuideTopBar(
                title = "西湖景点",
                subtitle = "湖光山色，慢慢游",
                onBack = onBack,
            )
        },
    ) { padding ->
        when (val current = state) {
            is PoiListViewModel.UiState.Loading -> LoadingView(
                text = "正在加载景点…",
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
        CategoryFilterRow(
            selectedCategory = selectedCategory,
            onSelect = onSelectCategory,
        )

        if (pois.isEmpty()) {
            EmptyView(
                message = "这个类别下暂无景点\n换个筛选条件试试吧",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filters) { (key, label) ->
            val selected = selectedCategory == key
            FilterChip(
                selected = selected,
                onClick = { onSelect(key) },
                label = { Text(label) },
                shape = MaterialTheme.shapes.medium,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
    GuideCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row {
            PoiImage(
                imageUrl = poi.imageUrl,
                contentDescription = poi.name,
                modifier = Modifier
                    .size(96.dp)
                    .clip(MaterialTheme.shapes.medium),
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poi.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = poi.category.categoryDisplayName(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )

                poi.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                poi.location?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "位置  $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
