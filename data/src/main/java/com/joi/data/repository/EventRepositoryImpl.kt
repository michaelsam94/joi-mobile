package com.joi.data.repository

import com.joi.data.network.JoiApiService
import com.joi.data.network.apiCall
import com.joi.data.network.dto.CreateEventRequestDto
import com.joi.data.network.dto.RecordEventPaymentRequestDto
import com.joi.data.network.dto.SetMemberEventTotalRequestDto
import com.joi.data.network.dto.UpdateEventPaymentRequestDto
import com.joi.data.network.dto.UpdateEventRequestDto
import com.joi.domain.model.AppResult
import com.joi.domain.model.Event
import com.joi.domain.model.EventPayment
import com.joi.domain.model.EventRoster
import com.joi.domain.model.MyEventPayments
import com.joi.domain.model.map
import com.joi.domain.repository.EventInput
import com.joi.domain.repository.EventRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class EventRepositoryImpl(private val api: JoiApiService) : EventRepository {

    override suspend fun listEvents(upcomingOnly: Boolean, activeOnly: Boolean): AppResult<List<Event>> =
        apiCall { api.listEvents(upcomingOnly, activeOnly) }.map { list -> list.map { it.toDomain() } }

    override suspend fun createEvent(input: EventInput): AppResult<Event> =
        apiCall {
            api.createEvent(
                CreateEventRequestDto(
                    name = input.name,
                    description = input.description,
                    location = input.location,
                    price = input.price,
                    eventDate = input.eventDate,
                    eventTime = input.eventTime,
                    imageUrl = input.imageUrl,
                ),
            )
        }.map { it.toDomain() }

    override suspend fun updateEvent(eventId: String, input: EventInput, active: Boolean?): AppResult<Event> =
        apiCall {
            api.updateEvent(
                eventId,
                UpdateEventRequestDto(
                    name = input.name,
                    // The Json here is configured with explicitNulls = false, so a null property
                    // is dropped from the body entirely and the server would keep the old value.
                    // An empty string is the agreed signal for "clear this" — the backend's
                    // updateEventSchema normalizes it back to null.
                    description = input.description.orEmpty(),
                    location = input.location.orEmpty(),
                    price = input.price,
                    eventDate = input.eventDate,
                    eventTime = input.eventTime.orEmpty(),
                    imageUrl = input.imageUrl.orEmpty(),
                    active = active,
                ),
            )
        }.map { it.toDomain() }

    override suspend fun deleteEvent(eventId: String): AppResult<Unit> =
        when (val result = apiCall { api.deleteEvent(eventId) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }

    override suspend fun getEventRoster(eventId: String): AppResult<EventRoster> =
        apiCall { api.getEventRoster(eventId) }.map { it.toDomain() }

    override suspend fun getMyEventPayments(eventId: String): AppResult<MyEventPayments> =
        apiCall { api.getMyEventPayments(eventId) }.map { it.toDomain() }

    override suspend fun recordPayment(
        eventId: String,
        userId: String,
        amount: Double,
        note: String?,
    ): AppResult<EventPayment> =
        apiCall { api.recordEventPayment(eventId, RecordEventPaymentRequestDto(userId, amount, note)) }
            .map { it.toDomain() }

    override suspend fun setMemberTotal(eventId: String, userId: String, total: Double): AppResult<MyEventPayments> =
        apiCall { api.setMemberEventTotal(eventId, userId, SetMemberEventTotalRequestDto(total)) }
            .map { it.toDomain() }

    override suspend fun updatePayment(
        eventId: String,
        paymentId: String,
        amount: Double?,
        note: String?,
    ): AppResult<EventPayment> =
        apiCall { api.updateEventPayment(eventId, paymentId, UpdateEventPaymentRequestDto(amount, note)) }
            .map { it.toDomain() }

    override suspend fun deletePayment(eventId: String, paymentId: String): AppResult<Unit> =
        when (val result = apiCall { api.deleteEventPayment(eventId, paymentId) }) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> result
        }

    override suspend fun uploadImage(bytes: ByteArray, mimeType: String): AppResult<String> {
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", "upload.$extension", body)
        return apiCall { api.uploadImage(part) }.map { it.url }
    }
}
