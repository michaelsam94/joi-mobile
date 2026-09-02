package com.joi.app.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.joi.designsystem.components.JoiPrimaryButton
import com.joi.designsystem.components.JoiTopBar
import com.joi.designsystem.components.LoadingState

/** Who missed this week's meeting, and how many they've attended all-time — the same data the Friday Telegram report sends. */
@Composable
fun AbsenteesScreen(container: AppContainer, onBack: () -> Unit) {
    val viewModel: AbsenteesViewModel = viewModel(
        factory = viewModelFactoryOf {
            AbsenteesViewModel(container.getAbsenteesUseCase, container.sendWeeklyReportNowUseCase)
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { JoiTopBar(title = "Absent this week", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.loading -> LoadingState(modifier = Modifier.weight(1f))
                uiState.errorMessage != null ->
                    ErrorState(uiState.errorMessage!!, modifier = Modifier.weight(1f), onRetry = viewModel::load)
                uiState.absentees.isEmpty() ->
                    EmptyState("Everyone showed up this week! 🎉", modifier = Modifier.weight(1f))
                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.absentees, key = { it.userId }) { absentee ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(absentee.fullName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Attended ${absentee.totalHistoricalAttendance} meeting(s) all-time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            JoiPrimaryButton(
                text = "Send weekly report to Telegram now",
                onClick = viewModel::sendWeeklyReportNow,
                loading = uiState.sendingReport,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    if (uiState.reportSentMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissReportMessage,
            title = { Text("Weekly report") },
            text = { Text(uiState.reportSentMessage!!) },
            confirmButton = { TextButton(onClick = viewModel::dismissReportMessage) { Text("OK") } },
        )
    }
}
