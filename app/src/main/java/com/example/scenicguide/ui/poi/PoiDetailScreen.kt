package com.example.scenicguide.ui.poi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
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
        // 顶部 Hero 区：图片 + 深色渐变蒙层 + 景点名/类别叠在上面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(detailBrushFor(poi.category)),
        ) {
            // 底层：图片 或 首字母大字
            val url = poi.imageUrl?.takeIf { it.isNotBlank() }
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = poi.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = poi.name.take(1).ifBlank { "·" },
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // 深色渐变蒙层（底部向上），保证叠在上层文字可读
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.55f),
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY,
                        ),
                    ),
            )

            // 叠在上面的景点名 + 类别
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, end = 16.dp, bottom = 16.dp),
            ) {
                Card(
                    shape = RoundedCornerShape(999.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.25f),
                    ),
                ) {
                    Text(
                        text = poi.category.categoryDisplayName(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = poi.name,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Spacer(Modifier.height(8.dp))

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

            Spacer(Modifier.height(16.dp))

            // 操作按钮行：导航 + 复制位置
            ActionRow(poi = poi)

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

// ---------------------------------------------------------------------------
// 操作按钮：导航 + 复制位置
// ---------------------------------------------------------------------------

@Composable
private fun ActionRow(poi: PoiDetail) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 主按钮：导航（有坐标传坐标，没有则用位置文字搜索）
        Button(
            onClick = { openNavigation(context, poi) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text("🧭 导航")
        }

        // 次按钮：复制位置
        OutlinedButton(
            onClick = { copyLocation(context, poi) },
            modifier = Modifier.weight(1f),
        ) {
            Text("📋 复制位置")
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
