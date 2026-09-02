package com.joi.app.ui.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joi.app.di.AppContainer
import com.joi.app.util.viewModelFactoryOf
import com.joi.designsystem.components.ErrorState
import com.joi.designsystem.components.JoiPrimaryButton
import com.joi.designsystem.components.JoiSecondaryButton
import com.joi.designsystem.components.JoiTopBar
import com.joi.designsystem.components.LevelBadge
import com.joi.designsystem.components.LoadingState
import com.joi.designsystem.components.PointsPill
import com.joi.designsystem.components.QrCodeImage
import com.joi.domain.model.PointType

@Composable
fun MemberDetailScreen(container: AppContainer, userId: String, onBack: () -> Unit) {
    val viewModel: MemberDetailViewModel = viewModel(
        factory = viewModelFactoryOf {
            MemberDetailViewModel(
                userId = userId,
                getMemberUseCase = container.getMemberUseCase,
                getMemberQrCodeUseCase = container.getMemberQrCodeUseCase,
                getMemberPointsHistoryUseCase = container.getMemberPointsHistoryUseCase,
                setMemberActiveUseCase = container.setMemberActiveUseCase,
                adjustPointsUseCase = container.adjustPointsUseCase,
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user = uiState.user

    Scaffold(topBar = { JoiTopBar(title = user?.fullName ?: "Member", onBack = onBack) }) { padding ->
        when {
            uiState.loading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.errorMessage != null || user == null ->
                ErrorState(uiState.errorMessage ?: "Member not found", modifier = Modifier.padding(padding), onRetry = viewModel::load)
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        uiState.qrPng?.let { QrCodeImage(it) } ?: LoadingState(modifier = Modifier.fillMaxWidth())
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            LevelBadge(level = user.level)
                            PointsPill(points = user.totalPoints)
                        }
                    }
                }

                item {
                    JoiPrimaryButton(text = "Add / remove points", onClick = viewModel::openAdjustDialog)
                }
                item {
                    JoiSecondaryButton(
                        text = if (user.active) "Deactivate" else "Reactivate",
                        onClick = viewModel::toggleActive,
                    )
                }

                item {
                    Text("Points history", style = MaterialTheme.typography.titleMedium)
                }
                if (uiState.history.isEmpty()) {
                    item { Text("No point activity yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(uiState.history, key = { it.id }) { tx ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "${if (tx.points >= 0) "+" else ""}${tx.points} pts — ${tx.type.label()}",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                if (!tx.reason.isNullOrBlank()) {
                                    Text(tx.reason, style = MaterialTheme.typography.bodyMedium)
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

    if (uiState.showAdjustDialog) {
        AdjustPointsDialog(
            loading = uiState.adjustLoading,
            errorMessage = uiState.adjustError,
            onDismiss = viewModel::dismissAdjustDialog,
            onConfirm = viewModel::adjustPoints,
        )
    }
}

private fun PointType.label(): String = when (this) {
    PointType.ATTENDANCE -> "Attendance"
    PointType.MANUAL_ADD -> "Bonus"
    PointType.MANUAL_REMOVE -> "Penalty"
    PointType.PRIZE_REDEEM -> "Prize redeemed"
}

@Composable
private fun AdjustPointsDialog(
    loading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (points: Int, reason: String) -> Unit,
) {
    var pointsText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var isAdd by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust points") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isAdd, onClick = { isAdd = true }, label = { Text("➕ Add") })
                    FilterChip(selected = !isAdd, onClick = { isAdd = false }, label = { Text("➖ Remove") })
                }
                OutlinedTextField(
                    value = pointsText,
                    onValueChange = { pointsText = it.filter { c -> c.isDigit() } },
                    label = { Text("Points") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading && pointsText.toIntOrNull() != null && pointsText.toInt() > 0,
                onClick = {
                    val magnitude = pointsText.toIntOrNull() ?: return@TextButton
                    onConfirm(if (isAdd) magnitude else -magnitude, reason)
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
