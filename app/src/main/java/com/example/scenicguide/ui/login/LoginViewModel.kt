package com.example.scenicguide.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scenicguide.core.network.ApiResult
import com.example.scenicguide.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    data class UiState(
        val username: String = "",
        val password: String = "",
        val loading: Boolean = false,
        val error: String? = null,
        val loggedIn: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun onUsernameChange(value: String) = _state.update { it.copy(username = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun login() {
        val current = _state.value
        Log.d("ApiTrace", "login() called username='${current.username}' pwLen=${current.password.length} loading=${current.loading}")
        if (current.loading) return
        if (current.username.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(error = "请输入账号和密码") }
            return
        }
        if (current.password.length < 6) {
            _state.update { it.copy(error = "密码至少 6 位") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.login(current.username, current.password)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, loggedIn = true) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error.message) }
            }
        }
    }

    companion object {
        fun factory(repository: AuthRepository) = viewModelFactory {
            initializer { LoginViewModel(repository) }
        }
    }
}
