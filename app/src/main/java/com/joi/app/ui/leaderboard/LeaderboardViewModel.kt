package com.joi.app.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.AppResult
import com.joi.domain.model.LeaderboardEntry
import com.joi.domain.usecase.GetLeaderboardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val entries: List<LeaderboardEntry> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val errorMessage: String? = null,
)

class LeaderboardViewModel(
    private val getLeaderboardUseCase: GetLeaderboardUseCase,
    val currentUserId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        load(isRefresh = true)
    }

    private fun load(isRefresh: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            loading = !isRefresh && _uiState.value.entries.isEmpty(),
            refreshing = isRefresh,
            errorMessage = null,
        )
        viewModelScope.launch {
            when (val result = getLeaderboardUseCase()) {
                is AppResult.Success ->
                    _uiState.value = _uiState.value.copy(entries = result.data, loading = false, refreshing = false)
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        refreshing = false,
                        errorMessage = result.error.message,
                    )
            }
        }
    }
}
