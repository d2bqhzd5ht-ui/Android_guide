package com.example.scenicguide.ui.lostfound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scenicguide.core.network.ApiResult
import com.example.scenicguide.data.model.LostFoundDraft
import com.example.scenicguide.data.repository.LostFoundRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LostFoundPublishViewModel(
    private val repository: LostFoundRepository,
) : ViewModel() {

    sealed interface SubmitState {
        data object Idle : SubmitState
        data object Submitting : SubmitState
        data object Success : SubmitState
        data class Error(val message: String) : SubmitState
    }

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState = _submitState.asStateFlow()

    fun submit(draft: LostFoundDraft) {
        if (_submitState.value is SubmitState.Submitting) return
        viewModelScope.launch {
            _submitState.value = SubmitState.Submitting
            _submitState.value = when (val result = repository.create(draft)) {
                is ApiResult.Success -> SubmitState.Success
                is ApiResult.Failure -> SubmitState.Error(result.error.message)
            }
        }
    }

    fun clearError() {
        if (_submitState.value is SubmitState.Error) _submitState.value = SubmitState.Idle
    }

    companion object {
        fun factory(repository: LostFoundRepository) = viewModelFactory {
            initializer { LostFoundPublishViewModel(repository) }
        }
    }
}
