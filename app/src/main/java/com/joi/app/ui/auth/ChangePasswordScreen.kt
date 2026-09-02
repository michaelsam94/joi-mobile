package com.joi.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joi.app.di.AppContainer
import com.joi.app.util.viewModelFactoryOf
import com.joi.designsystem.components.JoiPasswordField
import com.joi.designsystem.components.JoiPrimaryButton

/**
 * Shown right after a first login (mustChangePassword == true) or an admin-issued temporary
 * password — enforced client-side to match the backend rule, but the backend still rejects a
 * short password too, so this is convenience, not the source of truth.
 */
@Composable
fun ChangePasswordScreen(container: AppContainer) {
    val viewModel: ChangePasswordViewModel = viewModel(
        factory = viewModelFactoryOf { ChangePasswordViewModel(container.changePasswordUseCase, container.logoutUseCase) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🔐", style = MaterialTheme.typography.displayLarge)
            Text(
                "Set your own password",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "You're using a temporary password — choose a new one to continue.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            JoiPasswordField(
                value = uiState.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = "New password",
                modifier = Modifier.fillMaxWidth(),
            )
            JoiPasswordField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = "Confirm password",
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            if (uiState.errorMessage != null) {
                Text(
                    uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            JoiPrimaryButton(
                text = "Save and continue",
                onClick = viewModel::submit,
                loading = uiState.loading,
                modifier = Modifier.padding(top = 20.dp),
            )

            TextButton(onClick = viewModel::signOut, modifier = Modifier.padding(top = 8.dp)) {
                Text("Sign in with a different account")
            }
        }
    }
}
