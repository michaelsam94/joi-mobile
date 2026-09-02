package com.joi.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.joi.app.AppConfig
import com.joi.data.network.JoiApiService
import com.joi.data.network.NetworkModule
import com.joi.data.repository.AttendanceRepositoryImpl
import com.joi.data.repository.AuthRepositoryImpl
import com.joi.data.repository.LeaderboardRepositoryImpl
import com.joi.data.repository.PointsRepositoryImpl
import com.joi.data.repository.PrizeRepositoryImpl
import com.joi.data.repository.TelegramRepositoryImpl
import com.joi.data.repository.UserRepositoryImpl
import com.joi.data.session.DataStoreAuthSession
import com.joi.domain.repository.AttendanceRepository
import com.joi.domain.repository.AuthRepository
import com.joi.domain.repository.LeaderboardRepository
import com.joi.domain.repository.PointsRepository
import com.joi.domain.repository.PrizeRepository
import com.joi.domain.repository.TelegramRepository
import com.joi.domain.repository.UserRepository
import com.joi.domain.session.AuthSession
import com.joi.domain.usecase.AdjustPointsUseCase
import com.joi.domain.usecase.ChangePasswordUseCase
import com.joi.domain.usecase.CheckInUseCase
import com.joi.domain.usecase.DeletePrizeUseCase
import com.joi.domain.usecase.GetAbsenteesUseCase
import com.joi.domain.usecase.GetLeaderboardUseCase
import com.joi.domain.usecase.GetMemberPointsHistoryUseCase
import com.joi.domain.usecase.GetMemberQrCodeUseCase
import com.joi.domain.usecase.GetMemberUseCase
import com.joi.domain.usecase.GetMyProfileUseCase
import com.joi.domain.usecase.ListMembersUseCase
import com.joi.domain.usecase.ListPrizesUseCase
import com.joi.domain.usecase.LoginUseCase
import com.joi.domain.usecase.LogoutUseCase
import com.joi.domain.usecase.RedeemPrizeUseCase
import com.joi.domain.usecase.RegisterMemberUseCase
import com.joi.domain.usecase.SavePrizeUseCase
import com.joi.domain.usecase.SendWeeklyReportNowUseCase
import com.joi.domain.usecase.SetMemberActiveUseCase
import com.joi.domain.usecase.UpdateMemberUseCase

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "joi_session")

/**
 * The composition root for the whole app — the one place that `new`s every concrete
 * implementation and wires it into a use-case, exactly the same role `config/container.ts` plays
 * in the backend. Every ViewModel receives only the use-cases it needs from here; nothing outside
 * this file and `data`/`domain` ever references a concrete repository class.
 *
 * Deliberately a plain hand-written container rather than a DI framework (Hilt/Koin) — one less
 * thing that could be misconfigured in code nobody has compiled yet. Swapping to Hilt later is a
 * mechanical change once this is confirmed working.
 */
class AppContainer(context: Context) {

    val authSession: AuthSession = DataStoreAuthSession(context.applicationContext.dataStore)

    private val api: JoiApiService = NetworkModule.buildApiService(
        baseUrl = AppConfig.BASE_URL,
        session = authSession,
        debugLogging = AppConfig.DEBUG_NETWORK_LOGGING,
    )

    private val authRepository: AuthRepository = AuthRepositoryImpl(api)
    private val userRepository: UserRepository = UserRepositoryImpl(api)
    private val attendanceRepository: AttendanceRepository = AttendanceRepositoryImpl(api)
    private val pointsRepository: PointsRepository = PointsRepositoryImpl(api)
    private val leaderboardRepository: LeaderboardRepository = LeaderboardRepositoryImpl(api)
    private val prizeRepository: PrizeRepository = PrizeRepositoryImpl(api)
    private val telegramRepository: TelegramRepository = TelegramRepositoryImpl(api)

    // Auth
    val loginUseCase = LoginUseCase(authRepository, authSession)
    val changePasswordUseCase = ChangePasswordUseCase(authRepository, authSession)
    val logoutUseCase = LogoutUseCase(authSession)

    // Members
    val listMembersUseCase = ListMembersUseCase(userRepository)
    val registerMemberUseCase = RegisterMemberUseCase(userRepository)
    val getMemberQrCodeUseCase = GetMemberQrCodeUseCase(userRepository)
    val getMemberUseCase = GetMemberUseCase(userRepository)
    val getMemberPointsHistoryUseCase = GetMemberPointsHistoryUseCase(userRepository)
    val setMemberActiveUseCase = SetMemberActiveUseCase(userRepository)
    val updateMemberUseCase = UpdateMemberUseCase(userRepository)
    val getMyProfileUseCase = GetMyProfileUseCase(userRepository)

    // Attendance
    val checkInUseCase = CheckInUseCase(attendanceRepository)
    val getAbsenteesUseCase = GetAbsenteesUseCase(attendanceRepository)
    val sendWeeklyReportNowUseCase = SendWeeklyReportNowUseCase(telegramRepository)

    // Points & leaderboard
    val adjustPointsUseCase = AdjustPointsUseCase(pointsRepository)
    val getLeaderboardUseCase = GetLeaderboardUseCase(leaderboardRepository)

    // Prizes
    val listPrizesUseCase = ListPrizesUseCase(prizeRepository)
    val savePrizeUseCase = SavePrizeUseCase(prizeRepository)
    val deletePrizeUseCase = DeletePrizeUseCase(prizeRepository)
    val redeemPrizeUseCase = RedeemPrizeUseCase(prizeRepository)
}
