package com.joi.data.repository

import com.joi.data.network.JoiApiService
import com.joi.data.network.apiCall
import com.joi.data.network.dto.CheckInRequestDto
import com.joi.domain.model.Absentee
import com.joi.domain.model.AppResult
import com.joi.domain.model.map
import com.joi.domain.model.CheckInResult
import com.joi.domain.repository.AttendanceRepository

class AttendanceRepositoryImpl(private val api: JoiApiService) : AttendanceRepository {

    override suspend fun checkIn(qrToken: String, meetingDate: String?): AppResult<CheckInResult> =
        apiCall { api.checkIn(CheckInRequestDto(qrToken, meetingDate)) }.map { it.toDomain() }

    override suspend fun getAbsentees(meetingDate: String?): AppResult<List<Absentee>> =
        apiCall { api.getAbsentees(meetingDate) }.map { response -> response.absentees.map { it.toDomain() } }
}
