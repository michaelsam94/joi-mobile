package com.joi.app.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.AppResult
import com.joi.domain.model.Role
import com.joi.domain.repository.RegisterUserInput
import com.joi.domain.usecase.RegisterMemberUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterMemberUiState(
    val fullName: String = "",
    val username: String = "",
    val temporaryPassword: String = "",
    val asModerator: Boolean = false,
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val done: Boolean = false,
)

class RegisterMemberViewModel(private val registerMemberUseCase: RegisterMemberUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterMemberUiState())
    val uiState: StateFlow<RegisterMemberUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) {
        _uiState.value = _uiState.value.copy(fullName = value)
    }
    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }
    fun onTemporaryPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(temporaryPassword = value)
    }
    fun onAsModeratorChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(asModerator = value)
    }

    fun register() {
        val state = _uiState.value
        if (state.loading) return
        _uiState.value = state.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            val input = RegisterUserInput(
                fullName = state.fullName,
                username = state.username,
                temporaryPassword = state.temporaryPassword,
                role = if (state.asModerator) Role.MODERATOR else Role.MEMBER,
            )
            when (val result = registerMemberUseCase(input)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(loading = false, done = true)
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(loading = false, errorMessage = result.error.message)
            }
        }
    }
}
