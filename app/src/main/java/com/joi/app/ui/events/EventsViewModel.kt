package com.joi.app.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.AppResult
import com.joi.domain.model.Event
import com.joi.domain.model.MyEventPayments
import com.joi.domain.repository.EventInput
import com.joi.domain.usecase.DeleteEventUseCase
import com.joi.domain.usecase.GetMyEventPaymentsUseCase
import com.joi.domain.usecase.ListEventsUseCase
import com.joi.domain.usecase.SaveEventUseCase
import com.joi.domain.usecase.UploadEventImageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EventsUiState(
    val events: List<Event> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val errorMessage: String? = null,
    /** False shows past events too — moderators planning, members reminiscing. */
    val upcomingOnly: Boolean = true,
    val editingEvent: Event? = null,
    val showEditor: Boolean = false,
    val uploadingImage: Boolean = false,
    val actionError: String? = null,
    /** The event whose own-payment breakdown the viewer has opened, with its installments. */
    val myPaymentsFor: Event? = null,
    val myPayments: MyEventPayments? = null,
    val loadingMyPayments: Boolean = false,
)

class EventsViewModel(
    private val isModerator: Boolean,
    private val listEventsUseCase: ListEventsUseCase,
    private val saveEventUseCase: SaveEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val getMyEventPaymentsUseCase: GetMyEventPaymentsUseCase,
    private val uploadEventImageUseCase: UploadEventImageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load(isRefresh = true)

    fun load(isRefresh: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            loading = !isRefresh && _uiState.value.events.isEmpty(),
            refreshing = isRefresh,
            errorMessage = null,
        )
        viewModelScope.launch {
            // Only a moderator may ask for hidden (inactive) events; the backend ignores the flag
            // for anyone else anyway.
            when (val result = listEventsUseCase(_uiState.value.upcomingOnly, activeOnly = !isModerator)) {
                is AppResult.Success ->
                    _uiState.value = _uiState.value.copy(events = result.data, loading = false, refreshing = false)
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        refreshing = false,
                        errorMessage = result.error.message,
                    )
            }
        }
    }

    fun toggleUpcomingOnly() {
        _uiState.value = _uiState.value.copy(upcomingOnly = !_uiState.value.upcomingOnly, events = emptyList())
        load()
    }

    fun openCreate() {
        _uiState.value = _uiState.value.copy(editingEvent = null, showEditor = true, actionError = null)
    }

    fun openEdit(event: Event) {
        _uiState.value = _uiState.value.copy(editingEvent = event, showEditor = true, actionError = null)
    }

    fun dismissEditor() {
        _uiState.value = _uiState.value.copy(showEditor = false)
    }

    fun saveEvent(input: EventInput) {
        viewModelScope.launch {
            when (val result = saveEventUseCase(_uiState.value.editingEvent?.id, input)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(showEditor = false)
                    load()
                }
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(actionError = result.error.message)
            }
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            when (val result = deleteEventUseCase(event.id)) {
                is AppResult.Success -> load()
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(actionError = result.error.message)
            }
        }
    }

    /** Uploads a poster picked from the gallery and hands the hosted URL back via [onDone]
     * (null on failure — [EventsUiState.actionError] carries the reason). */
    fun uploadImage(bytes: ByteArray, mimeType: String, onDone: (String?) -> Unit) {
        _uiState.value = _uiState.value.copy(uploadingImage = true, actionError = null)
        viewModelScope.launch {
            when (val result = uploadEventImageUseCase(bytes, mimeType)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(uploadingImage = false)
                    onDone(result.data)
                }
                is AppResult.Failure -> {
                    _uiState.value = _uiState.value.copy(uploadingImage = false, actionError = result.error.message)
                    onDone(null)
                }
            }
        }
    }

    /** Opens the viewer's own installment breakdown for one event — every payment recorded
     * against them, and what's still owed. */
    fun openMyPayments(event: Event) {
        _uiState.value = _uiState.value.copy(myPaymentsFor = event, myPayments = null, loadingMyPayments = true)
        viewModelScope.launch {
            when (val result = getMyEventPaymentsUseCase(event.id)) {
                is AppResult.Success ->
                    _uiState.value = _uiState.value.copy(myPayments = result.data, loadingMyPayments = false)
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(
                        loadingMyPayments = false,
                        actionError = result.error.message,
                    )
            }
        }
    }

    fun dismissMyPayments() {
        _uiState.value = _uiState.value.copy(myPaymentsFor = null, myPayments = null)
    }

    fun dismissActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }
}
