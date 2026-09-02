package com.joi.domain.repository

import com.joi.domain.model.Absentee
import com.joi.domain.model.AppResult
import com.joi.domain.model.CheckInResult

interface AttendanceRepository {
    suspend fun checkIn(qrToken: String, meetingDate: String? = null): AppResult<CheckInResult>
    suspend fun getAbsentees(meetingDate: String? = null): AppResult<List<Absentee>>
}
