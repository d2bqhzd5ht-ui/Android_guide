package com.example.scenicguide.ui.login

import android.util.Log
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

class RegisterViewModel(private val repository: AuthRepository) : ViewModel() {

    data class UiState(
        val username: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val email: String = "",
        val loading: Boolean = false,
        val error: String? = null,
        val registered: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun onUsernameChange(v: String) = _state.update { it.copy(username = v, error = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onConfirmPasswordChange(v: String) = _state.update { it.copy(confirmPassword = v, error = null) }
    fun onEmailChange(v: String) = _state.update { it.copy(email = v, error = null) }

    fun register() {
        val s = _state.value
        Log.d("ApiTrace", "register() username='${s.username}' email='${s.email}'")
        if (s.loading) return
        if (s.username.isBlank()) {
            _state.update { it.copy(error = "请输入账号") }; return
        }
        if (s.username.length < 3) {
            _state.update { it.copy(error = "账号至少 3 位") }; return
        }
        if (s.password.isBlank()) {
            _state.update { it.copy(error = "请输入密码") }; return
        }
        if (s.password.length < 6) {
            _state.update { it.copy(error = "密码至少 6 位") }; return
        }
        if (s.password != s.confirmPassword) {
            _state.update { it.copy(error = "两次密码不一致") }; return
        }
        if (s.email.isNotBlank() && !EMAIL_REGEX.matches(s.email.trim())) {
            _state.update { it.copy(error = "邮箱格式不正确") }; return
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.register(s.username, s.password, s.email.ifBlank { null })) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, registered = true) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error.message) }
            }
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$")

        fun factory(repository: AuthRepository) = viewModelFactory {
            initializer { RegisterViewModel(repository) }
        }
    }
}
