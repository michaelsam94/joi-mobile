package com.joi.app.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joi.app.di.AppContainer
import com.joi.app.util.formatMoney
import com.joi.app.util.viewModelFactoryOf
import com.joi.designsystem.components.ErrorState
import com.joi.designsystem.components.JoiTopBar
import com.joi.designsystem.components.LoadingState
import com.joi.domain.model.EventPayment
import com.joi.domain.model.EventRosterEntry

/**
 * The moderator's payment sheet for one event: every member, what they've paid so far, and what's
 * left. Each row can take another installment, have its running total set outright, or have any
 * individual payment corrected or removed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventPaymentsScreen(container: AppContainer, eventId: String, onBack: () -> Unit) {
    val viewModel: EventPaymentsViewModel = viewModel(
        factory = viewModelFactoryOf {
            EventPaymentsViewModel(
                eventId = eventId,
                getEventRosterUseCase = container.getEventRosterUseCase,
                recordEventPaymentUseCase = container.recordEventPaymentUseCase,
                setMemberEventTotalUseCase = container.setMemberEventTotalUseCase,
                updateEventPaymentUseCase = container.updateEventPaymentUseCase,
                deleteEventPaymentUseCase = container.deleteEventPaymentUseCase,
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val roster = uiState.roster
    var pendingDelete by remember { mutableStateOf<EventPayment?>(null) }

    Scaffold(
        topBar = { JoiTopBar(title = roster?.event?.name ?: "Payments", onBack = onBack) },
    ) { padding ->
        when {
            uiState.loading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.errorMessage != null && roster == null ->
                ErrorState(uiState.errorMessage!!, modifier = Modifier.padding(padding), onRetry = { viewModel.load() })
            roster != null -> PullToRefreshBox(
                isRefreshing = uiState.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                val query = uiState.query.trim()
                val entries = if (query.isBlank()) {
                    roster.entries
                } else {
                    roster.entries.filter { it.fullName.contains(query, ignoreCase = true) }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        EventTotalsCard(
                            price = roster.event.price,
                            totalCollected = roster.totalCollected,
                            totalExpected = roster.totalExpected,
                            paidInFullCount = roster.entries.count { it.fullyPaid },
                            memberCount = roster.entries.size,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.query,
                            onValueChange = viewModel::onQueryChange,
                            label = { Text("Search members") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        )
                    }
                    items(entries, key = { it.userId }) { entry ->
                        MemberPaymentCard(
                            entry = entry,
                            price = roster.event.price,
                            expanded = entry.userId in uiState.expandedUserIds,
                            onToggleExpanded = { viewModel.toggleExpanded(entry.userId) },
                            onAddPayment = { viewModel.openAddPayment(entry) },
                            onSetTotal = { viewModel.openSetTotal(entry) },
                            onEditPayment = { payment -> viewModel.openEditPayment(entry, payment) },
                            onDeletePayment = { payment -> pendingDelete = payment },
                        )
                    }
                    if (entries.isEmpty()) {
                        item {
                            Text(
                                "Nobody matches \"$query\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    val entry = uiState.dialogEntry
    if (entry != null && uiState.dialogMode != null) {
        AmountDialog(
            entry = entry,
            price = roster?.event?.price ?: 0.0,
            mode = uiState.dialogMode!!,
            editing = uiState.editingPayment,
            saving = uiState.saving,
            errorMessage = uiState.actionError,
            onDismiss = viewModel::dismissDialog,
            onSubmit = viewModel::submit,
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove this payment?") },
            text = { Text("${formatMoney(toDelete.amount)} will come off their total.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePayment(toDelete)
                        pendingDelete = null
                    },
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }

    val actionMessage = uiState.actionMessage
    if (actionMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissActionMessage,
            title = { Text("Saved") },
            text = { Text(actionMessage) },
            confirmButton = { TextButton(onClick = viewModel::dismissActionMessage) { Text("OK") } },
        )
    }

    val actionError = uiState.actionError
    if (actionError != null && uiState.dialogMode == null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissActionError,
            title = { Text("Couldn't do that") },
            text = { Text(actionError) },
            confirmButton = { TextButton(onClick = viewModel::dismissActionError) { Text("OK") } },
        )
    }
}

@Composable
private fun EventTotalsCard(
    price: Double,
    totalCollected: Double,
    totalExpected: Double,
    paidInFullCount: Int,
    memberCount: Int,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Collected so far", style = MaterialTheme.typography.labelMedium)
            Text(
                "${formatMoney(totalCollected)} of ${formatMoney(totalExpected)}",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "$paidInFullCount of $memberCount paid in full · ${formatMoney(price)} per person",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun MemberPaymentCard(
    entry: EventRosterEntry,
    price: Double,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAddPayment: () -> Unit,
    onSetTotal: () -> Unit,
    onEditPayment: (EventPayment) -> Unit,
    onDeletePayment: (EventPayment) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpanded),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(entry.fullName, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${entry.payments.size} payment${if (entry.payments.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Hide payments" else "Show payments",
                    )
                }
            }

            PaymentProgress(
                price = price,
                paidAmount = entry.paidAmount,
                remainingAmount = entry.remainingAmount,
                fullyPaid = entry.fullyPaid,
                modifier = Modifier.padding(top = 4.dp),
            )

            if (expanded) {
                if (entry.payments.isEmpty()) {
                    Text(
                        "Nothing recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        entry.payments.forEach { payment ->
                            PaymentRow(payment) {
                                Row {
                                    IconButton(onClick = { onEditPayment(payment) }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit payment",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    IconButton(onClick = { onDeletePayment(payment) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove payment",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onAddPayment, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Add payment")
                }
                OutlinedButton(onClick = onSetTotal, modifier = Modifier.weight(1f)) { Text("Set total") }
            }
        }
    }
}

/**
 * One dialog serving all three amount edits — a new installment, a correction to an existing one,
 * and an outright total — because they only differ in their heading and what the number means.
 */
@Composable
private fun AmountDialog(
    entry: EventRosterEntry,
    price: Double,
    mode: PaymentDialogMode,
    editing: EventPayment?,
    saving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, note: String?) -> Unit,
) {
    val isSetTotal = mode == PaymentDialogMode.SET_TOTAL && editing == null
    var amountText by remember {
        mutableStateOf(
            when {
                editing != null -> editing.amount.toPlainText()
                isSetTotal -> entry.paidAmount.toPlainText()
                // Default a fresh installment to whatever's still owed: the common case is
                // someone settling up, and the moderator can always type a smaller part-payment.
                entry.remainingAmount > 0 -> entry.remainingAmount.toPlainText()
                else -> ""
            },
        )
    }
    var note by remember { mutableStateOf(editing?.note.orEmpty()) }
    val amount = amountText.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text(
                when {
                    editing != null -> "Edit payment"
                    isSetTotal -> "Set total paid"
                    else -> "Add payment"
                },
            )
        },
        text = {
            Column {
                Text(
                    "${entry.fullName} · paid ${formatMoney(entry.paidAmount)} of ${formatMoney(price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text(if (isSetTotal) "Total paid" else "Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (!isSetTotal) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                Text(
                    if (isSetTotal) {
                        "Replaces their running total. The payments already recorded stay in the history."
                    } else {
                        "Added on top of what they've already paid — record as many as you need."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                // Setting a total to 0 is meaningful ("they've paid nothing"); a 0 installment
                // isn't, so only the total case accepts it.
                enabled = !saving && amount != null && (isSetTotal || amount != 0.0),
                onClick = { onSubmit(amount ?: 0.0, note.ifBlank { null }) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

/** An editable representation of an amount — no thousands separators, since this string is parsed
 * straight back into a Double. */
private fun Double.toPlainText(): String =
    if (this == Math.floor(this)) toLong().toString() else toString()
