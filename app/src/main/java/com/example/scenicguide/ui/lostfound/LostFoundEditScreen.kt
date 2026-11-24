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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scenicguide.data.model.LostFoundDraft
import com.example.scenicguide.data.model.LostFoundItem
import com.example.scenicguide.ui.components.ErrorView
import com.example.scenicguide.ui.components.LoadingView
import com.example.scenicguide.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostFoundEditScreen(
    itemId: Int,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: LostFoundEditViewModel = viewModel(
        factory = LostFoundEditViewModel.factory(container.lostFoundRepository, itemId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var type by rememberSaveable { mutableStateOf(LostFoundItem.TYPE_LOST) }
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var publisherName by rememberSaveable { mutableStateOf("") }
    var titleMissing by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        when (state) {
            is LostFoundEditViewModel.UiState.Ready -> {
                val item = (state as LostFoundEditViewModel.UiState.Ready).item
                type = item.type
                title = item.title
                category = item.category.takeIf { it != LostFoundItem.CATEGORY_OTHER }.orEmpty()
                description = item.description.orEmpty()
                location = item.location.orEmpty()
                contact = item.contact.orEmpty()
                publisherName = item.publisherName.orEmpty()
            }
            LostFoundEditViewModel.UiState.Saved -> onSaved()
            else -> Unit
        }
    }

    val loading = state is LostFoundEditViewModel.UiState.Loading || state is LostFoundEditViewModel.UiState.Submitting
    val submitError = (state as? LostFoundEditViewModel.UiState.Error)?.message

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑失物招领", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        when (state) {
            LostFoundEditViewModel.UiState.Loading -> LoadingView(
                text = "正在加载…",
                modifier = Modifier.padding(padding),
            )

            is LostFoundEditViewModel.UiState.Error -> ErrorView(
                message = (state as LostFoundEditViewModel.UiState.Error).message,
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )

            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == LostFoundItem.TYPE_LOST,
                        onClick = { type = LostFoundItem.TYPE_LOST },
                        label = { Text("寻物启事") },
                    )
                    FilterChip(
                        selected = type == LostFoundItem.TYPE_FOUND,
                        onClick = { type = LostFoundItem.TYPE_FOUND },
                        label = { Text("招领启事") },
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (titleMissing && it.isNotBlank()) titleMissing = false
                    },
                    label = { Text("标题（必填）") },
                    singleLine = true,
                    isError = titleMissing,
                    supportingText = if (titleMissing) {
                        { Text("请填写标题") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("物品分类") },
                    placeholder = { Text("如：雨伞 / 钥匙 / 手机") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("地点") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("详细描述") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("联系方式") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = publisherName,
                    onValueChange = { publisherName = it },
                    label = { Text("发布人") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (submitError != null) {
                    Text(
                        text = "保存失败：$submitError",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleMissing = true
                            return@Button
                        }
                        viewModel.update(
                            LostFoundDraft(
                                type = type,
                                title = title.trim(),
                                category = category.trim().ifBlank { null },
                                description = description.trim().ifBlank { null },
                                location = location.trim().ifBlank { null },
                                contact = contact.trim().ifBlank { null },
                                publisherName = publisherName.trim().ifBlank { null },
                                userId = null, // 编辑不改 user_id
                            ),
                        )
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Text("保存")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
