package com.example.scenicguide.ui.poi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.scenicguide.data.model.PoiCategory
import com.example.scenicguide.data.model.PoiDetail
import com.example.scenicguide.data.model.categoryDisplayName
import com.example.scenicguide.ui.components.ErrorView
import com.example.scenicguide.ui.components.LoadingView
import com.example.scenicguide.ui.rememberAppContainer

private val detailBrushes: Map<String, Brush> = mapOf(
    PoiCategory.attraction.name to Brush.verticalGradient(
        listOf(Color(0xFF0E7C66), Color(0xFF2FA888)),
    ),
    PoiCategory.facility.name to Brush.verticalGradient(
        listOf(Color(0xFF1F5F8B), Color(0xFF3D7FB5)),
    ),
    PoiCategory.landmark.name to Brush.verticalGradient(
        listOf(Color(0xFF8B5E1F), Color(0xFFB88635)),
    ),
    PoiCategory.food.name to Brush.verticalGradient(
        listOf(Color(0xFFB83280), Color(0xFFD65A9E)),
    ),
    PoiCategory.other.name to Brush.verticalGradient(
        listOf(Color(0xFF4A5568), Color(0xFF6B7B8F)),
    ),
)

private fun detailBrushFor(category: String?): Brush =
    category?.let { detailBrushes[it] }
        ?: Brush.verticalGradient(listOf(Color(0xFF0E7C66), Color(0xFF2FA888)))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailScreen(
    poiId: Int,
    onBack: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: PoiDetailViewModel = viewModel(
        factory = PoiDetailViewModel.factory(container.poiRepository, poiId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("景点详情", fontWeight = FontWeight.SemiBold) },
            )
        },
    ) { padding ->
        when (val current = state) {
            is PoiDetailViewModel.UiState.Loading -> LoadingView(
                text = "正在加载详情…",
                modifier = Modifier.padding(padding),
            )

            is PoiDetailViewModel.UiState.Error -> ErrorView(
                message = current.message,
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(padding),
            )

            is PoiDetailViewModel.UiState.Success -> PoiDetailContent(
                poi = current.poi,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun PoiDetailContent(
    poi: PoiDetail,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // 顶部 Hero 区：有 image_url 则全幅加载，否则 fallback 到类别渐变色
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(detailBrushFor(poi.category)),
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
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            // 景点名
            Text(
                text = poi.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            // 类别标签
            Card(
                shape = RoundedCornerShape(999.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = poi.category.categoryDisplayName(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(20.dp))

            // 描述
            poi.description?.takeIf { it.isNotBlank() }?.let {
                SectionTitle("景点介绍")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(20.dp))

            // 信息卡片
            InfoCard(poi = poi)

            Spacer(Modifier.height(24.dp))
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
private fun InfoCard(poi: PoiDetail) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("基本信息")
            Spacer(Modifier.height(12.dp))

            InfoRow(
                icon = "📍",
                label = "位置",
                value = poi.location?.takeIf { it.isNotBlank() } ?: "暂无位置信息",
            )

            if (poi.longitude != null && poi.latitude != null) {
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    icon = "🗺️",
                    label = "坐标",
                    value = "%.6f, %.6f".format(poi.longitude, poi.latitude),
                )
            }

            poi.createdAt?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    icon = "📅",
                    label = "录入时间",
                    value = it,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: String,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
