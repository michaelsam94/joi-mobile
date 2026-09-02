package com.joi.data.network

import com.joi.data.network.dto.ErrorResponseDto
import com.joi.domain.model.AppError
import com.joi.domain.model.AppResult
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Every repository funnels its Retrofit call through this so failure handling is written once:
 * a non-2xx response is parsed as {error, message} (falling back to a generic message if the
 * body isn't that shape), and a thrown exception (no network, timeout, ...) becomes a clearly
 * labeled NETWORK_ERROR instead of crashing the caller.
 */
suspend fun <T> apiCall(block: suspend () -> Response<T>): AppResult<T> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                AppResult.Success(body)
            } else {
                @Suppress("UNCHECKED_CAST")
                AppResult.Success(Unit as T)
            }
        } else {
            val raw = response.errorBody()?.string()
            val parsed = raw?.let { runCatching { errorJson.decodeFromString<ErrorResponseDto>(it) }.getOrNull() }
            AppResult.Failure(
                AppError(
                    code = parsed?.error ?: "HTTP_${response.code()}",
                    message = parsed?.message ?: "Request failed (${response.code()})",
                ),
            )
        }
    } catch (e: IOException) {
        AppResult.Failure(AppError("NETWORK_ERROR", "Couldn't reach the server — check your connection"))
    } catch (e: Exception) {
        AppResult.Failure(AppError("UNKNOWN_ERROR", e.message ?: "Something went wrong"))
    }
}
