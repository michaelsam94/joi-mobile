package com.joi.app.ui.raffle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.AppResult
import com.joi.domain.usecase.ListRaffleNumbersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LuckyWheelUiState(
    /** Every number handed out, straight from the backend. Never reordered here — it arrives
     * sorted precisely so nothing about it hints at who holds which. */
    val allNumbers: List<Int> = emptyList(),
    /** Numbers taken out of the wheel after being drawn. Local to this screen on purpose — the
     * member keeps their number (it's still on their profile), they're just not eligible again. */
    val removed: Set<Int> = emptySet(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val spinning: Boolean = false,
    /** The number the wheel is currently spinning towards. Chosen up front so the animation can
     * land on it — the pick is already made before the wheel starts moving. */
    val pendingWinner: Int? = null,
    /** Revealed only once the wheel has stopped, and cleared by keeping or removing it. */
    val winner: Int? = null,
    /** Bumped on every spin so the screen re-runs its animation even for a repeat winner. */
    val spinId: Int = 0,
) {
    /** What the wheel actually shows: everything not drawn-and-removed this session. */
    val inPlay: List<Int> get() = allNumbers.filterNot { it in removed }
}

class LuckyWheelViewModel(
    private val listRaffleNumbersUseCase: ListRaffleNumbersUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LuckyWheelUiState())
    val uiState: StateFlow<LuckyWheelUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = listRaffleNumbersUseCase()) {
                is AppResult.Success ->
                    _uiState.value = _uiState.value.copy(
                        allNumbers = result.data,
                        loading = false,
                        // A number that's gone from the pool entirely (numbers were reset) has
                        // nothing left to exclude.
                        removed = _uiState.value.removed.filter { it in result.data }.toSet(),
                    )
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(loading = false, errorMessage = result.error.message)
            }
        }
    }

    /**
     * Picks the winner, then lets the wheel animate to it. Choosing first and animating second is
     * what makes the draw honest — where the wheel stops can't be nudged by the animation.
     */
    fun spin() {
        val state = _uiState.value
        if (state.spinning) return
        val pool = state.inPlay
        if (pool.isEmpty()) return
        _uiState.value = state.copy(
            spinning = true,
            winner = null,
            pendingWinner = pool.random(),
            spinId = state.spinId + 1,
        )
    }

    /** Called by the screen once the wheel has come to rest. */
    fun onSpinSettled() {
        val state = _uiState.value
        if (!state.spinning) return
        _uiState.value = state.copy(spinning = false, winner = state.pendingWinner, pendingWinner = null)
    }

    /** Leaves the drawn number in the wheel — it can come up again next round. */
    fun keepWinner() {
        _uiState.value = _uiState.value.copy(winner = null)
    }

    /** Takes the drawn number out for the rest of this session, so the next round draws from the
     * others. The member still holds the number; it just can't win twice. */
    fun removeWinner() {
        val winner = _uiState.value.winner ?: return
        _uiState.value = _uiState.value.copy(winner = null, removed = _uiState.value.removed + winner)
    }

    /** Puts every removed number back — starting the draw over without a new check-in round. */
    fun restoreRemoved() {
        _uiState.value = _uiState.value.copy(removed = emptySet())
    }
}
