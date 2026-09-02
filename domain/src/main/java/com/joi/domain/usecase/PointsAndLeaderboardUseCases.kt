package com.joi.domain.usecase

import com.joi.domain.model.AppError
import com.joi.domain.model.AppResult
import com.joi.domain.model.LeaderboardEntry
import com.joi.domain.model.PublicUser
import com.joi.domain.repository.LeaderboardRepository
import com.joi.domain.repository.PointsRepository

/** Moderator-only manual add/remove of points — always with an audited reason, same rule as the backend. */
class AdjustPointsUseCase(private val pointsRepository: PointsRepository) {
    suspend operator fun invoke(userId: String, points: Int, reason: String): AppResult<PublicUser> {
        if (points == 0) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Points delta cannot be zero"))
        }
        if (reason.isBlank()) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "A reason is required"))
        }
        return pointsRepository.adjustPoints(userId, points, reason.trim())
    }
}

class GetLeaderboardUseCase(private val leaderboardRepository: LeaderboardRepository) {
    suspend operator fun invoke(): AppResult<List<LeaderboardEntry>> = leaderboardRepository.getLeaderboard()
}
