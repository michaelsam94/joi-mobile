package com.joi.app.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.Absentee
import com.joi.domain.model.AppResult
import com.joi.domain.usecase.GetAbsenteesUseCase
import com.joi.domain.usecase.SendWeeklyReportNowUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AbsenteesUiState(
    val absentees: List<Absentee> = emptyList(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val sendingReport: Boolean = false,
    val reportSentMessage: String? = null,
)

class AbsenteesViewModel(
    private val getAbsenteesUseCase: GetAbsenteesUseCase,
    private val sendWeeklyReportNowUseCase: SendWeeklyReportNowUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AbsenteesUiState())
    val uiState: StateFlow<AbsenteesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = getAbsenteesUseCase()) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(absentees = result.data, loading = false)
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(loading = false, errorMessage = result.error.message)
            }
        }
    }

    fun sendWeeklyReportNow() {
        if (_uiState.value.sendingReport) return
        _uiState.value = _uiState.value.copy(sendingReport = true, reportSentMessage = null)
        viewModelScope.launch {
            when (val result = sendWeeklyReportNowUseCase()) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    sendingReport = false,
                    reportSentMessage = if (result.data.sentToChatIds.isEmpty()) {
                        "Report generated, but no Telegram chat is configured on the backend yet."
                    } else {
                        "Sent to ${result.data.sentToChatIds.size} Telegram chat(s)."
                    },
                )
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(
                    sendingReport = false,
                    reportSentMessage = "Couldn't send: ${result.error.message}",
                )
            }
        }
    }

    fun dismissReportMessage() {
        _uiState.value = _uiState.value.copy(reportSentMessage = null)
    }
}
