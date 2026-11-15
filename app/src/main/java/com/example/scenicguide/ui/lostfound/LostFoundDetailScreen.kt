package com.example.scenicguide.ui.lostfound

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scenicguide.data.model.LostFoundItem
import com.example.scenicguide.data.model.formatLostFoundTime
import com.example.scenicguide.ui.components.ErrorView
import com.example.scenicguide.ui.components.LoadingView
import com.example.scenicguide.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostFoundDetailScreen(
    itemId: Int,
    onBack: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: LostFoundDetailViewModel = viewModel(
        factory = LostFoundDetailViewModel.factory(container.lostFoundRepository, itemId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("失物详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        when (val current = state) {
            is LostFoundDetailViewModel.UiState.Loading -> LoadingView(
                text = "正在加载详情…",
                modifier = Modifier.padding(padding),
            )

            is LostFoundDetailViewModel.UiState.Error -> ErrorView(
                message = current.message,
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )

            is LostFoundDetailViewModel.UiState.Success -> DetailContent(
                state = current,
                onUpdateStatus = viewModel::updateStatus,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun DetailContent(
    state: LostFoundDetailViewModel.UiState.Success,
    onUpdateStatus: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.item

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val badgeColor = if (item.type == LostFoundItem.TYPE_FOUND) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.tertiary
            }
            Box(
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(item.typeLabel, style = MaterialTheme.typography.labelSmall, color = badgeColor)
            }
            Text(
                text = item.statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            InfoRow("分类", item.category)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow("地点", item.location)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow("联系方式", item.contact)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow("发布人", item.publisherName)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow("发布时间", formatLostFoundTime(item.createdAt))
        }

        item.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                text = "详细描述",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (state.actionError != null) {
            Text(
                text = "操作失败：${state.actionError}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (item.status != LostFoundItem.STATUS_RESOLVED) {
            StatusButton(
                text = "标记为已解决",
                enabled = !state.updating,
                onClick = { onUpdateStatus(LostFoundItem.STATUS_RESOLVED) },
            )
        }
        if (item.status != LostFoundItem.STATUS_EXPIRED) {
            StatusButton(
                text = "标记为已过期",
                enabled = !state.updating,
                onClick = { onUpdateStatus(LostFoundItem.STATUS_EXPIRED) },
            )
        }
        if (item.status != LostFoundItem.STATUS_OPEN) {
            StatusButton(
                text = "重新开启",
                enabled = !state.updating,
                onClick = { onUpdateStatus(LostFoundItem.STATUS_OPEN) },
            )
        }

        if (state.updating) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(
                    text = "正在更新状态…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(text)
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
