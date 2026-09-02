package com.joi.app.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.AppResult
import com.joi.domain.model.PointTransaction
import com.joi.domain.model.PublicUser
import com.joi.domain.usecase.AdjustPointsUseCase
import com.joi.domain.usecase.GetMemberPointsHistoryUseCase
import com.joi.domain.usecase.GetMemberQrCodeUseCase
import com.joi.domain.usecase.GetMemberUseCase
import com.joi.domain.usecase.SetMemberActiveUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MemberDetailUiState(
    val user: PublicUser? = null,
    val qrPng: ByteArray? = null,
    val history: List<PointTransaction> = emptyList(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val showAdjustDialog: Boolean = false,
    val adjustLoading: Boolean = false,
    val adjustError: String? = null,
)

class MemberDetailViewModel(
    private val userId: String,
    private val getMemberUseCase: GetMemberUseCase,
    private val getMemberQrCodeUseCase: GetMemberQrCodeUseCase,
    private val getMemberPointsHistoryUseCase: GetMemberPointsHistoryUseCase,
    private val setMemberActiveUseCase: SetMemberActiveUseCase,
    private val adjustPointsUseCase: AdjustPointsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberDetailUiState())
    val uiState: StateFlow<MemberDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            when (val userResult = getMemberUseCase(userId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(user = userResult.data, loading = false)
                    loadQr()
                    loadHistory()
                }
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(loading = false, errorMessage = userResult.error.message)
            }
        }
    }

    private fun loadQr() {
        viewModelScope.launch {
            val result = getMemberQrCodeUseCase(userId)
            if (result is AppResult.Success) {
                _uiState.value = _uiState.value.copy(qrPng = result.data)
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val result = getMemberPointsHistoryUseCase(userId)
            if (result is AppResult.Success) {
                _uiState.value = _uiState.value.copy(history = result.data)
            }
        }
    }

    fun toggleActive() {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            when (val result = setMemberActiveUseCase(userId, !user.active)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(user = result.data)
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
            }
        }
    }

    fun openAdjustDialog() {
        _uiState.value = _uiState.value.copy(showAdjustDialog = true, adjustError = null)
    }

    fun dismissAdjustDialog() {
        _uiState.value = _uiState.value.copy(showAdjustDialog = false)
    }

    fun adjustPoints(points: Int, reason: String) {
        if (_uiState.value.adjustLoading) return
        _uiState.value = _uiState.value.copy(adjustLoading = true, adjustError = null)
        viewModelScope.launch {
            when (val result = adjustPointsUseCase(userId, points, reason)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        user = result.data,
                        adjustLoading = false,
                        showAdjustDialog = false,
                    )
                    loadHistory()
                }
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(adjustLoading = false, adjustError = result.error.message)
            }
        }
    }
}
