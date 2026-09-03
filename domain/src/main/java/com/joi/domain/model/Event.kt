package com.joi.domain.model

/**
 * An event a moderator organizes — a trip, a conference, a party. Everyone signed in can see the
 * upcoming ones; the `my*` fields are always about the person asking, so a member never learns
 * what anyone else has paid.
 */
data class Event(
    val id: String,
    val name: String,
    val description: String?,
    val location: String?,
    /** Price per person. 0 means free. */
    val price: Double,
    /** YYYY-MM-DD. */
    val eventDate: String,
    /** HH:MM, or null when the start time isn't fixed yet. */
    val eventTime: String?,
    val imageUrl: String?,
    val active: Boolean,
    val myPaidAmount: Double = 0.0,
    val myRemainingAmount: Double = 0.0,
    val myFullyPaid: Boolean = false,
)

/**
 * One installment. A member can settle an event's price in a single payment or across many, so
 * what they've paid is always the sum of these rather than one stored total.
 */
data class EventPayment(
    val id: String,
    val eventId: String,
    val userId: String,
    val amount: Double,
    val note: String?,
    val createdAt: String,
)

/** One member's line on a moderator's payment sheet. */
data class EventRosterEntry(
    val userId: String,
    val fullName: String,
    val paidAmount: Double,
    val remainingAmount: Double,
    val fullyPaid: Boolean,
    val payments: List<EventPayment>,
)

/** The moderator's whole payment sheet for one event. */
data class EventRoster(
    val event: Event,
    val entries: List<EventRosterEntry>,
    /** Everything collected so far, across all members. */
    val totalCollected: Double,
    /** What the event brings in if everyone on the sheet pays in full. */
    val totalExpected: Double,
)

/** What one member sees about their own money on one event. */
data class MyEventPayments(
    val eventId: String,
    val price: Double,
    val paidAmount: Double,
    val remainingAmount: Double,
    val fullyPaid: Boolean,
    val payments: List<EventPayment>,
)

/** Mirrors the backend's `paymentStanding` so the UI can show a corrected balance immediately
 * after an edit, without waiting on a refetch. */
fun remainingFor(price: Double, paidAmount: Double): Double = maxOf(0.0, roundMoney(price - paidAmount))

/** Money is carried as a Double; every value that reaches the UI or the wire goes through this so
 * float arithmetic can't leave 19.999999999999996 on screen. */
fun roundMoney(value: Double): Double = Math.round(value * 100.0) / 100.0
