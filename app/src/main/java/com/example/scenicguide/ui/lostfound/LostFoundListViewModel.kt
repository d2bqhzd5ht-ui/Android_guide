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

class LostFoundListViewModel(
    private val repository: LostFoundRepository,
) : ViewModel() {

    /** 列表筛选条件，null 表示「全部」 */
    data class Query(val type: String? = null, val status: String? = null)

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val items: List<LostFoundItem>,
            val hasMore: Boolean,
            val loadingMore: Boolean = false,
        ) : UiState

        data class Error(val message: String) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()

    private val _query = MutableStateFlow(Query())
    val query = _query.asStateFlow()

    private var page = 1

    fun refresh() = load(reset = true)

    fun loadMore() {
        val current = _state.value
        if (current !is UiState.Success || !current.hasMore || current.loadingMore) return
        load(reset = false)
    }

    fun setType(type: String?) {
        if (_query.value.type == type) return
        _query.value = _query.value.copy(type = type)
        refresh()
    }

    fun setStatus(status: String?) {
        if (_query.value.status == status) return
        _query.value = _query.value.copy(status = status)
        refresh()
    }

    private fun load(reset: Boolean) {
        viewModelScope.launch {
            if (reset) {
                page = 1
                _state.value = UiState.Loading
            } else {
                _state.value = (_state.value as? UiState.Success)?.copy(loadingMore = true) ?: UiState.Loading
            }

            val query = _query.value
            when (val result = repository.list(type = query.type, status = query.status, page = page, size = PAGE_SIZE)) {
                is ApiResult.Success -> {
                    val pageData = result.data
                    val fresh = pageData.list
                    val previous = if (reset) emptyList() else (_state.value as? UiState.Success)?.items.orEmpty()
                    _state.value = UiState.Success(
                        items = (previous + fresh).distinctBy { it.id },
                        hasMore = pageData.pagination.page < pageData.pagination.totalPages,
                    )
                    page += 1
                }

                is ApiResult.Failure -> {
                    val previous = (_state.value as? UiState.Success)
                    _state.value = if (reset || previous == null) {
                        UiState.Error(result.error.message)
                    } else {
                        previous.copy(loadingMore = false)
                    }
                }
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 20

        fun factory(repository: LostFoundRepository) = viewModelFactory {
            initializer { LostFoundListViewModel(repository) }
        }
    }
}
