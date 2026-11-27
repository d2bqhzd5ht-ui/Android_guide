package com.example.scenicguide.ui.poi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scenicguide.data.model.PoiDetail
import com.example.scenicguide.data.model.categoryDisplayName
import com.example.scenicguide.ui.components.ErrorView
import com.example.scenicguide.ui.components.GuideCard
import com.example.scenicguide.ui.components.GuideTopBar
import com.example.scenicguide.ui.components.LoadingView
import com.example.scenicguide.ui.components.PoiImage
import com.example.scenicguide.ui.components.SectionTitle
import com.example.scenicguide.ui.rememberAppContainer

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GuideTopBar(
                title = "景点详情",
                subtitle = "西湖文旅指南",
                onBack = onBack,
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PoiImage(
            imageUrl = poi.imageUrl,
            contentDescription = poi.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(224.dp)
                .clip(MaterialTheme.shapes.medium),
        )

        Column {
            Text(
                text = poi.category.categoryDisplayName(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = poi.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        poi.description?.takeIf { it.isNotBlank() }?.let {
            GuideCard(modifier = Modifier.fillMaxWidth()) {
                SectionTitle("景点介绍")
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }

        InfoCard(poi = poi)
        ActionRow(poi = poi)
    }
}

@Composable
private fun InfoCard(poi: PoiDetail) {
    GuideCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("基本信息")
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InfoRow(
                label = "位置",
                value = poi.location?.takeIf { it.isNotBlank() } ?: "暂无位置信息",
            )

            if (poi.longitude != null && poi.latitude != null) {
                InfoRow(
                    label = "坐标",
                    value = "%.6f, %.6f".format(poi.longitude, poi.latitude),
                )
            }

            poi.createdAt?.takeIf { it.isNotBlank() }?.let {
                InfoRow(
                    label = "录入时间",
                    value = it,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ActionRow(poi: PoiDetail) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = { openNavigation(context, poi) },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("导航")
        }

        OutlinedButton(
            onClick = { copyLocation(context, poi) },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text("复制位置")
        }
    }
}

/**
 * 打开地图应用导航：
 *  - 有经纬度 → geo:lat,lon?q=名称（Google Maps / 高德都支持）
 *  - 没坐标但有位置文字 → geo:0,0?q=位置文字（跳搜索框）
 *  - 都没有 → Toast 提示
 */
private fun openNavigation(context: Context, poi: PoiDetail) {
    val query = poi.location?.takeIf { it.isNotBlank() } ?: poi.name
    val geoUri = if (poi.longitude != null && poi.latitude != null) {
        "geo:${poi.latitude},${poi.longitude}?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
    } else {
        "geo:0,0?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
    }
    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(geoUri)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "未检测到地图应用，请先安装高德/百度/Google Maps", Toast.LENGTH_LONG).show()
    }
}

/** 复制位置文字到剪贴板 */
private fun copyLocation(context: Context, poi: PoiDetail) {
    val text = poi.location?.takeIf { it.isNotBlank() }
        ?: "（位置信息暂缺）"
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    if (clipboard != null) {
        clipboard.setPrimaryClip(ClipData.newPlainText("景点位置", text))
        Toast.makeText(context, "已复制位置：$text", Toast.LENGTH_SHORT).show()
    }
}
