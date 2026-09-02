package com.joi.domain.repository

import com.joi.domain.model.AppResult
import com.joi.domain.model.PublicUser

interface PointsRepository {
    suspend fun adjustPoints(userId: String, points: Int, reason: String): AppResult<PublicUser>
}
