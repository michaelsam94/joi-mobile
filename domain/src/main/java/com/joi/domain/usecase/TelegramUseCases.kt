package com.joi.domain.usecase

import com.joi.domain.model.AppResult
import com.joi.domain.model.WeeklyReportResult
import com.joi.domain.repository.TelegramRepository

class SendWeeklyReportNowUseCase(private val telegramRepository: TelegramRepository) {
    suspend operator fun invoke(): AppResult<WeeklyReportResult> = telegramRepository.sendWeeklyReportNow()
}
