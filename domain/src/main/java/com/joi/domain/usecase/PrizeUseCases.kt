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
        return prizeRepository.redeemPrize(prize.id, userId)
    }
}
