package com.joi.app.navigation

/** Route constants for the post-login NavHost (auth itself isn't a NavHost route — see JoiNavHost). */
object Destinations {
    const val LEADERBOARD = "leaderboard"
    const val PRIZES = "prizes"
    const val PROFILE = "profile"
    const val SCAN = "scan"
    const val ABSENTEES = "absentees"
    const val MEMBERS = "members"
    const val REGISTER_MEMBER = "register_member"

    const val MEMBER_DETAIL_ROUTE = "member_detail/{userId}"
    fun memberDetail(userId: String) = "member_detail/$userId"
    const val MEMBER_DETAIL_ARG = "userId"
}
