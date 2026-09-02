package com.joi.app.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
fun CheckInScreen(container: AppContainer, onOpenAbsentees: () -> Unit) {
    val context = LocalContext.current
    val scanner = remember { GmsBarcodeScanning.getClient(context) }

    val viewModel: CheckInViewModel = viewModel(
        factory = viewModelFactoryOf { CheckInViewModel(container.checkInUseCase) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            JoiTopBar(
                title = "Check In",
                actions = {
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
                Text(
                    "+${result.pointsAwarded} points for ${result.meetingDate}\n" +
                        "Now at ${result.totalPoints} points total.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissResult) { Text("Nice") }
            },
        )
    }
}
