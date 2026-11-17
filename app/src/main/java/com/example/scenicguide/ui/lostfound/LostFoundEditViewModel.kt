package com.example.scenicguide.ui.lostfound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scenicguide.core.network.ApiResult
import com.example.scenicguide.data.model.LostFoundDraft
import com.example.scenicguide.data.model.LostFoundItem
import com.example.scenicguide.data.repository.LostFoundRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LostFoundEditViewModel(
    private val repository: LostFoundRepository,
    private val itemId: Int,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Ready(val item: LostFoundItem) : UiState
        data object Saved : UiState
        data class Submitting(val item: LostFoundItem) : UiState
        data class Error(val message: String) : UiState
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
                is ApiResult.Success -> UiState.Ready(result.data)
                is ApiResult.Failure -> UiState.Error(result.error.message)
            }
        }
    }

    fun update(draft: LostFoundDraft) {
        val current = (_state.value as? UiState.Ready) ?: return
        viewModelScope.launch {
            _state.value = UiState.Submitting(current.item)
            _state.value = when (val result = repository.update(itemId, draft)) {
                is ApiResult.Success -> UiState.Saved
                is ApiResult.Failure -> UiState.Error(result.error.message)
            }
        }
    }

    companion object {
        fun factory(repository: LostFoundRepository, itemId: Int) = viewModelFactory {
            initializer { LostFoundEditViewModel(repository, itemId) }
        }
    }
}
