package com.example.scenicguide.ui.lostfound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scenicguide.core.network.ApiResult
import com.example.scenicguide.data.model.LostFoundItem
import com.example.scenicguide.data.repository.LostFoundRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LostFoundDetailViewModel(
    private val repository: LostFoundRepository,
    private val itemId: Int,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val item: LostFoundItem,
            val updating: Boolean = false,
            val actionError: String? = null,
        ) : UiState

        data class Error(val message: String) : UiState
        data object Deleted : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = repository.detail(itemId)) {
                is ApiResult.Success -> UiState.Success(result.data)
                is ApiResult.Failure -> UiState.Error(result.error.message)
            }
        }
    }

    fun updateStatus(status: String) {
        val current = _state.value as? UiState.Success ?: return
        if (current.updating) return
        viewModelScope.launch {
            _state.value = current.copy(updating = true, actionError = null)
            when (val result = repository.updateStatus(itemId, status)) {
                // PATCH /status 只回 { id, status }，本地 merge 即可，不用再拉一次详情
                is ApiResult.Success -> _state.value = UiState.Success(
                    item = current.item.copy(status = result.data.status.ifBlank { status }),
                    updating = false,
                )
                is ApiResult.Failure -> _state.value = current.copy(updating = false, actionError = result.error.message)
            }
        }
    }

    fun delete(onDeleted: () -> Unit = {}) {
        val current = _state.value as? UiState.Success ?: return
        if (current.updating) return
        viewModelScope.launch {
            _state.value = current.copy(updating = true, actionError = null)
            when (val result = repository.delete(itemId)) {
                is ApiResult.Success -> {
                    _state.value = UiState.Deleted
                    onDeleted()
                }
                is ApiResult.Failure -> _state.value = current.copy(updating = false, actionError = result.error.message)
            }
        }
    }

    companion object {
        fun factory(repository: LostFoundRepository, itemId: Int) = viewModelFactory {
            initializer { LostFoundDetailViewModel(repository, itemId) }
        }
    }
}
