package com.joi.data.network.dto

import kotlinx.serialization.Serializable

// Every DTO here mirrors a JSON shape the backend actually returns/expects — see
// joi-backend/src/interfaces/http/routes/*.ts and joi-backend/src/interfaces/http/dto/schemas.ts.

@Serializable
data class ErrorResponseDto(
    val error: String,
    val message: String,
)

@Serializable
data class LoginRequestDto(val username: String, val password: String)

@Serializable
data class ChangePasswordRequestDto(val newPassword: String)

@Serializable
data class OkResponseDto(val ok: Boolean = true)

@Serializable
data class CurrentUserDto(val id: String, val fullName: String, val role: String)

@Serializable
data class LoginResponseDto(
    val token: String,
    val mustChangePassword: Boolean,
    val user: CurrentUserDto,
)

@Serializable
data class PublicUserDto(
    val id: String,
    val fullName: String,
    val role: String,
    val totalPoints: Int,
    val level: String,
    val active: Boolean,
    val username: String? = null,
    val telegramChatId: String? = null,
)

@Serializable
data class RegisterUserRequestDto(
    val fullName: String,
    val username: String,
    val temporaryPassword: String,
    val role: String? = null,
)

@Serializable
data class UpdateUserRequestDto(
    val fullName: String? = null,
    val role: String? = null,
    val active: Boolean? = null,
)

@Serializable
data class PointTransactionDto(
    val id: String,
    val points: Int,
    val type: String,
    val reason: String? = null,
    val createdAt: String,
)

@Serializable
data class CheckInRequestDto(val qrToken: String, val meetingDate: String? = null)

@Serializable
data class CheckInResponseDto(
    val userId: String,
    val fullName: String,
    val meetingDate: String,
    val pointsAwarded: Int,
    val totalPoints: Int,
)

@Serializable
data class AbsenteeDto(
    val userId: String,
    val fullName: String,
    val totalHistoricalAttendance: Int,
)

@Serializable
data class AbsenteesResponseDto(val meetingDate: String, val absentees: List<AbsenteeDto>)

@Serializable
data class AdjustPointsRequestDto(val userId: String, val points: Int, val reason: String)

@Serializable
data class LeaderboardEntryDto(
    val rank: Int,
    val userId: String,
    val fullName: String,
    val totalPoints: Int,
    val level: String,
)

@Serializable
data class PrizeDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val pointsCost: Int,
    val imageUrl: String? = null,
    val active: Boolean,
)

@Serializable
data class CreatePrizeRequestDto(
    val name: String,
    val description: String? = null,
    val pointsCost: Int,
    val imageUrl: String? = null,
)

@Serializable
data class UpdatePrizeRequestDto(
    val name: String? = null,
    val description: String? = null,
    val pointsCost: Int? = null,
    val imageUrl: String? = null,
    val active: Boolean? = null,
)

@Serializable
data class RedeemPrizeRequestDto(val userId: String)

@Serializable
data class PrizeRedemptionDto(
    val id: String,
    val prizeId: String,
    val userId: String,
    val pointsSpent: Int,
    val createdAt: String,
)

@Serializable
data class WeeklyReportResponseDto(
    val meetingDate: String,
    val attendedCount: Int,
    val totalActiveMembers: Int,
    val absentees: List<AbsenteeDto>,
    val sentToChatIds: List<String>,
)
