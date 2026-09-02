package com.joi.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.joi.app.di.AppContainer
import com.joi.app.ui.attendance.AbsenteesScreen
import com.joi.app.ui.attendance.CheckInScreen
import com.joi.app.ui.auth.ChangePasswordScreen
import com.joi.app.ui.auth.LoginScreen
import com.joi.app.ui.leaderboard.LeaderboardScreen
import com.joi.app.ui.members.MemberDetailScreen
import com.joi.app.ui.members.MembersScreen
import com.joi.app.ui.members.RegisterMemberScreen
import com.joi.app.ui.prizes.PrizesScreen
import com.joi.app.ui.profile.ProfileScreen
import com.joi.designsystem.components.LoadingState
import com.joi.domain.model.Role
import com.joi.domain.session.SessionState

/**
 * The single root composable. Reacts to the session's Flow<SessionState> to decide, at the top
 * level, which of three modes to show — signed out, "must set a new password", or the real app —
 * rather than driving that transition through explicit `navController.navigate()` calls. Once
 * signed in, `MainScaffold` below owns its own NavHost for the role-appropriate tabs/screens.
 */
@Composable
fun JoiNavHost(container: AppContainer) {
    val sessionState by container.authSession.state.collectAsStateWithLifecycle(initialValue = null)

    when (val state = sessionState) {
        null -> LoadingState(modifier = Modifier.fillMaxSize())
        else -> if (!state.isSignedIn) {
            LoginScreen(container)
        } else if (state.mustChangePassword) {
            ChangePasswordScreen(container)
        } else {
            MainScaffold(container = container, session = state)
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun MainScaffold(container: AppContainer, session: SessionState) {
    val navController = rememberNavController()
    val isModerator = session.role == Role.MODERATOR

    val tabs = buildList {
        add(Tab(Destinations.LEADERBOARD, "Leaderboard", Icons.Default.EmojiEvents))
        if (isModerator) add(Tab(Destinations.SCAN, "Scan", Icons.Default.QrCodeScanner))
        if (isModerator) add(Tab(Destinations.MEMBERS, "Members", Icons.Default.Group))
        add(Tab(Destinations.PRIZES, "Prizes", Icons.Default.Redeem))
        add(Tab(Destinations.PROFILE, "Profile", Icons.Default.Person))
    }

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.LEADERBOARD,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Destinations.LEADERBOARD) {
                LeaderboardScreen(container, currentUserId = session.userId)
            }
            composable(Destinations.PRIZES) {
                PrizesScreen(container, isModerator = isModerator)
            }
            composable(Destinations.PROFILE) {
                ProfileScreen(container)
            }

            if (isModerator) {
                composable(Destinations.SCAN) {
                    CheckInScreen(container, onOpenAbsentees = { navController.navigate(Destinations.ABSENTEES) })
                }
                composable(Destinations.ABSENTEES) {
                    AbsenteesScreen(container, onBack = { navController.popBackStack() })
                }
                composable(Destinations.MEMBERS) {
                    MembersScreen(
                        container,
                        onOpenMember = { userId -> navController.navigate(Destinations.memberDetail(userId)) },
                        onRegisterMember = { navController.navigate(Destinations.REGISTER_MEMBER) },
                    )
                }
                composable(Destinations.REGISTER_MEMBER) {
                    RegisterMemberScreen(
                        container,
                        onBack = { navController.popBackStack() },
                        onRegistered = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Destinations.MEMBER_DETAIL_ROUTE,
                    arguments = listOf(navArgument(Destinations.MEMBER_DETAIL_ARG) { type = NavType.StringType }),
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString(Destinations.MEMBER_DETAIL_ARG).orEmpty()
                    MemberDetailScreen(container, userId = userId, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
