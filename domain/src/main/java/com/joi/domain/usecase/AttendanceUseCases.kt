package com.joi.domain.usecase

import com.joi.domain.model.AppError
import com.joi.domain.model.AppResult
import com.joi.domain.model.Absentee
import com.joi.domain.model.CheckInResult
import com.joi.domain.model.RaffleNumberAssignment
import com.joi.domain.repository.AttendanceRepository

/** Scanner endpoint: a moderator scans (or manually enters) a QR token to check someone in. */
class CheckInUseCase(private val attendanceRepository: AttendanceRepository) {
    suspend operator fun invoke(qrToken: String): AppResult<CheckInResult> {
        if (qrToken.isBlank()) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "No QR code detected"))
        }
        return attendanceRepository.checkIn(qrToken.trim())
    }
}

class GetAbsenteesUseCase(private val attendanceRepository: AttendanceRepository) {
    suspend operator fun invoke(): AppResult<List<Absentee>> = attendanceRepository.getAbsentees()
}

/**
 * Optional step after a check-in: give the member a temporary number to use in the meeting's
 * raffle or activity. Asking twice for the same person returns the number they already hold
 * rather than redrawing — see the backend's AssignRaffleNumberUseCase.
 */
class AssignRaffleNumberUseCase(private val attendanceRepository: AttendanceRepository) {
    suspend operator fun invoke(userId: String): AppResult<RaffleNumberAssignment> {
        if (userId.isBlank()) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "No member to give a number to"))
        }
        return attendanceRepository.assignRaffleNumber(userId)
    }
}

/** "The activity is over" — clears every draw number at once, so they vanish from members'
 * profiles and the pool is free for the next meeting. */
class ResetRaffleNumbersUseCase(private val attendanceRepository: AttendanceRepository) {
    suspend operator fun invoke(): AppResult<Int> = attendanceRepository.resetRaffleNumbers()
}
