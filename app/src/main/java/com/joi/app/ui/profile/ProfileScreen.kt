package com.joi.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joi.app.di.AppContainer
import com.joi.app.util.viewModelFactoryOf
import com.joi.designsystem.components.ErrorState
import com.joi.designsystem.components.LevelBadge
import com.joi.designsystem.components.LoadingState
import com.joi.designsystem.components.PointsPill
import com.joi.designsystem.components.QrCodeImage
import com.joi.designsystem.components.WaveProgress
import com.joi.domain.model.Level
import com.joi.domain.model.PointType

@Composable
fun ProfileScreen(container: AppContainer) {
    val viewModel: ProfileViewModel = viewModel(
        factory = viewModelFactoryOf {
            ProfileViewModel(
                getMyProfileUseCase = container.getMyProfileUseCase,
                getMemberQrCodeUseCase = container.getMemberQrCodeUseCase,
                getMemberPointsHistoryUseCase = container.getMemberPointsHistoryUseCase,
                logoutUseCase = container.logoutUseCase,
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user = uiState.user

    Scaffold(
        topBar = {
            com.joi.designsystem.components.JoiTopBar(
                title = "My Profile",
                actions = {
                    IconButton(onClick = viewModel::signOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.loading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.errorMessage != null || user == null ->
                ErrorState(uiState.errorMessage ?: "Couldn't load your profile", modifier = Modifier.padding(padding), onRetry = viewModel::load)
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(user.fullName, style = MaterialTheme.typography.headlineMedium)
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            LevelBadge(level = user.level)
                            PointsPill(points = user.totalPoints)
                        }

                        val nextThreshold = nextLevelThreshold(user.level)
                        if (nextThreshold != null) {
                            val progress = (user.totalPoints.toFloat() / nextThreshold).coerceIn(0f, 1f)
                            Text(
                                "${nextThreshold - user.totalPoints} points to the next level",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                            )
                            WaveProgress(progress = progress)
                        }

                        uiState.qrPng?.let {
                            QrCodeImage(it, modifier = Modifier.padding(top = 24.dp))
                            Text(
                                "Show this at check-in",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }

                item {
                    Text(
                        "History",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (uiState.history.isEmpty()) {
                    item { Text("No point activity yet — attend a meeting to get started!", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(uiState.history, key = { it.id }) { tx ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "${if (tx.points >= 0) "+" else ""}${tx.points} pts — ${tx.type.label()}",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                val reason = tx.reason
                                if (!reason.isNullOrBlank()) {
                                    Text(reason, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    tx.createdAt,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun nextLevelThreshold(level: Level): Int? = when (level) {
    Level.BRONZE -> 100
    Level.SILVER -> 300
    Level.GOLD -> 600
    Level.DIAMOND -> null
}

private fun PointType.label(): String = when (this) {
    PointType.ATTENDANCE -> "Attendance"
    PointType.MANUAL_ADD -> "Bonus"
    PointType.MANUAL_REMOVE -> "Penalty"
    PointType.PRIZE_REDEEM -> "Prize redeemed"
}
