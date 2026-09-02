package com.joi.domain.usecase

import com.joi.domain.model.AppError
import com.joi.domain.model.AppResult
import com.joi.domain.model.Prize
import com.joi.domain.model.PrizeRedemption
import com.joi.domain.repository.PrizeInput
import com.joi.domain.repository.PrizeRepository

class ListPrizesUseCase(private val prizeRepository: PrizeRepository) {
    suspend operator fun invoke(activeOnly: Boolean = true): AppResult<List<Prize>> =
        prizeRepository.listPrizes(activeOnly)
}

class SavePrizeUseCase(private val prizeRepository: PrizeRepository) {
    suspend operator fun invoke(prizeId: String?, input: PrizeInput): AppResult<Prize> {
        if (input.name.isBlank()) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Prize name is required"))
        }
        if (input.pointsCost <= 0) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Points cost must be positive"))
        }
        return if (prizeId == null) {
            prizeRepository.createPrize(input)
        } else {
            prizeRepository.updatePrize(prizeId, input)
        }
    }
}

class DeletePrizeUseCase(private val prizeRepository: PrizeRepository) {
    suspend operator fun invoke(prizeId: String): AppResult<Unit> = prizeRepository.deletePrize(prizeId)
}

/** Uploads a picture picked from the gallery (as opposed to a URL the moderator already has to
 * hand) and returns the URL to use as the prize's imageUrl. Mirrors the 5MB limit the backend
 * enforces so a too-large picture fails fast with a clear message instead of a slow doomed upload. */
class UploadPrizeImageUseCase(private val prizeRepository: PrizeRepository) {
    suspend operator fun invoke(bytes: ByteArray, mimeType: String): AppResult<String> {
        if (bytes.isEmpty()) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "That image looks empty"))
        }
        if (bytes.size > 5 * 1024 * 1024) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Image must be 5MB or smaller"))
        }
        return prizeRepository.uploadImage(bytes, mimeType)
    }
}

/** Spends a member's points on a prize; the backend re-validates the balance regardless, this is just fast UI feedback. */
class RedeemPrizeUseCase(private val prizeRepository: PrizeRepository) {
    suspend operator fun invoke(prize: Prize, memberTotalPoints: Int, userId: String): AppResult<PrizeRedemption> {
        if (memberTotalPoints < prize.pointsCost) {
            return AppResult.Failure(
                AppError(
                    "VALIDATION_ERROR",
                    "Not enough points: needs ${prize.pointsCost}, has $memberTotalPoints",
                ),
            )
        }
        if (prize.quantity != null && prize.quantity <= 0) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "\"${prize.name}\" is out of stock"))
        }
        return prizeRepository.redeemPrize(prize.id, userId)
    }
}

/** Powers the "you've redeemed this" badge on the prize list. */
class GetRedeemedPrizeIdsUseCase(private val prizeRepository: PrizeRepository) {
    suspend operator fun invoke(): AppResult<Set<String>> = prizeRepository.getRedeemedPrizeIds()
}
