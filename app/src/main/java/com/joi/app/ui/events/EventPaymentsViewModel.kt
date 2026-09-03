package com.joi.app.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.AppResult
import com.joi.domain.model.EventPayment
import com.joi.domain.model.EventRoster
import com.joi.domain.model.EventRosterEntry
import com.joi.domain.usecase.DeleteEventPaymentUseCase
import com.joi.domain.usecase.GetEventRosterUseCase
import com.joi.domain.usecase.RecordEventPaymentUseCase
import com.joi.domain.usecase.SetMemberEventTotalUseCase
import com.joi.domain.usecase.UpdateEventPaymentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Which of the two ways of changing a member's balance the open dialog is offering. */
enum class PaymentDialogMode {
    /** Add one more installment on top of what they've already paid. */
    ADD_PAYMENT,

    /** Overwrite their running total with an exact figure. */
    SET_TOTAL,
}

data class EventPaymentsUiState(
    val roster: EventRoster? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val errorMessage: String? = null,
    val query: String = "",
    /** Members whose installment list is expanded on the sheet. */
    val expandedUserIds: Set<String> = emptySet(),
    val dialogMode: PaymentDialogMode? = null,
    val dialogEntry: EventRosterEntry? = null,
    /** Set when editing one existing installment rather than adding a new one. */
    val editingPayment: EventPayment? = null,
    val saving: Boolean = false,
    val actionError: String? = null,
    val actionMessage: String? = null,
)

class EventPaymentsViewModel(
    private val eventId: String,
    private val getEventRosterUseCase: GetEventRosterUseCase,
    private val recordEventPaymentUseCase: RecordEventPaymentUseCase,
    private val setMemberEventTotalUseCase: SetMemberEventTotalUseCase,
    private val updateEventPaymentUseCase: UpdateEventPaymentUseCase,
    private val deleteEventPaymentUseCase: DeleteEventPaymentUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventPaymentsUiState())
    val uiState: StateFlow<EventPaymentsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load(isRefresh = true)

    fun load(isRefresh: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            loading = !isRefresh && _uiState.value.roster == null,
            refreshing = isRefresh,
            errorMessage = null,
        )
        viewModelScope.launch {
            when (val result = getEventRosterUseCase(eventId)) {
                is AppResult.Success ->
                    _uiState.value = _uiState.value.copy(roster = result.data, loading = false, refreshing = false)
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        refreshing = false,
                        errorMessage = result.error.message,
                    )
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun toggleExpanded(userId: String) {
        val expanded = _uiState.value.expandedUserIds
        _uiState.value = _uiState.value.copy(
            expandedUserIds = if (userId in expanded) expanded - userId else expanded + userId,
        )
    }

    fun openAddPayment(entry: EventRosterEntry) {
        _uiState.value = _uiState.value.copy(
            dialogMode = PaymentDialogMode.ADD_PAYMENT,
            dialogEntry = entry,
            editingPayment = null,
            actionError = null,
        )
    }

    fun openSetTotal(entry: EventRosterEntry) {
        _uiState.value = _uiState.value.copy(
            dialogMode = PaymentDialogMode.SET_TOTAL,
            dialogEntry = entry,
            editingPayment = null,
            actionError = null,
        )
    }

    fun openEditPayment(entry: EventRosterEntry, payment: EventPayment) {
        _uiState.value = _uiState.value.copy(
            dialogMode = PaymentDialogMode.ADD_PAYMENT,
            dialogEntry = entry,
            editingPayment = payment,
            actionError = null,
        )
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(dialogMode = null, dialogEntry = null, editingPayment = null)
    }

    /**
     * Applies whatever the open dialog is for: a brand-new installment, an edit to an existing
     * one, or an outright total. All three end with a reload so the sheet's totals and the
     * member's own view agree with the ledger.
     */
    fun submit(amount: Double, note: String?) {
        val entry = _uiState.value.dialogEntry ?: return
        val mode = _uiState.value.dialogMode ?: return
        val editing = _uiState.value.editingPayment
        _uiState.value = _uiState.value.copy(saving = true, actionError = null)

        viewModelScope.launch {
            val result: AppResult<*> = when {
                editing != null -> updateEventPaymentUseCase(eventId, editing.id, amount, note)
                mode == PaymentDialogMode.SET_TOTAL -> setMemberEventTotalUseCase(eventId, entry.userId, amount)
                else -> recordEventPaymentUseCase(eventId, entry.userId, amount, note)
            }
            when (result) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        saving = false,
                        dialogMode = null,
                        dialogEntry = null,
                        editingPayment = null,
                        actionMessage = when {
                            editing != null -> "Updated ${entry.fullName}'s payment"
                            mode == PaymentDialogMode.SET_TOTAL -> "Set ${entry.fullName}'s total"
                            else -> "Recorded a payment for ${entry.fullName}"
                        },
                    )
                    load(isRefresh = true)
                }
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(saving = false, actionError = result.error.message)
            }
        }
    }

    fun deletePayment(payment: EventPayment) {
        viewModelScope.launch {
            when (val result = deleteEventPaymentUseCase(eventId, payment.id)) {
                is AppResult.Success -> load(isRefresh = true)
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(actionError = result.error.message)
            }
        }
    }

    fun dismissActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun dismissActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }
}
