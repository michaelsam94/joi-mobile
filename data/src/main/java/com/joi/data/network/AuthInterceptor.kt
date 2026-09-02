package com.joi.data.network

import com.joi.domain.session.AuthSession
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the current session's bearer token to every request. OkHttp interceptors are
 * synchronous, so this does a blocking read of the (very fast, disk-cached) DataStore value —
 * the standard pattern for wiring a suspend-based session store into OkHttp.
 */
class AuthInterceptor(private val session: AuthSession) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { session.current().token }
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}
