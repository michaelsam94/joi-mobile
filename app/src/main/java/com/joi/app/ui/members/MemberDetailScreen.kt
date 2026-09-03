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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.joi.domain.model.Role

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
                updateMemberUseCase = container.updateMemberUseCase,
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user = uiState.user

    Scaffold(
        topBar = {
            JoiTopBar(
                title = user?.fullName ?: "Member",
                onBack = onBack,
                actions = {
                    // A protected account (the seeded admin) can't be edited at all — the server
                    // refuses every field change, so there's no edit action to offer here.
                    if (user != null && !user.isProtected) {
                        IconButton(onClick = viewModel::openEditDialog) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit member")
                        }
                    }
                },
            )
        },
    ) { padding ->
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
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Profile", style = MaterialTheme.typography.titleMedium)
                            ProfileField("Username", user.username)
                            ProfileField("Date of birth", user.dateOfBirth)
                            ProfileField("Phone number", user.phoneNumber)
                            ProfileField("Address", user.address)
                            ProfileField("Class", user.className)
                            ProfileField("Note", user.note)
                        }
                    }
                }

                if (user.isProtected) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "🔒 Protected account — it can't be edited, deactivated, or reset.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }

                item {
                    JoiPrimaryButton(text = "Add / remove points", onClick = viewModel::openAdjustDialog)
                }
                // Points can still be adjusted for a protected account (that's audit trail, not
                // account data) — only deactivating/editing/resetting are blocked.
                if (!user.isProtected) {
                    item {
                        JoiSecondaryButton(
                            text = if (user.active) "Deactivate" else "Reactivate",
                            onClick = viewModel::toggleActive,
                        )
                    }
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

    if (uiState.showAdjustDialog) {
        AdjustPointsDialog(
            loading = uiState.adjustLoading,
            errorMessage = uiState.adjustError,
            onDismiss = viewModel::dismissAdjustDialog,
            onConfirm = viewModel::adjustPoints,
        )
    }

    if (uiState.showEditDialog && user != null) {
        EditMemberDialog(
            user = user,
            loading = uiState.editLoading,
            errorMessage = uiState.editError,
            onDismiss = viewModel::dismissEditDialog,
            onConfirm = viewModel::updateMember,
        )
    }
}

@Composable
private fun ProfileField(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Keeps the date-of-birth field to exactly YYYY-MM-DD: strips non-digits, caps at 8 digits, and
 * auto-inserts the two dashes as the person types — so the field can never end up in a shape the
 * backend's `dateOfBirthSchema` would reject. */
private fun formatDateOfBirthInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(8)
    return buildString {
        for (i in digits.indices) {
            if (i == 4 || i == 6) append('-')
            append(digits[i])
        }
    }
}

private val DATE_OF_BIRTH_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

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

@Composable
private fun EditMemberDialog(
    user: com.joi.domain.model.PublicUser,
    loading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (
        fullName: String,
        role: Role,
        dateOfBirth: String?,
        phoneNumber: String?,
        address: String?,
        className: String?,
        note: String?,
        temporaryPassword: String?,
    ) -> Unit,
) {
    var fullName by remember { mutableStateOf(user.fullName) }
    var isModerator by remember { mutableStateOf(user.role == Role.MODERATOR) }
    var dateOfBirth by remember { mutableStateOf(user.dateOfBirth.orEmpty()) }
    var phoneNumber by remember { mutableStateOf(user.phoneNumber.orEmpty()) }
    var address by remember { mutableStateOf(user.address.orEmpty()) }
    var className by remember { mutableStateOf(user.className.orEmpty()) }
    var note by remember { mutableStateOf(user.note.orEmpty()) }
    var temporaryPassword by remember { mutableStateOf("") }

    val dateOfBirthValid = dateOfBirth.isBlank() || DATE_OF_BIRTH_REGEX.matches(dateOfBirth)
    val temporaryPasswordValid = temporaryPassword.isBlank() || temporaryPassword.length >= 6

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit member") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = formatDateOfBirthInput(it) },
                    label = { Text("Date of birth (YYYY-MM-DD)") },
                    placeholder = { Text("YYYY-MM-DD") },
                    isError = !dateOfBirthValid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Class") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (only moderators see this)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = temporaryPassword,
                    onValueChange = { temporaryPassword = it },
                    label = { Text("Reset password (optional)") },
                    placeholder = { Text("Leave blank to keep their current password") },
                    isError = !temporaryPasswordValid,
                    supportingText = {
                        if (!temporaryPasswordValid) Text("Must be at least 6 characters")
                        else Text("Sets a temporary password they'll be asked to change on next login")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = isModerator, onCheckedChange = { isModerator = it })
                    Text("Moderator")
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading && fullName.isNotBlank() && dateOfBirthValid && temporaryPasswordValid,
                onClick = {
                    onConfirm(
                        fullName,
                        if (isModerator) Role.MODERATOR else Role.MEMBER,
                        dateOfBirth.trim().ifBlank { null },
                        phoneNumber.trim().ifBlank { null },
                        address.trim().ifBlank { null },
                        className.trim().ifBlank { null },
                        note.trim().ifBlank { null },
                        temporaryPassword.trim().ifBlank { null },
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
