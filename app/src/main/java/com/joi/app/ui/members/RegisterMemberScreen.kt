package com.joi.app.ui.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joi.app.di.AppContainer
import com.joi.app.util.viewModelFactoryOf
import com.joi.designsystem.components.JoiPrimaryButton
import com.joi.designsystem.components.JoiTopBar

@Composable
fun RegisterMemberScreen(container: AppContainer, onBack: () -> Unit, onRegistered: () -> Unit) {
    val viewModel: RegisterMemberViewModel = viewModel(
        factory = viewModelFactoryOf { RegisterMemberViewModel(container.registerMemberUseCase) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.done) {
        if (uiState.done) onRegistered()
    }

    Scaffold(topBar = { JoiTopBar(title = "Register new person", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                "They'll sign in with this username and temporary password, then be asked to " +
                    "set their own on first login — same as the app's rule everywhere else.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            OutlinedTextField(
                value = uiState.fullName,
                onValueChange = viewModel::onFullNameChange,
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            OutlinedTextField(
                value = uiState.temporaryPassword,
                onValueChange = viewModel::onTemporaryPasswordChange,
                label = { Text("Temporary password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = uiState.asModerator, onCheckedChange = viewModel::onAsModeratorChange)
                Text("Register as a moderator (can edit points, prizes, members)")
            }

            if (uiState.errorMessage != null) {
                Text(
                    uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            JoiPrimaryButton(
                text = "Register",
                onClick = viewModel::register,
                loading = uiState.loading,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}
