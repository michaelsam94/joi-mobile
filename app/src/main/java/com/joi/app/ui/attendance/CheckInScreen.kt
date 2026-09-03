package com.joi.app.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.joi.app.di.AppContainer
import com.joi.app.util.viewModelFactoryOf
import com.joi.designsystem.components.JoiPrimaryButton
import com.joi.designsystem.components.JoiSecondaryButton
import com.joi.designsystem.components.JoiTopBar

/**
 * Moderator-only. "Scan QR" launches Google Play services' modal Code Scanner — no CAMERA
 * permission or preview UI to build ourselves, it's a self-contained system sheet. The manual
 * entry field underneath is the fallback for devices without Play services, a damaged/unreadable
 * code, or just testing against a QR you don't have printed yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(container: AppContainer, onOpenAbsentees: () -> Unit, onOpenLuckyWheel: () -> Unit) {
    val context = LocalContext.current
    val scanner = remember { GmsBarcodeScanning.getClient(context) }

    val viewModel: CheckInViewModel = viewModel(
        factory = viewModelFactoryOf {
            CheckInViewModel(
                checkInUseCase = container.checkInUseCase,
                assignRaffleNumberUseCase = container.assignRaffleNumberUseCase,
                resetRaffleNumbersUseCase = container.resetRaffleNumbersUseCase,
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            JoiTopBar(
                title = "Check In",
                actions = {
                    IconButton(onClick = onOpenLuckyWheel) {
                        Icon(Icons.Default.Casino, contentDescription = "Lucky wheel")
                    }
                    IconButton(onClick = viewModel::askResetNumbers) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset draw numbers")
                    }
                    IconButton(onClick = onOpenAbsentees) {
                        Icon(Icons.Default.List, contentDescription = "Absentees")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("📷", style = MaterialTheme.typography.displayLarge)
            Text(
                "Scan a member's QR code to check them in",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
            )

            JoiPrimaryButton(
                text = "Scan QR code",
                onClick = {
                    scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            viewModel.checkIn(barcode.rawValue.orEmpty())
                        }
                        .addOnFailureListener { /* user-visible errorMessage stays null; they can just retry or use manual entry */ }
                },
            )
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.padding(top = 4.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text(
                "Or enter a QR token manually",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = uiState.manualToken,
                onValueChange = viewModel::onManualTokenChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("QR token") },
            )
            JoiSecondaryButton(
                text = "Check in",
                onClick = { viewModel.checkIn(uiState.manualToken) },
                enabled = uiState.manualToken.isNotBlank() && !uiState.loading,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (uiState.errorMessage != null) {
                Text(
                    uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    val result = uiState.lastResult
    if (result != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            title = { Text("✅ ${result.fullName} checked in!") },
            text = {
                Column {
                    Text(
                        "+${result.pointsAwarded} points for ${result.meetingDate}\n" +
                            "Now at ${result.totalPoints} points total.",
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Entirely optional: a meeting with no raffle just never taps this, and
                    // nobody gets a number.
                    val number = uiState.assignedNumber
                    if (number != null) {
                        Text(
                            "Draw number",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "#$number",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "They'll see this on their profile until you reset the numbers.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        OutlinedButton(
                            onClick = viewModel::assignRaffleNumber,
                            enabled = !uiState.assigningNumber,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (uiState.assigningNumber) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("🎲 Give a draw number")
                            }
                        }
                    }
                    if (uiState.numberError != null) {
                        Text(
                            uiState.numberError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissResult) { Text("Nice") }
            },
        )
    }

    if (uiState.showResetConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResetConfirm,
            title = { Text("Reset all draw numbers?") },
            text = {
                Text(
                    "Everyone's number is cleared and disappears from their profile. " +
                        "Do this once the raffle or activity is finished.",
                )
            },
            confirmButton = {
                TextButton(enabled = !uiState.resetting, onClick = viewModel::resetRaffleNumbers) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(enabled = !uiState.resetting, onClick = viewModel::dismissResetConfirm) { Text("Cancel") }
            },
        )
    }

    val cleared = uiState.resetCleared
    if (cleared != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResetResult,
            title = { Text("Numbers reset") },
            text = {
                Text(
                    if (cleared == 0) {
                        "Nobody had a number to clear."
                    } else {
                        "Cleared $cleared number${if (cleared == 1) "" else "s"}."
                    },
                )
            },
            confirmButton = { TextButton(onClick = viewModel::dismissResetResult) { Text("OK") } },
        )
    }
}
