package com.joi.domain.repository

import com.joi.domain.model.AppResult
import com.joi.domain.model.LeaderboardEntry

interface LeaderboardRepository {
    suspend fun getLeaderboard(): AppResult<List<LeaderboardEntry>>
}
