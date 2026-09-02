package com.joi.domain.repository

import com.joi.domain.model.AppResult
import com.joi.domain.model.WeeklyReportResult

interface TelegramRepository {
    /** Moderator-only: fires the same weekly report the Friday-13:00-Cairo cron sends, right now — for testing or an ad-hoc resend. */
    suspend fun sendWeeklyReportNow(): AppResult<WeeklyReportResult>
}
