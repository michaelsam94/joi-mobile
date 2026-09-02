package com.joi.data.repository

import com.joi.data.network.JoiApiService
import com.joi.data.network.apiCall
import com.joi.domain.model.AppResult
import com.joi.domain.model.map
import com.joi.domain.repository.ExportRepository

class ExportRepositoryImpl(private val api: JoiApiService) : ExportRepository {
    override suspend fun exportDatabase(): AppResult<String> =
        apiCall { api.exportDatabase() }.map { it.url }
}
