package com.joi.domain.usecase

import com.joi.domain.model.AppError
import com.joi.domain.model.AppResult
import com.joi.domain.repository.AuthRepository
import com.joi.domain.repository.LoginResult
import com.joi.domain.session.AuthSession

class LoginUseCase(
    private val authRepository: AuthRepository,
    private val session: AuthSession,
) {
    suspend operator fun invoke(username: String, password: String): AppResult<LoginResult> {
        if (username.isBlank() || password.isBlank()) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Enter both a username and password"))
        }
        return when (val result = authRepository.login(username.trim(), password)) {
            is AppResult.Success -> {
                session.save(
                    token = result.data.token,
                    userId = result.data.user.id,
                    fullName = result.data.user.fullName,
                    role = result.data.user.role,
                    mustChangePassword = result.data.mustChangePassword,
                )
                result
            }
            is AppResult.Failure -> result
        }
    }
}

class ChangePasswordUseCase(
    private val authRepository: AuthRepository,
    private val session: AuthSession,
) {
    suspend operator fun invoke(newPassword: String, confirmPassword: String): AppResult<Unit> {
        if (newPassword.length < 6) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Password must be at least 6 characters"))
        }
        if (newPassword != confirmPassword) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Passwords don't match"))
        }
        return when (val result = authRepository.changePassword(newPassword)) {
            is AppResult.Success -> {
                session.updateMustChangePassword(false)
                result
            }
            is AppResult.Failure -> result
        }
    }
}

class LogoutUseCase(private val session: AuthSession) {
    suspend operator fun invoke() = session.clear()
}
