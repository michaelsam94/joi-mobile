package com.joi.domain.repository

import com.joi.domain.model.AppResult

interface ExportRepository {
    /** Moderator-only: dumps every table in the database into a single Google Sheet on Drive
     * (one tab per table) and returns a link to it. */
    suspend fun exportDatabase(): AppResult<String>
}
