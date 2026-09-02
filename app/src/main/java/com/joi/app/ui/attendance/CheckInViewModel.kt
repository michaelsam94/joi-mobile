package com.joi.app.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.AppResult
import com.joi.domain.model.CheckInResult
import com.joi.domain.usecase.CheckInUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CheckInUiState(
    val manualToken: String = "",
    val loading: Boolean = false,
    val lastResult: CheckInResult? = null,
    val errorMessage: String? = null,
)

class CheckInViewModel(private val checkInUseCase: CheckInUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    fun onManualTokenChange(value: String) {
        _uiState.value = _uiState.value.copy(manualToken = value)
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(lastResult = null, errorMessage = null)
    }

    /** Called with the raw text from either the QR scanner result or the manual-entry field. */
    fun checkIn(qrToken: String) {
        if (_uiState.value.loading) return
        _uiState.value = _uiState.value.copy(loading = true, errorMessage = null, lastResult = null)
        viewModelScope.launch {
            when (val result = checkInUseCase(qrToken)) {
                is AppResult.Success ->
                    _uiState.value = _uiState.value.copy(loading = false, lastResult = result.data, manualToken = "")
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(loading = false, errorMessage = result.error.message)
            }
        }
    }
}
