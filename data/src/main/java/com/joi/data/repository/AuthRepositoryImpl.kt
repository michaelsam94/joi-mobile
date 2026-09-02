package com.joi.data.repository

import com.joi.data.network.JoiApiService
import com.joi.data.network.apiCall
import com.joi.data.network.dto.ChangePasswordRequestDto
import com.joi.data.network.dto.LoginRequestDto
import com.joi.domain.model.AppResult
import com.joi.domain.repository.AuthRepository
import com.joi.domain.repository.LoginResult

class AuthRepositoryImpl(private val api: JoiApiService) : AuthRepository {

    override suspend fun login(username: String, password: String): AppResult<LoginResult> =
        apiCall { api.login(LoginRequestDto(username, password)) }.let { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(
                    LoginResult(
                        token = result.data.token,
                        mustChangePassword = result.data.mustChangePassword,
                        user = result.data.user.toDomain(),
                    ),
                )
                is AppResult.Failure -> result
            }
        }

    override suspend fun changePassword(newPassword: String): AppResult<Unit> =
        when (val result = apiCall { api.changePassword(ChangePasswordRequestDto(newPassword)) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }
}
