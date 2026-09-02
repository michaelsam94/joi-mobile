package com.joi.domain.usecase

import com.joi.domain.model.AppResult
import com.joi.domain.repository.ExportRepository

/** Moderator profile action: exports every table in the database to a Google Sheet and returns
 * its shareable URL. */
class ExportDatabaseUseCase(private val exportRepository: ExportRepository) {
    suspend operator fun invoke(): AppResult<String> = exportRepository.exportDatabase()
}
