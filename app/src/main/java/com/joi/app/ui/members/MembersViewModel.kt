package com.joi.app.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.AppResult
import com.joi.domain.model.PublicUser
import com.joi.domain.usecase.ListMembersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MembersUiState(
    val allUsers: List<PublicUser> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
    val errorMessage: String? = null,
) {
    val filtered: List<PublicUser>
        get() = if (query.isBlank()) allUsers else allUsers.filter { it.fullName.contains(query, ignoreCase = true) }
}

class MembersViewModel(private val listMembersUseCase: ListMembersUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(MembersUiState())
    val uiState: StateFlow<MembersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
    }

    fun load() {
        _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = listMembersUseCase(activeOnly = false)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(allUsers = result.data, loading = false)
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(loading = false, errorMessage = result.error.message)
            }
        }
    }
}
