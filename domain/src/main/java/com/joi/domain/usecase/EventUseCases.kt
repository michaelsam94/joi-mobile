package com.joi.domain.usecase

import com.joi.domain.model.AppError
import com.joi.domain.model.AppResult
import com.joi.domain.model.Event
import com.joi.domain.model.EventPayment
import com.joi.domain.model.EventRoster
import com.joi.domain.model.MyEventPayments
import com.joi.domain.repository.EventInput
import com.joi.domain.repository.EventRepository

private val DATE_PATTERN = Regex("""^\d{4}-\d{2}-\d{2}$""")
private val TIME_PATTERN = Regex("""^\d{2}:\d{2}$""")

class ListEventsUseCase(private val eventRepository: EventRepository) {
    suspend operator fun invoke(upcomingOnly: Boolean = true, activeOnly: Boolean = true): AppResult<List<Event>> =
        eventRepository.listEvents(upcomingOnly, activeOnly)
}

/** Creates or edits an event. The backend re-validates everything regardless — this is just fast
 * feedback so a typo doesn't cost a round-trip. */
class SaveEventUseCase(private val eventRepository: EventRepository) {
    suspend operator fun invoke(eventId: String?, input: EventInput): AppResult<Event> {
        if (input.name.isBlank()) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Event name is required"))
        }
        if (input.price < 0) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Price cannot be negative"))
        }
        if (!DATE_PATTERN.matches(input.eventDate)) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Pick a date for the event"))
        }
        val time = input.eventTime
        if (time != null && time.isNotBlank() && !TIME_PATTERN.matches(time)) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Time must look like 18:30"))
        }
        return if (eventId == null) {
            eventRepository.createEvent(input)
        } else {
            eventRepository.updateEvent(eventId, input)
        }
    }
}

class DeleteEventUseCase(private val eventRepository: EventRepository) {
    suspend operator fun invoke(eventId: String): AppResult<Unit> = eventRepository.deleteEvent(eventId)
}

/** Moderator only — the payment sheet for one event. */
class GetEventRosterUseCase(private val eventRepository: EventRepository) {
    suspend operator fun invoke(eventId: String): AppResult<EventRoster> = eventRepository.getEventRoster(eventId)
}

/** A member's own installments and balance for one event. */
class GetMyEventPaymentsUseCase(private val eventRepository: EventRepository) {
    suspend operator fun invoke(eventId: String): AppResult<MyEventPayments> =
        eventRepository.getMyEventPayments(eventId)
}

/**
 * Records one installment. Called once for a member paying the whole price at once, and again
 * each time a member paying in parts hands over more.
 */
class RecordEventPaymentUseCase(private val eventRepository: EventRepository) {
    suspend operator fun invoke(
        eventId: String,
        userId: String,
        amount: Double,
        note: String? = null,
    ): AppResult<EventPayment> {
        if (amount == 0.0) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Enter an amount"))
        }
        return eventRepository.recordPayment(eventId, userId, amount, note)
    }
}

/** Sets a member's total for an event to an exact figure — the "just make it say 250" action,
 * recorded as a balancing entry so the ledger keeps its history. */
class SetMemberEventTotalUseCase(private val eventRepository: EventRepository) {
    suspend operator fun invoke(eventId: String, userId: String, total: Double): AppResult<MyEventPayments> {
        if (total < 0) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Total paid cannot be negative"))
        }
        return eventRepository.setMemberTotal(eventId, userId, total)
    }
}

/** Corrects a single installment that was entered wrong. */
class UpdateEventPaymentUseCase(private val eventRepository: EventRepository) {
    suspend operator fun invoke(
        eventId: String,
        paymentId: String,
        amount: Double?,
        note: String? = null,
    ): AppResult<EventPayment> {
        if (amount != null && amount == 0.0) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Enter an amount"))
        }
        return eventRepository.updatePayment(eventId, paymentId, amount, note)
    }
}

class DeleteEventPaymentUseCase(private val eventRepository: EventRepository) {
    suspend operator fun invoke(eventId: String, paymentId: String): AppResult<Unit> =
        eventRepository.deletePayment(eventId, paymentId)
}

/** Uploads an event poster picked from the gallery. Mirrors the 5MB limit the backend enforces so
 * a too-large picture fails fast instead of after a slow doomed upload. */
class UploadEventImageUseCase(private val eventRepository: EventRepository) {
    suspend operator fun invoke(bytes: ByteArray, mimeType: String): AppResult<String> {
        if (bytes.isEmpty()) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "That image looks empty"))
        }
        if (bytes.size > 5 * 1024 * 1024) {
            return AppResult.Failure(AppError("VALIDATION_ERROR", "Image must be 5MB or smaller"))
        }
        return eventRepository.uploadImage(bytes, mimeType)
    }
}
