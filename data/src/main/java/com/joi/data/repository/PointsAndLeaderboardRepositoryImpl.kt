package com.joi.data.repository

import com.joi.data.network.JoiApiService
import com.joi.data.network.apiCall
import com.joi.data.network.dto.AdjustPointsRequestDto
import com.joi.domain.model.AppResult
import com.joi.domain.model.map
import com.joi.domain.model.LeaderboardEntry
import com.joi.domain.model.PublicUser
import com.joi.domain.repository.LeaderboardRepository
import com.joi.domain.repository.PointsRepository

class PointsRepositoryImpl(private val api: JoiApiService) : PointsRepository {
    override suspend fun adjustPoints(userId: String, points: Int, reason: String): AppResult<PublicUser> =
        apiCall { api.adjustPoints(AdjustPointsRequestDto(userId, points, reason)) }.map { it.toDomain() }
}

class LeaderboardRepositoryImpl(private val api: JoiApiService) : LeaderboardRepository {
    override suspend fun getLeaderboard(): AppResult<List<LeaderboardEntry>> =
        apiCall { api.getLeaderboard() }.map { list -> list.map { it.toDomain() } }
}
