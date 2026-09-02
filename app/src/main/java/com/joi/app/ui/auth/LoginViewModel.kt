package com.joi.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.AppResult
import com.joi.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val errorMessage: String? = null,
)

class LoginViewModel(private val loginUseCase: LoginUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun login() {
        val state = _uiState.value
        if (state.loading) return
        _uiState.value = state.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = loginUseCase(state.username, state.password)) {
                is AppResult.Success -> {
                    // Session is already saved by the use-case; JoiNavHost reacts to the session
                    // flow and switches screens on its own — nothing else to do here.
                    _uiState.value = _uiState.value.copy(loading = false)
                }
                is AppResult.Failure -> {
                    _uiState.value = _uiState.value.copy(loading = false, errorMessage = result.error.message)
                }
            }
        }
    }
}
