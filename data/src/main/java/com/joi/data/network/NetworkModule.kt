package com.joi.data.network

import com.joi.domain.session.AuthSession
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Builds the Retrofit client used by every repository in `data`. One instance, shared. */
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun buildApiService(baseUrl: String, session: AuthSession, debugLogging: Boolean): JoiApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (debugLogging) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(JoiApiService::class.java)
    }

    /** A second OkHttp client (no JSON converter needed) reused by Coil to load auth-gated QR images. */
    fun buildAuthedHttpClient(session: AuthSession): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .build()
}
