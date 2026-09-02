package com.joi.domain.repository

import com.joi.domain.model.AppResult
import com.joi.domain.model.Prize
import com.joi.domain.model.PrizeRedemption

data class PrizeInput(
    val name: String,
    val description: String?,
    val pointsCost: Int,
    val imageUrl: String? = null,
)

interface PrizeRepository {
    suspend fun listPrizes(activeOnly: Boolean = true): AppResult<List<Prize>>
    suspend fun createPrize(input: PrizeInput): AppResult<Prize>
    suspend fun updatePrize(prizeId: String, input: PrizeInput, active: Boolean? = null): AppResult<Prize>
    suspend fun deletePrize(prizeId: String): AppResult<Unit>
    suspend fun redeemPrize(prizeId: String, userId: String): AppResult<PrizeRedemption>
    /** Uploads raw image bytes (picked from the gallery, not a URL someone had to already have)
     * and returns the hosted URL to use as a prize's imageUrl. */
    suspend fun uploadImage(bytes: ByteArray, mimeType: String): AppResult<String>
}
