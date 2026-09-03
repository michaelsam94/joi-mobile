package com.joi.data.network

import com.joi.data.network.dto.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/** One-to-one with joi-backend's Express routes — see joi-backend/README.md's API summary table. */
interface JoiApiService {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<LoginResponseDto>

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequestDto): Response<OkResponseDto>

    @POST("users")
    suspend fun registerUser(@Body body: RegisterUserRequestDto): Response<PublicUserDto>

    @GET("users")
    suspend fun listUsers(@Query("activeOnly") activeOnly: Boolean = true): Response<List<PublicUserDto>>

    @GET("users/me")
    suspend fun me(): Response<PublicUserDto>

    @PATCH("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body body: UpdateUserRequestDto): Response<PublicUserDto>

    @GET("users/{id}/qr")
    suspend fun getQrCode(@Path("id") id: String): Response<ResponseBody>

    @GET("users/{id}/points/history")
    suspend fun getPointsHistory(@Path("id") id: String): Response<List<PointTransactionDto>>

    @POST("attendance/check-in")
    suspend fun checkIn(@Body body: CheckInRequestDto): Response<CheckInResponseDto>

    @POST("attendance/raffle-number")
    suspend fun assignRaffleNumber(@Body body: AssignRaffleNumberRequestDto): Response<RaffleNumberAssignmentDto>

    @POST("attendance/raffle-number/reset")
    suspend fun resetRaffleNumbers(): Response<ResetRaffleNumbersResponseDto>

    @GET("attendance/absentees")
    suspend fun getAbsentees(@Query("meetingDate") meetingDate: String? = null): Response<AbsenteesResponseDto>

    @POST("points/adjust")
    suspend fun adjustPoints(@Body body: AdjustPointsRequestDto): Response<PublicUserDto>

    @GET("leaderboard")
    suspend fun getLeaderboard(): Response<List<LeaderboardEntryDto>>

    @GET("prizes")
    suspend fun listPrizes(@Query("activeOnly") activeOnly: Boolean = true): Response<List<PrizeDto>>

    @POST("prizes")
    suspend fun createPrize(@Body body: CreatePrizeRequestDto): Response<PrizeDto>

    @PATCH("prizes/{id}")
    suspend fun updatePrize(@Path("id") id: String, @Body body: UpdatePrizeRequestDto): Response<PrizeDto>

    @DELETE("prizes/{id}")
    suspend fun deletePrize(@Path("id") id: String): Response<Unit>

    @POST("prizes/{id}/redeem")
    suspend fun redeemPrize(@Path("id") id: String, @Body body: RedeemPrizeRequestDto): Response<PrizeRedemptionDto>

    @GET("prizes/redeemed-by-me")
    suspend fun getRedeemedPrizeIds(): Response<RedeemedPrizeIdsResponseDto>

    @GET("events")
    suspend fun listEvents(
        @Query("upcomingOnly") upcomingOnly: Boolean = true,
        @Query("activeOnly") activeOnly: Boolean = true,
    ): Response<List<EventDto>>

    @POST("events")
    suspend fun createEvent(@Body body: CreateEventRequestDto): Response<EventDto>

    @PATCH("events/{id}")
    suspend fun updateEvent(@Path("id") id: String, @Body body: UpdateEventRequestDto): Response<EventDto>

    @DELETE("events/{id}")
    suspend fun deleteEvent(@Path("id") id: String): Response<Unit>

    @GET("events/{id}/payments")
    suspend fun getEventRoster(@Path("id") id: String): Response<EventRosterDto>

    @GET("events/{id}/payments/me")
    suspend fun getMyEventPayments(@Path("id") id: String): Response<MyEventPaymentsDto>

    @POST("events/{id}/payments")
    suspend fun recordEventPayment(
        @Path("id") id: String,
        @Body body: RecordEventPaymentRequestDto,
    ): Response<EventPaymentDto>

    @PUT("events/{id}/payments/member/{userId}")
    suspend fun setMemberEventTotal(
        @Path("id") id: String,
        @Path("userId") userId: String,
        @Body body: SetMemberEventTotalRequestDto,
    ): Response<MyEventPaymentsDto>

    @PATCH("events/{id}/payments/{paymentId}")
    suspend fun updateEventPayment(
        @Path("id") id: String,
        @Path("paymentId") paymentId: String,
        @Body body: UpdateEventPaymentRequestDto,
    ): Response<EventPaymentDto>

    @DELETE("events/{id}/payments/{paymentId}")
    suspend fun deleteEventPayment(@Path("id") id: String, @Path("paymentId") paymentId: String): Response<Unit>

    @POST("telegram/send-weekly-report")
    suspend fun sendWeeklyReport(): Response<WeeklyReportResponseDto>

    @POST("export/database")
    suspend fun exportDatabase(): Response<ExportUrlResponseDto>

    @Multipart
    @POST("uploads/image")
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<UploadImageResponseDto>
}
