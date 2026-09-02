package com.joi.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.AppResult
import com.joi.domain.usecase.ChangePasswordUseCase
import com.joi.domain.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChangePasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val done: Boolean = false,
)

class ChangePasswordViewModel(
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onNewPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(newPassword = value, errorMessage = null)
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, errorMessage = null)
    }

    fun submit() {
        val state = _uiState.value
        if (state.loading) return
        _uiState.value = state.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = changePasswordUseCase(state.newPassword, state.confirmPassword)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(loading = false, done = true)
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(loading = false, errorMessage = result.error.message)
            }
        }
    }

    /** Escape hatch if someone got a temp password wrong and just wants to try a different account. */
    fun signOut() {
        viewModelScope.launch { logoutUseCase() }
    }
}
