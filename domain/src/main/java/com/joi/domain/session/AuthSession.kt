package com.joi.domain.session

import com.joi.domain.model.Role
import kotlinx.coroutines.flow.Flow

/** What's persisted locally about who's signed in — enough to route navigation without a network call. */
data class SessionState(
    val token: String?,
    val userId: String?,
    val fullName: String?,
    val role: Role?,
    val mustChangePassword: Boolean,
) {
    val isSignedIn: Boolean get() = token != null

    companion object {
        val SignedOut = SessionState(null, null, null, null, mustChangePassword = false)
    }
}

/**
 * Port for persisting the session (implemented in `data` with DataStore). Kept in `domain` so
 * use-cases and ViewModels depend on this interface, never on the concrete storage mechanism.
 */
interface AuthSession {
    val state: Flow<SessionState>
    suspend fun current(): SessionState
    suspend fun save(
        token: String,
        userId: String,
        fullName: String,
        role: Role,
        mustChangePassword: Boolean,
    )
    suspend fun updateMustChangePassword(value: Boolean)
    suspend fun clear()
}
