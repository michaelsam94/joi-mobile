package com.joi.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.AppResult
import com.joi.domain.model.PointTransaction
import com.joi.domain.model.PublicUser
import com.joi.domain.usecase.GetMemberPointsHistoryUseCase
import com.joi.domain.usecase.GetMemberQrCodeUseCase
import com.joi.domain.usecase.GetMyProfileUseCase
import com.joi.domain.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: PublicUser? = null,
    val qrPng: ByteArray? = null,
    val history: List<PointTransaction> = emptyList(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
)

class ProfileViewModel(
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val getMemberQrCodeUseCase: GetMemberQrCodeUseCase,
    private val getMemberPointsHistoryUseCase: GetMemberPointsHistoryUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = getMyProfileUseCase()) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(user = result.data, loading = false)
                    val qr = getMemberQrCodeUseCase(result.data.id)
                    if (qr is AppResult.Success) _uiState.value = _uiState.value.copy(qrPng = qr.data)
                    val history = getMemberPointsHistoryUseCase(result.data.id)
                    if (history is AppResult.Success) _uiState.value = _uiState.value.copy(history = history.data)
                }
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(loading = false, errorMessage = result.error.message)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { logoutUseCase() }
    }
}
