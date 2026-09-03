package com.joi.domain.model

data class CheckInResult(
    val userId: String,
    val fullName: String,
    val meetingDate: String,
    val pointsAwarded: Int,
    val totalPoints: Int,
)

/** The outcome of handing someone a temporary draw number from the check-in popup. */
data class RaffleNumberAssignment(
    val userId: String,
    val fullName: String,
    val raffleNumber: Int,
    /** True when they already held this number and it was handed back rather than redrawn. */
    val alreadyHeld: Boolean,
)

data class Absentee(
    val userId: String,
    val fullName: String,
    val totalHistoricalAttendance: Int,
)

data class WeeklyReportResult(
    val meetingDate: String,
    val attendedCount: Int,
    val totalActiveMembers: Int,
    val absentees: List<Absentee>,
    val sentToChatIds: List<String>,
    val failedChatIds: List<String> = emptyList(),
)
