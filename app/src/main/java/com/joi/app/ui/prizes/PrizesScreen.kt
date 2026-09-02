package com.joi.app.ui.prizes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
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
import com.joi.domain.model.Prize

@Composable
fun PrizesScreen(container: AppContainer, isModerator: Boolean) {
    val viewModel: PrizesViewModel = viewModel(
        factory = viewModelFactoryOf {
            PrizesViewModel(
                isModerator = isModerator,
                listPrizesUseCase = container.listPrizesUseCase,
                savePrizeUseCase = container.savePrizeUseCase,
                deletePrizeUseCase = container.deletePrizeUseCase,
                redeemPrizeUseCase = container.redeemPrizeUseCase,
                listMembersUseCase = container.listMembersUseCase,
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { JoiTopBar(title = "Prizes") },
        floatingActionButton = {
            if (isModerator) {
                FloatingActionButton(onClick = viewModel::openCreate) {
                    Icon(Icons.Default.Add, contentDescription = "Add prize")
                }
            }
        },
    ) { padding ->
        when {
            uiState.loading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.errorMessage != null ->
                ErrorState(uiState.errorMessage!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            uiState.prizes.isEmpty() -> EmptyState("No prizes yet — check back soon!", modifier = Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.prizes, key = { it.id }) { prize ->
                    PrizeCard(
                        prize = prize,
                        isModerator = isModerator,
                        onEdit = { viewModel.openEdit(prize) },
                        onDelete = { viewModel.deletePrize(prize) },
                        onRedeem = { viewModel.openRedeem(prize) },
                    )
                }
            }
        }
    }

    if (uiState.showEditor) {
        PrizeEditorDialog(
            initial = uiState.editingPrize,
            errorMessage = uiState.actionError,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::savePrize,
        )
    }

    val redeeming = uiState.redeemingPrize
    if (redeeming != null) {
        RedeemDialog(
            prize = redeeming,
            members = uiState.members,
            errorMessage = uiState.actionError,
            onDismiss = viewModel::dismissRedeem,
            onPick = viewModel::redeem,
        )
    }

    if (uiState.actionMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissActionMessage,
            title = { Text("Redeemed!") },
            text = { Text(uiState.actionMessage!!) },
            confirmButton = { TextButton(onClick = viewModel::dismissActionMessage) { Text("OK") } },
        )
    }
}

@Composable
private fun PrizeCard(
    prize: Prize,
    isModerator: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRedeem: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!prize.imageUrl.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = prize.imageUrl,
                    contentDescription = prize.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .padding(bottom = 8.dp),
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(prize.name, style = MaterialTheme.typography.titleMedium)
                Text("⭐ ${prize.pointsCost} pts", style = MaterialTheme.typography.titleMedium)
            }
            val description = prize.description
            if (!description.isNullOrBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (!prize.active) {
                Text("Inactive", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }

            if (isModerator) {
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
                JoiPrimaryButton(text = "Redeem for a member", onClick = onRedeem)
            }
        }
    }
}

@Composable
private fun PrizeEditorDialog(
    initial: Prize?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, pointsCost: Int) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var pointsText by remember { mutableStateOf(initial?.pointsCost?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New prize" else "Edit prize") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = pointsText,
                    onValueChange = { pointsText = it.filter { c -> c.isDigit() } },
                    label = { Text("Points cost") },
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (pointsText.toIntOrNull() ?: 0) > 0,
                onClick = { onSave(name, description, pointsText.toInt()) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RedeemDialog(
    prize: Prize,
    members: List<com.joi.domain.model.PublicUser>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onPick: (userId: String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redeem \"${prize.name}\" (${prize.pointsCost} pts)") },
        text = {
            Column(modifier = Modifier.heightIn(max = 360.dp)) {
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(members, key = { it.id }) { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(onClick = { onPick(member.id) }) {
                                Text("${member.fullName} — ${member.totalPoints} pts")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
