package com.example.scenicguide.ui.poi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scenicguide.core.network.ApiResult
import com.example.scenicguide.data.model.PoiDetail
import com.example.scenicguide.data.repository.PoiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PoiDetailViewModel(
    private val poiRepository: PoiRepository,
    private val poiId: Int,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(val poi: PoiDetail) : UiState
        data class Error(val message: String) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = poiRepository.getPoiDetail(poiId)) {
                is ApiResult.Success -> UiState.Success(result.data)
                is ApiResult.Failure -> UiState.Error(result.error.message)
            }
        }
    }

    companion object {
        fun factory(poiRepository: PoiRepository, poiId: Int) = viewModelFactory {
            initializer { PoiDetailViewModel(poiRepository, poiId) }
        }
    }
}
