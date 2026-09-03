package com.joi.domain.repository

import com.joi.domain.model.Absentee
import com.joi.domain.model.AppResult
import com.joi.domain.model.CheckInResult
import com.joi.domain.model.RaffleNumberAssignment

interface AttendanceRepository {
    suspend fun checkIn(qrToken: String, meetingDate: String? = null): AppResult<CheckInResult>
    suspend fun getAbsentees(meetingDate: String? = null): AppResult<List<Absentee>>
    /** Hands one member a temporary draw number for the meeting. */
    suspend fun assignRaffleNumber(userId: String): AppResult<RaffleNumberAssignment>
    /** Every draw number currently in play, sorted, with no indication of who holds which —
     * the pool the lucky wheel spins over. */
    suspend fun listRaffleNumbers(): AppResult<List<Int>>
    /** Clears everyone's draw number; resolves to how many were cleared. */
    suspend fun resetRaffleNumbers(): AppResult<Int>
}
