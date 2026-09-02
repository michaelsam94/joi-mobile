package com.joi.domain.model

data class CheckInResult(
    val userId: String,
    val fullName: String,
    val meetingDate: String,
    val pointsAwarded: Int,
    val totalPoints: Int,
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
)
