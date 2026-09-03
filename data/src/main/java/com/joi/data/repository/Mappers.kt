package com.joi.data.repository

import com.joi.data.network.dto.*
import com.joi.domain.model.*

// Small, explicit DTO -> domain mapping functions, kept in one file since none of them carry
// real logic (that lives in the use-cases) — this is purely "wire shape" -> "domain shape".

internal fun String.toRole(): Role = if (this == "MODERATOR") Role.MODERATOR else Role.MEMBER
internal fun String.toLevel(): Level = when (this) {
    "Diamond" -> Level.DIAMOND
    "Gold" -> Level.GOLD
    "Silver" -> Level.SILVER
    else -> Level.BRONZE
}
internal fun String.toPointType(): PointType = when (this) {
    "ATTENDANCE" -> PointType.ATTENDANCE
    "MANUAL_ADD" -> PointType.MANUAL_ADD
    "MANUAL_REMOVE" -> PointType.MANUAL_REMOVE
    else -> PointType.PRIZE_REDEEM
}

internal fun PublicUserDto.toDomain() = PublicUser(
    id = id,
    fullName = fullName,
    role = role.toRole(),
    totalPoints = totalPoints,
    level = level.toLevel(),
    active = active,
    username = username,
    dateOfBirth = dateOfBirth,
    phoneNumber = phoneNumber,
    address = address,
    className = className,
    note = note,
)

internal fun CurrentUserDto.toDomain() = CurrentUser(id = id, fullName = fullName, role = role.toRole())

internal fun PointTransactionDto.toDomain() = PointTransaction(
    id = id,
    points = points,
    type = type.toPointType(),
    reason = reason,
    createdAt = createdAt,
)

internal fun LeaderboardEntryDto.toDomain() = LeaderboardEntry(
    rank = rank,
    userId = userId,
    fullName = fullName,
    totalPoints = totalPoints,
    level = level.toLevel(),
)

internal fun PrizeDto.toDomain() = Prize(
    id = id,
    name = name,
    description = description,
    pointsCost = pointsCost,
    imageUrl = imageUrl,
    active = active,
    quantity = quantity,
)

internal fun PrizeRedemptionDto.toDomain() = PrizeRedemption(
    id = id,
    prizeId = prizeId,
    userId = userId,
    pointsSpent = pointsSpent,
    createdAt = createdAt,
)

internal fun AbsenteeDto.toDomain() = Absentee(
    userId = userId,
    fullName = fullName,
    totalHistoricalAttendance = totalHistoricalAttendance,
)

internal fun CheckInResponseDto.toDomain() = CheckInResult(
    userId = userId,
    fullName = fullName,
    meetingDate = meetingDate,
    pointsAwarded = pointsAwarded,
    totalPoints = totalPoints,
)

internal fun EventDto.toDomain() = Event(
    id = id,
    name = name,
    description = description,
    location = location,
    price = price,
    eventDate = eventDate,
    eventTime = eventTime,
    imageUrl = imageUrl,
    active = active,
    myPaidAmount = myPaidAmount,
    myRemainingAmount = myRemainingAmount,
    myFullyPaid = myFullyPaid,
)

internal fun EventPaymentDto.toDomain() = EventPayment(
    id = id,
    eventId = eventId,
    userId = userId,
    amount = amount,
    note = note,
    createdAt = createdAt,
)

internal fun EventRosterEntryDto.toDomain() = EventRosterEntry(
    userId = userId,
    fullName = fullName,
    paidAmount = paidAmount,
    remainingAmount = remainingAmount,
    fullyPaid = fullyPaid,
    payments = payments.map { it.toDomain() },
)

internal fun EventRosterDto.toDomain() = EventRoster(
    event = event.toDomain(),
    entries = entries.map { it.toDomain() },
    totalCollected = totalCollected,
    totalExpected = totalExpected,
)

internal fun MyEventPaymentsDto.toDomain() = MyEventPayments(
    eventId = eventId,
    price = price,
    paidAmount = paidAmount,
    remainingAmount = remainingAmount,
    fullyPaid = fullyPaid,
    payments = payments.map { it.toDomain() },
)
