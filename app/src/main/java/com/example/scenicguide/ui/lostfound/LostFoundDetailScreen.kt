package com.example.scenicguide.ui.lostfound

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scenicguide.data.model.LostFoundItem
import com.example.scenicguide.data.model.formatLostFoundTime
import com.example.scenicguide.ui.components.ErrorView
import com.example.scenicguide.ui.components.GuideCard
import com.example.scenicguide.ui.components.GuideTopBar
import com.example.scenicguide.ui.components.LoadingView
import com.example.scenicguide.ui.rememberAppContainer

@Composable
fun LostFoundDetailScreen(
    itemId: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onDeleted: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: LostFoundDetailViewModel = viewModel(
        factory = LostFoundDetailViewModel.factory(container.lostFoundRepository, itemId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GuideTopBar(
                title = "失物详情",
                subtitle = "核对线索，帮助物品早日回家",
                onBack = onBack,
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

            is LostFoundDetailViewModel.UiState.Deleted -> { /* 被删除后外部导航回去 */ }

            is LostFoundDetailViewModel.UiState.Success -> DetailContent(
                state = current,
                onUpdateStatus = viewModel::updateStatus,
                onEdit = { onEdit(itemId) },
                onDelete = { viewModel.delete(onDeleted) },
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.item
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GuideCard(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.type == LostFoundItem.TYPE_FOUND) {
                    DetailBadge(
                        label = item.typeLabel,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    DetailBadge(
                        label = item.typeLabel,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                DetailStatusBadge(status = item.status, label = item.statusLabel)
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "发布于 ${formatLostFoundTime(item.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        GuideCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "物品与联系信息",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow("分类", item.category.takeIf { it != LostFoundItem.CATEGORY_OTHER })
            DetailDivider()
            InfoRow("地点", item.location)
            DetailDivider()
            InfoRow("联系方式", item.contact)
            DetailDivider()
            InfoRow("发布人", item.publisherName)
            DetailDivider()
            InfoRow("发布时间", formatLostFoundTime(item.createdAt))
        }

        item.description?.takeIf { it.isNotBlank() }?.let { description ->
            GuideCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "详细描述",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        if (state.actionError != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = "操作失败：${state.actionError}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "管理这条信息",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            Button(
                onClick = onEdit,
                enabled = !state.updating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("编辑")
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

            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                enabled = !state.updating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }

            if (state.updating) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = "正在处理…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("删除后无法恢复，确定要删除这条失物招领信息吗？") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    modifier = Modifier.height(48.dp),
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    modifier = Modifier.height(48.dp),
                ) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun DetailStatusBadge(status: String, label: String) {
    when (status) {
        LostFoundItem.STATUS_RESOLVED -> DetailBadge(
            label = label,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )

        LostFoundItem.STATUS_EXPIRED -> DetailBadge(
            label = label,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )

        else -> DetailBadge(
            label = label,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun DetailBadge(
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
private fun DetailDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun StatusButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text)
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
