package com.example.scenicguide.ui.poi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scenicguide.core.network.ApiResult
import com.example.scenicguide.data.model.PoiCategory
import com.example.scenicguide.data.model.PoiListItem
import com.example.scenicguide.data.repository.PoiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PoiListViewModel(
    private val poiRepository: PoiRepository,
) : ViewModel() {

    /** 可选的筛选类别，null 表示「全部」 */
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    /** 所有可选类别（含「全部」） */
    val categories: List<String?> = listOf(null) + PoiCategory.entries.map { it.name }

    sealed interface UiState {
        data object Loading : UiState
        data class Success(val pois: List<PoiListItem>) : UiState
        data class Error(val message: String) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun selectCategory(category: String?) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = poiRepository.listPois(category = _selectedCategory.value)) {
                is ApiResult.Success -> UiState.Success(result.data)
                is ApiResult.Failure -> UiState.Error(result.error.message)
            }
        }
    }

    companion object {
        fun factory(poiRepository: PoiRepository) = viewModelFactory {
            initializer { PoiListViewModel(poiRepository) }
        }
    }
}
