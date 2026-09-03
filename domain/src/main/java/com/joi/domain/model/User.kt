package com.joi.domain.model

enum class Role { MODERATOR, MEMBER }

enum class Level { BRONZE, SILVER, GOLD, DIAMOND }

/**
 * The public (leaderboard-safe) shape of a person, as returned by most endpoints.
 * Matches the backend's `toPublicUser` shape exactly.
 */
data class PublicUser(
    val id: String,
    val fullName: String,
    val role: Role,
    val totalPoints: Int,
    val level: Level,
    val active: Boolean,
    /** Only present for the signed-in user's own profile and, for moderators, everyone's. */
    val username: String? = null,
    /** YYYY-MM-DD. Only present in the same cases as [username]. */
    val dateOfBirth: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    /** Sunday-school / age-group class. Only present in the same cases as [username]. */
    val className: String? = null,
    /** A moderator's private note about this member. Only present when a moderator is viewing
     * someone else — the backend never includes it in a member's own profile. */
    val note: String? = null,
    /** A temporary draw number handed out at check-in, for raffles and activities during the
     * meeting. Null once a moderator has reset the numbers — which is exactly how it disappears
     * from a member's profile again. */
    val raffleNumber: Int? = null,
    /** True only for the protected admin account (see the backend's UpdateUserUseCase) — the app
     * uses this to hide edit/deactivate/reset controls rather than let a moderator attempt an
     * action the server will refuse anyway. */
    val isProtected: Boolean = false,
)

/** The signed-in person's own session identity — a thin slice kept for quick UI access. */
data class CurrentUser(
    val id: String,
    val fullName: String,
    val role: Role,
)

/** Pure domain rule, mirrors the backend's `levelForPoints` — kept here too so the UI can
 * preview a level change locally (e.g. after a points adjustment) without waiting on a refetch. */
fun levelForPoints(totalPoints: Int): Level = when {
    totalPoints >= 600 -> Level.DIAMOND
    totalPoints >= 300 -> Level.GOLD
    totalPoints >= 100 -> Level.SILVER
    else -> Level.BRONZE
}
