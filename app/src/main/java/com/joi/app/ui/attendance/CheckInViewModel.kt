package com.joi.app.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.AppResult
import com.joi.domain.model.CheckInResult
import com.joi.domain.usecase.AssignRaffleNumberUseCase
import com.joi.domain.usecase.CheckInUseCase
import com.joi.domain.usecase.ResetRaffleNumbersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CheckInUiState(
    val manualToken: String = "",
    val loading: Boolean = false,
    val lastResult: CheckInResult? = null,
    val errorMessage: String? = null,
    /** The draw number just handed to the person in the check-in popup, if the moderator chose
     * to give one. Null means they haven't (yet) — giving a number is always optional. */
    val assignedNumber: Int? = null,
    val assigningNumber: Boolean = false,
    val numberError: String? = null,
    val showResetConfirm: Boolean = false,
    val resetting: Boolean = false,
    /** How many numbers the last reset cleared — shown once, then dismissed. */
    val resetCleared: Int? = null,
)

class CheckInViewModel(
    private val checkInUseCase: CheckInUseCase,
    private val assignRaffleNumberUseCase: AssignRaffleNumberUseCase,
    private val resetRaffleNumbersUseCase: ResetRaffleNumbersUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    fun onManualTokenChange(value: String) {
        _uiState.value = _uiState.value.copy(manualToken = value)
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(
            lastResult = null,
            errorMessage = null,
            assignedNumber = null,
            numberError = null,
        )
    }

    /** Called with the raw text from either the QR scanner result or the manual-entry field. */
    fun checkIn(qrToken: String) {
        if (_uiState.value.loading) return
        _uiState.value = _uiState.value.copy(
            loading = true,
            errorMessage = null,
            lastResult = null,
            // A fresh scan starts a fresh popup — never carry the previous person's number over.
            assignedNumber = null,
            numberError = null,
        )
        viewModelScope.launch {
            when (val result = checkInUseCase(qrToken)) {
                is AppResult.Success ->
                    _uiState.value = _uiState.value.copy(loading = false, lastResult = result.data, manualToken = "")
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(loading = false, errorMessage = result.error.message)
            }
        }
    }

    /** The optional extra in the check-in popup: hand this person a number for the meeting's
     * raffle or activity. Tapping again returns the same number rather than redrawing. */
    fun assignRaffleNumber() {
        val member = _uiState.value.lastResult ?: return
        if (_uiState.value.assigningNumber) return
        _uiState.value = _uiState.value.copy(assigningNumber = true, numberError = null)
        viewModelScope.launch {
            when (val result = assignRaffleNumberUseCase(member.userId)) {
                is AppResult.Success ->
                    _uiState.value = _uiState.value.copy(
                        assigningNumber = false,
                        assignedNumber = result.data.raffleNumber,
                    )
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(assigningNumber = false, numberError = result.error.message)
            }
        }
    }

    fun askResetNumbers() {
        _uiState.value = _uiState.value.copy(showResetConfirm = true)
    }

    fun dismissResetConfirm() {
        _uiState.value = _uiState.value.copy(showResetConfirm = false)
    }

    /** Clears everyone's number — after this, members stop seeing one on their profile. */
    fun resetRaffleNumbers() {
        if (_uiState.value.resetting) return
        _uiState.value = _uiState.value.copy(resetting = true)
        viewModelScope.launch {
            when (val result = resetRaffleNumbersUseCase()) {
                is AppResult.Success ->
                    _uiState.value = _uiState.value.copy(
                        resetting = false,
                        showResetConfirm = false,
                        resetCleared = result.data,
                        // Whatever was on screen from the last scan is stale now.
                        assignedNumber = null,
                    )
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(
                        resetting = false,
                        showResetConfirm = false,
                        errorMessage = result.error.message,
                    )
            }
        }
    }

    fun dismissResetResult() {
        _uiState.value = _uiState.value.copy(resetCleared = null)
    }
}
