package com.joi.data.network

import com.joi.data.network.dto.ErrorResponseDto
import com.joi.domain.session.AuthSession
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Watches every response for the one signal that means "this token is no longer good, and not
 * just expired — the account behind it was deactivated" and clears the local session the instant
 * it sees it. JoiNavHost reacts to the session Flow, so that clear alone is what routes the person
 * back to the login screen — nothing per-screen has to know this exists.
 *
 * Uses peekBody rather than consuming the real body, so the response is still there, untouched,
 * for apiCall to parse into its usual AppResult.Failure — this only adds a side effect, it never
 * changes what the caller sees.
 */
class SessionInvalidationInterceptor(private val session: AuthSession) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            val body = runCatching { response.peekBody(2048).string() }.getOrNull()
            val code = body?.let { runCatching { errorJson.decodeFromString<ErrorResponseDto>(it).error }.getOrNull() }
            if (code == "ACCOUNT_DEACTIVATED") {
                runBlocking { session.clear() }
            }
        }
        return response
    }
}
