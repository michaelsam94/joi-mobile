package com.joi.data.repository

import com.joi.data.network.JoiApiService
import com.joi.data.network.apiCall
import com.joi.domain.model.AppResult
import com.joi.domain.model.WeeklyReportResult
import com.joi.domain.repository.TelegramRepository

class TelegramRepositoryImpl(private val api: JoiApiService) : TelegramRepository {
    override suspend fun sendWeeklyReportNow(): AppResult<WeeklyReportResult> =
        apiCall { api.sendWeeklyReport() }.map { dto ->
            WeeklyReportResult(
                meetingDate = dto.meetingDate,
                attendedCount = dto.attendedCount,
                totalActiveMembers = dto.totalActiveMembers,
                absentees = dto.absentees.map { it.toDomain() },
                sentToChatIds = dto.sentToChatIds,
            )
        }
}
