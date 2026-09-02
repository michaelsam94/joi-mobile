package com.joi.domain.model

enum class PointType { ATTENDANCE, MANUAL_ADD, MANUAL_REMOVE, PRIZE_REDEEM }

data class PointTransaction(
    val id: String,
    val points: Int,
    val type: PointType,
    val reason: String?,
    val createdAt: String,
)

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val fullName: String,
    val totalPoints: Int,
    val level: Level,
)
