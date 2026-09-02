package com.joi.domain.repository

import com.joi.domain.model.AppResult
import com.joi.domain.model.PointTransaction
import com.joi.domain.model.PublicUser
import com.joi.domain.model.Role

data class RegisterUserInput(
    val fullName: String,
    val username: String,
    val temporaryPassword: String,
    val role: Role = Role.MEMBER,
    val dateOfBirth: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val className: String? = null,
)

data class UpdateUserInput(
    val fullName: String? = null,
    val role: Role? = null,
    val active: Boolean? = null,
    val dateOfBirth: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val className: String? = null,
    val note: String? = null,
    /** Set to reset a member's forgotten password — hashed server-side and forces them to change
     * it on next login, same as a brand-new registration. */
    val temporaryPassword: String? = null,
)

interface UserRepository {
    suspend fun listUsers(activeOnly: Boolean = true): AppResult<List<PublicUser>>
    suspend fun me(): AppResult<PublicUser>
    suspend fun registerUser(input: RegisterUserInput): AppResult<PublicUser>
    suspend fun updateUser(userId: String, input: UpdateUserInput): AppResult<PublicUser>
    /** Returns the raw PNG bytes for the QR code — the caller decides how to render/cache them. */
    suspend fun getQrCodePng(userId: String): AppResult<ByteArray>
    suspend fun getPointsHistory(userId: String): AppResult<List<PointTransaction>>
}
