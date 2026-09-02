package com.joi.app.ui.prizes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joi.domain.model.AppResult
import com.joi.domain.model.Prize
import com.joi.domain.model.PublicUser
import com.joi.domain.model.Role
import com.joi.domain.repository.PrizeInput
import com.joi.domain.usecase.DeletePrizeUseCase
import com.joi.domain.usecase.GetRedeemedPrizeIdsUseCase
import com.joi.domain.usecase.ListMembersUseCase
import com.joi.domain.usecase.ListPrizesUseCase
import com.joi.domain.usecase.RedeemPrizeUseCase
import com.joi.domain.usecase.SavePrizeUseCase
import com.joi.domain.usecase.UploadPrizeImageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrizesUiState(
    val prizes: List<Prize> = emptyList(),
    val members: List<PublicUser> = emptyList(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val editingPrize: Prize? = null,
    val showEditor: Boolean = false,
    val redeemingPrize: Prize? = null,
    val actionError: String? = null,
    val actionMessage: String? = null,
    val uploadingImage: Boolean = false,
    /** Prize ids the signed-in user has personally redeemed before — drives the "you've redeemed
     * this" badge. */
    val redeemedPrizeIds: Set<String> = emptySet(),
)

class PrizesViewModel(
    private val isModerator: Boolean,
    private val listPrizesUseCase: ListPrizesUseCase,
    private val savePrizeUseCase: SavePrizeUseCase,
    private val deletePrizeUseCase: DeletePrizeUseCase,
    private val redeemPrizeUseCase: RedeemPrizeUseCase,
    private val listMembersUseCase: ListMembersUseCase,
    private val uploadPrizeImageUseCase: UploadPrizeImageUseCase,
    private val getRedeemedPrizeIdsUseCase: GetRedeemedPrizeIdsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrizesUiState())
    val uiState: StateFlow<PrizesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = listPrizesUseCase(activeOnly = !isModerator)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(prizes = result.data, loading = false)
                is AppResult.Failure ->
                    _uiState.value = _uiState.value.copy(loading = false, errorMessage = result.error.message)
            }
            if (isModerator) {
                val members = listMembersUseCase(activeOnly = true)
                if (members is AppResult.Success) {
                    _uiState.value = _uiState.value.copy(members = members.data.filter { it.role == Role.MEMBER })
                }
            }
            val redeemed = getRedeemedPrizeIdsUseCase()
            if (redeemed is AppResult.Success) {
                _uiState.value = _uiState.value.copy(redeemedPrizeIds = redeemed.data)
            }
        }
    }

    fun openCreate() {
        _uiState.value = _uiState.value.copy(editingPrize = null, showEditor = true, actionError = null)
    }

    fun openEdit(prize: Prize) {
        _uiState.value = _uiState.value.copy(editingPrize = prize, showEditor = true, actionError = null)
    }

    fun dismissEditor() {
        _uiState.value = _uiState.value.copy(showEditor = false)
    }

    fun savePrize(name: String, description: String, pointsCost: Int, imageUrl: String?, quantity: Int?) {
        viewModelScope.launch {
            val editing = _uiState.value.editingPrize
            val input = PrizeInput(
                name = name,
                description = description.ifBlank { null },
                pointsCost = pointsCost,
                imageUrl = imageUrl,
                quantity = quantity,
            )
            when (val result = savePrizeUseCase(editing?.id, input)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(showEditor = false)
                    load()
                }
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(actionError = result.error.message)
            }
        }
    }

    /** Uploads a picture picked from the gallery and hands the resulting hosted URL back via
     * [onDone] (null on failure — [actionError] is set for the dialog to show). */
    fun uploadImage(bytes: ByteArray, mimeType: String, onDone: (String?) -> Unit) {
        _uiState.value = _uiState.value.copy(uploadingImage = true, actionError = null)
        viewModelScope.launch {
            when (val result = uploadPrizeImageUseCase(bytes, mimeType)) {
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

    fun deletePrize(prize: Prize) {
        viewModelScope.launch {
            when (val result = deletePrizeUseCase(prize.id)) {
                is AppResult.Success -> load()
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(actionError = result.error.message)
            }
        }
    }

    fun openRedeem(prize: Prize) {
        _uiState.value = _uiState.value.copy(redeemingPrize = prize, actionError = null)
    }

    fun dismissRedeem() {
        _uiState.value = _uiState.value.copy(redeemingPrize = null)
    }

    fun redeem(userId: String) {
        val prize = _uiState.value.redeemingPrize ?: return
        val member = _uiState.value.members.find { it.id == userId } ?: return
        viewModelScope.launch {
            when (val result = redeemPrizeUseCase(prize, member.totalPoints, userId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        redeemingPrize = null,
                        actionMessage = "🎉 ${member.fullName} redeemed \"${prize.name}\"",
                    )
                    load()
                }
                is AppResult.Failure -> _uiState.value = _uiState.value.copy(actionError = result.error.message)
            }
        }
    }

    fun dismissActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }
}
