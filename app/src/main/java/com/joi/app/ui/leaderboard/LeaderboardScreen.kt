package com.joi.app.ui.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joi.app.di.AppContainer
import com.joi.app.util.viewModelFactoryOf
import com.joi.designsystem.components.EmptyState
import com.joi.designsystem.components.ErrorState
import com.joi.designsystem.components.JoiTopBar
import com.joi.designsystem.components.LeaderboardRow
import com.joi.designsystem.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(container: AppContainer, currentUserId: String?) {
    val viewModel: LeaderboardViewModel = viewModel(
        factory = viewModelFactoryOf { LeaderboardViewModel(container.getLeaderboardUseCase, currentUserId) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { JoiTopBar(title = "Leaderboard") }) { padding ->
        when {
            uiState.loading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.errorMessage != null && uiState.entries.isEmpty() ->
                ErrorState(uiState.errorMessage!!, modifier = Modifier.padding(padding), onRetry = viewModel::refresh)
            else -> PullToRefreshBox(
                isRefreshing = uiState.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                if (uiState.entries.isEmpty()) {
                    EmptyState("No one's on the board yet — attend a meeting to get your first points!")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.entries, key = { it.userId }) { entry ->
                            LeaderboardRow(
                                rank = entry.rank,
                                fullName = entry.fullName,
                                totalPoints = entry.totalPoints,
                                level = entry.level,
                                isCurrentUser = entry.userId == currentUserId,
                            )
                        }
                    }
                }
            }
        }
    }
}
