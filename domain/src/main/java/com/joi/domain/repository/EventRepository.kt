package com.joi.domain.repository

import com.joi.domain.model.AppResult
import com.joi.domain.model.Event
import com.joi.domain.model.EventPayment
import com.joi.domain.model.EventRoster
import com.joi.domain.model.MyEventPayments

data class EventInput(
    val name: String,
    val description: String?,
    val location: String?,
    val price: Double,
    /** YYYY-MM-DD. */
    val eventDate: String,
    /** HH:MM, or null. */
    val eventTime: String? = null,
    val imageUrl: String? = null,
)

interface EventRepository {
    suspend fun listEvents(upcomingOnly: Boolean = true, activeOnly: Boolean = true): AppResult<List<Event>>
    suspend fun createEvent(input: EventInput): AppResult<Event>
    suspend fun updateEvent(eventId: String, input: EventInput, active: Boolean? = null): AppResult<Event>
    suspend fun deleteEvent(eventId: String): AppResult<Unit>

    /** Moderator only: everyone's standing on one event. */
    suspend fun getEventRoster(eventId: String): AppResult<EventRoster>
    /** The signed-in member's own installments and balance for one event. */
    suspend fun getMyEventPayments(eventId: String): AppResult<MyEventPayments>

    /** Records one installment against a member's balance. Negative amounts are refunds. */
    suspend fun recordPayment(eventId: String, userId: String, amount: Double, note: String?): AppResult<EventPayment>
    /** Sets a member's running total outright, whatever their ledger currently sums to. */
    suspend fun setMemberTotal(eventId: String, userId: String, total: Double): AppResult<MyEventPayments>
    suspend fun updatePayment(
        eventId: String,
        paymentId: String,
        amount: Double?,
        note: String?,
    ): AppResult<EventPayment>
    suspend fun deletePayment(eventId: String, paymentId: String): AppResult<Unit>

    /** Uploads a poster picked from the gallery and returns the hosted URL to use as the event's
     * imageUrl — the same endpoint prize images use. */
    suspend fun uploadImage(bytes: ByteArray, mimeType: String): AppResult<String>
}
