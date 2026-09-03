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
    val dateOfBirth: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val className: String? = null,
    val note: String? = null,
    val raffleNumber: Int? = null,
)

@Serializable
data class RegisterUserRequestDto(
    val fullName: String,
    val username: String,
    val temporaryPassword: String,
    val role: String? = null,
    val dateOfBirth: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val className: String? = null,
)

@Serializable
data class UpdateUserRequestDto(
    val fullName: String? = null,
    val role: String? = null,
    val active: Boolean? = null,
    val dateOfBirth: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val className: String? = null,
    val note: String? = null,
    val temporaryPassword: String? = null,
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
data class AssignRaffleNumberRequestDto(val userId: String)

@Serializable
data class RaffleNumberAssignmentDto(
    val userId: String,
    val fullName: String,
    val raffleNumber: Int,
    val alreadyHeld: Boolean = false,
)

@Serializable
data class ResetRaffleNumbersResponseDto(val cleared: Int)

@Serializable
data class RaffleNumbersResponseDto(val numbers: List<Int> = emptyList(), val count: Int = 0)

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
    val quantity: Int? = null,
)

@Serializable
data class CreatePrizeRequestDto(
    val name: String,
    val description: String? = null,
    val pointsCost: Int,
    val imageUrl: String? = null,
    val quantity: Int? = null,
)

@Serializable
data class UpdatePrizeRequestDto(
    val name: String? = null,
    val description: String? = null,
    val pointsCost: Int? = null,
    val imageUrl: String? = null,
    val active: Boolean? = null,
    val quantity: Int? = null,
)

@Serializable
data class RedeemedPrizeIdsResponseDto(val prizeIds: List<String>)

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
data class UploadImageResponseDto(val url: String)

@Serializable
data class WeeklyReportResponseDto(
    val meetingDate: String,
    val attendedCount: Int,
    val totalActiveMembers: Int,
    val absentees: List<AbsenteeDto>,
    val sentToChatIds: List<String>,
    val failedChatIds: List<String> = emptyList(),
)

@Serializable
data class ExportUrlResponseDto(val url: String)

@Serializable
data class EventDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val price: Double,
    val eventDate: String,
    val eventTime: String? = null,
    val imageUrl: String? = null,
    val active: Boolean,
    // Present on GET /events (the caller's own balance); absent on create/update responses.
    val myPaidAmount: Double = 0.0,
    val myRemainingAmount: Double = 0.0,
    val myFullyPaid: Boolean = false,
)

@Serializable
data class CreateEventRequestDto(
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val price: Double,
    val eventDate: String,
    val eventTime: String? = null,
    val imageUrl: String? = null,
)

@Serializable
data class UpdateEventRequestDto(
    val name: String? = null,
    val description: String? = null,
    val location: String? = null,
    val price: Double? = null,
    val eventDate: String? = null,
    val eventTime: String? = null,
    val imageUrl: String? = null,
    val active: Boolean? = null,
)

@Serializable
data class EventPaymentDto(
    val id: String,
    val eventId: String,
    val userId: String,
    val amount: Double,
    val note: String? = null,
    val createdAt: String,
)

@Serializable
data class EventRosterEntryDto(
    val userId: String,
    val fullName: String,
    val paidAmount: Double,
    val remainingAmount: Double,
    val fullyPaid: Boolean,
    val payments: List<EventPaymentDto> = emptyList(),
)

@Serializable
data class EventRosterDto(
    val event: EventDto,
    val entries: List<EventRosterEntryDto>,
    val totalCollected: Double,
    val totalExpected: Double,
)

@Serializable
data class MyEventPaymentsDto(
    val eventId: String,
    val price: Double,
    val paidAmount: Double,
    val remainingAmount: Double,
    val fullyPaid: Boolean,
    val payments: List<EventPaymentDto> = emptyList(),
)

@Serializable
data class RecordEventPaymentRequestDto(val userId: String, val amount: Double, val note: String? = null)

@Serializable
data class UpdateEventPaymentRequestDto(val amount: Double? = null, val note: String? = null)

@Serializable
data class SetMemberEventTotalRequestDto(val total: Double)
