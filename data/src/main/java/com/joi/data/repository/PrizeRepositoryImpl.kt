package com.joi.data.repository

import com.joi.data.network.JoiApiService
import com.joi.data.network.apiCall
import com.joi.data.network.dto.CreatePrizeRequestDto
import com.joi.data.network.dto.RedeemPrizeRequestDto
import com.joi.data.network.dto.UpdatePrizeRequestDto
import com.joi.domain.model.AppResult
import com.joi.domain.model.map
import com.joi.domain.model.Prize
import com.joi.domain.model.PrizeRedemption
import com.joi.domain.repository.PrizeInput
import com.joi.domain.repository.PrizeRepository

class PrizeRepositoryImpl(private val api: JoiApiService) : PrizeRepository {

    override suspend fun listPrizes(activeOnly: Boolean): AppResult<List<Prize>> =
        apiCall { api.listPrizes(activeOnly) }.map { list -> list.map { it.toDomain() } }

    override suspend fun createPrize(input: PrizeInput): AppResult<Prize> =
        apiCall {
            api.createPrize(CreatePrizeRequestDto(input.name, input.description, input.pointsCost, input.imageUrl))
        }.map { it.toDomain() }

    override suspend fun updatePrize(prizeId: String, input: PrizeInput, active: Boolean?): AppResult<Prize> =
        apiCall {
            api.updatePrize(
                prizeId,
                UpdatePrizeRequestDto(input.name, input.description, input.pointsCost, input.imageUrl, active),
            )
        }.map { it.toDomain() }

    override suspend fun deletePrize(prizeId: String): AppResult<Unit> =
        when (val result = apiCall { api.deletePrize(prizeId) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }

    override suspend fun redeemPrize(prizeId: String, userId: String): AppResult<PrizeRedemption> =
        apiCall { api.redeemPrize(prizeId, RedeemPrizeRequestDto(userId)) }.map { it.toDomain() }
}
