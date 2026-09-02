package com.joi.domain.usecase

import com.joi.domain.model.AppError
import com.joi.domain.model.AppResult
import com.joi.domain.model.PointTransaction
import com.joi.domain.model.PublicUser
import com.joi.domain.model.Role
import com.joi.domain.repository.RegisterUserInput
import com.joi.domain.repository.UpdateUserInput
import com.joi.domain.repository.UserRepository

class ListMembersUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(activeOnly: Boolean = true): AppResult<List<PublicUser>> =
        userRepository.listUsers(activeOnly)
}

/** Moderator-only: registers a new person with a one-time temporary password, same rule the backend enforces. */
class RegisterMemberUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(input: RegisterUserInput): AppResult<PublicUser> {
        if (input.fullName.isBlank()) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Full name is required"))
        }
        if (input.username.trim().length < 3) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Username must be at least 3 characters"))
        }
        if (input.temporaryPassword.length < 6) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Temporary password must be at least 6 characters"))
        }
        return userRepository.registerUser(
            input.copy(fullName = input.fullName.trim(), username = input.username.trim()),
        )
    }
}

class GetMemberQrCodeUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(userId: String): AppResult<ByteArray> = userRepository.getQrCodePng(userId)
}

class GetMemberPointsHistoryUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(userId: String): AppResult<List<PointTransaction>> =
        userRepository.getPointsHistory(userId)
}

/** Finds one person by id from the full roster — the backend has no single-user-by-id endpoint,
 * only /users (list) and /users/me, so this is the client-side equivalent. */
class GetMemberUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(userId: String): AppResult<PublicUser> =
        when (val result = userRepository.listUsers(activeOnly = false)) {
            is AppResult.Success -> {
                val user = result.data.find { it.id == userId }
                if (user != null) AppResult.Success(user)
                else AppResult.Failure(AppError("NOT_FOUND", "Member not found"))
            }
            is AppResult.Failure -> result
        }
}

class SetMemberActiveUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(userId: String, active: Boolean): AppResult<PublicUser> =
        userRepository.updateUser(userId, UpdateUserInput(active = active))
}

private val DATE_OF_BIRTH_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")

/** Edits a member's profile: name, role, and the contact/personal fields moderators keep on file. */
class UpdateMemberUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(
        userId: String,
        fullName: String,
        role: Role,
        dateOfBirth: String?,
        phoneNumber: String?,
        address: String?,
        className: String?,
        note: String?,
        temporaryPassword: String?,
    ): AppResult<PublicUser> {
        if (fullName.isBlank()) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Full name is required"))
        }
        val trimmedDob = dateOfBirth?.trim()?.ifBlank { null }
        if (trimmedDob != null && !DATE_OF_BIRTH_PATTERN.matches(trimmedDob)) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Date of birth must be in YYYY-MM-DD format"))
        }
        val trimmedTempPassword = temporaryPassword?.trim()?.ifBlank { null }
        if (trimmedTempPassword != null && trimmedTempPassword.length < 6) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Temporary password must be at least 6 characters"))
        }
        return userRepository.updateUser(
            userId,
            UpdateUserInput(
                fullName = fullName.trim(),
                role = role,
                dateOfBirth = trimmedDob,
                phoneNumber = phoneNumber?.trim()?.ifBlank { null },
                address = address?.trim()?.ifBlank { null },
                className = className?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null },
                temporaryPassword = trimmedTempPassword,
            ),
        )
    }
}

class GetMyProfileUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(): AppResult<PublicUser> = userRepository.me()
}
