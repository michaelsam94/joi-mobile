package com.joi.domain.repository

import com.joi.domain.model.AppResult
import com.joi.domain.model.CurrentUser
import com.joi.domain.model.Role

data class LoginResult(
    val token: String,
    val mustChangePassword: Boolean,
    val user: CurrentUser,
)

interface AuthRepository {
    suspend fun login(username: String, password: String): AppResult<LoginResult>
    suspend fun changePassword(newPassword: String): AppResult<Unit>
}

/** Kept here (not just in DTOs) so `Role` round-trips through the repository layer with a real type. */
fun roleFromWire(value: String): Role = if (value == "MODERATOR") Role.MODERATOR else Role.MEMBER
