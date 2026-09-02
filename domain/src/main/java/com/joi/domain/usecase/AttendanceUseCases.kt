package com.joi.domain.usecase

import com.joi.domain.model.AppError
import com.joi.domain.model.AppResult
import com.joi.domain.model.Absentee
import com.joi.domain.model.CheckInResult
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
