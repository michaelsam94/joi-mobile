package com.joi.data.repository

import com.joi.data.network.JoiApiService
import com.joi.data.network.apiCall
import com.joi.data.network.dto.RegisterUserRequestDto
import com.joi.data.network.dto.UpdateUserRequestDto
import com.joi.domain.model.AppResult
import com.joi.domain.model.PointTransaction
import com.joi.domain.model.PublicUser
import com.joi.domain.repository.RegisterUserInput
import com.joi.domain.repository.UpdateUserInput
import com.joi.domain.repository.UserRepository

class UserRepositoryImpl(private val api: JoiApiService) : UserRepository {

    override suspend fun listUsers(activeOnly: Boolean): AppResult<List<PublicUser>> =
        apiCall { api.listUsers(activeOnly) }.map { list -> list.map { it.toDomain() } }

    override suspend fun me(): AppResult<PublicUser> =
        apiCall { api.me() }.map { it.toDomain() }

    override suspend fun registerUser(input: RegisterUserInput): AppResult<PublicUser> =
        apiCall {
            api.registerUser(
                RegisterUserRequestDto(
                    fullName = input.fullName,
                    username = input.username,
                    temporaryPassword = input.temporaryPassword,
                    role = input.role.name,
                ),
            )
        }.map { it.toDomain() }

    override suspend fun updateUser(userId: String, input: UpdateUserInput): AppResult<PublicUser> =
        apiCall {
            api.updateUser(
                userId,
                UpdateUserRequestDto(fullName = input.fullName, role = input.role?.name, active = input.active),
            )
        }.map { it.toDomain() }

    override suspend fun getQrCodePng(userId: String): AppResult<ByteArray> =
        apiCall { api.getQrCode(userId) }.map { it.bytes() }

    override suspend fun getPointsHistory(userId: String): AppResult<List<PointTransaction>> =
        apiCall { api.getPointsHistory(userId) }.map { list -> list.map { it.toDomain() } }
}
