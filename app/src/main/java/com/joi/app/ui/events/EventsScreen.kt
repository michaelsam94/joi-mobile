package com.joi.app.ui.events

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.joi.app.di.AppContainer
import com.joi.app.util.formatMoney
import com.joi.app.util.todayIsoDate
import com.joi.app.util.viewModelFactoryOf
import com.joi.designsystem.components.EmptyState
import com.joi.designsystem.components.ErrorState
import com.joi.designsystem.components.JoiPrimaryButton
import com.joi.designsystem.components.JoiTopBar
import com.joi.designsystem.components.LoadingState
import com.joi.domain.model.Event
import com.joi.domain.repository.EventInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Same rule the prize editor uses: a bare `example.com/x.jpg` isn't a URL the backend accepts or
 * Coil can load, so fill in the scheme for anything pasted without one. */
private fun normalizeImageUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    container: AppContainer,
    isModerator: Boolean,
    onOpenPayments: (eventId: String) -> Unit,
) {
    val viewModel: EventsViewModel = viewModel(
        factory = viewModelFactoryOf {
            EventsViewModel(
                isModerator = isModerator,
                listEventsUseCase = container.listEventsUseCase,
                saveEventUseCase = container.saveEventUseCase,
                deleteEventUseCase = container.deleteEventUseCase,
                getMyEventPaymentsUseCase = container.getMyEventPaymentsUseCase,
                uploadEventImageUseCase = container.uploadEventImageUseCase,
            )
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Event?>(null) }

    Scaffold(
        topBar = {
            JoiTopBar(
                title = "Events",
                actions = {
                    AssistChip(
                        onClick = viewModel::toggleUpcomingOnly,
                        label = { Text(if (uiState.upcomingOnly) "Upcoming" else "All") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                },
            )
        },
        floatingActionButton = {
            if (isModerator) {
                FloatingActionButton(onClick = viewModel::openCreate) {
                    Icon(Icons.Default.Add, contentDescription = "Add event")
                }
            }
        },
    ) { padding ->
        when {
            uiState.loading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.errorMessage != null && uiState.events.isEmpty() ->
                ErrorState(uiState.errorMessage!!, modifier = Modifier.padding(padding), onRetry = { viewModel.load() })
            else -> PullToRefreshBox(
                isRefreshing = uiState.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                if (uiState.events.isEmpty()) {
                    EmptyState(
                        if (uiState.upcomingOnly) {
                            "No events coming up yet — check back soon!"
                        } else {
                            "No events have been added yet."
                        },
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.events, key = { it.id }) { event ->
                            EventCard(
                                event = event,
                                isModerator = isModerator,
                                onEdit = { viewModel.openEdit(event) },
                                onDelete = { pendingDelete = event },
                                onOpenPayments = { onOpenPayments(event.id) },
                                onOpenMyPayments = { viewModel.openMyPayments(event) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showEditor) {
        EventEditorDialog(
            initial = uiState.editingEvent,
            errorMessage = uiState.actionError,
            uploading = uiState.uploadingImage,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::saveEvent,
            onUploadImage = viewModel::uploadImage,
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove \"${toDelete.name}\"?") },
            // Worth spelling out: the payment ledger goes with it, and that's real money history.
            text = { Text("This also deletes every payment recorded for this event. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEvent(toDelete)
                        pendingDelete = null
                    },
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }

    val myPaymentsFor = uiState.myPaymentsFor
    if (myPaymentsFor != null) {
        MyPaymentsDialog(
            event = myPaymentsFor,
            state = uiState,
            onDismiss = viewModel::dismissMyPayments,
        )
    }

    val actionError = uiState.actionError
    if (actionError != null && !uiState.showEditor) {
        AlertDialog(
            onDismissRequest = viewModel::dismissActionError,
            title = { Text("Couldn't do that") },
            text = { Text(actionError) },
            confirmButton = { TextButton(onClick = viewModel::dismissActionError) { Text("OK") } },
        )
    }
}

@Composable
private fun EventCard(
    event: Event,
    isModerator: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenMyPayments: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Tapping anywhere on the card opens your own payment history for the event — the
            // thing a member most often wants and the moderator's quickest sanity check.
            .clickable(onClick = onOpenMyPayments),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val imageUrl = event.imageUrl?.let(::normalizeImageUrl)
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = event.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).padding(bottom = 8.dp),
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(event.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (event.price <= 0.0) "Free" else formatMoney(event.price),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            EventFactsRow(event, modifier = Modifier.padding(top = 4.dp))

            val description = event.description
            if (!description.isNullOrBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (!event.active) {
                Text(
                    "Hidden from members",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            PaymentProgress(
                price = event.price,
                paidAmount = event.myPaidAmount,
                remainingAmount = event.myRemainingAmount,
                fullyPaid = event.myFullyPaid,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (isModerator) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit event") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Remove event") }
                }
                JoiPrimaryButton(text = "Manage payments", onClick = onOpenPayments)
            } else {
                OutlinedButton(onClick = onOpenMyPayments, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" My payments", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

/** Everyone's own ledger for one event: each installment recorded against them, and the balance. */
@Composable
private fun MyPaymentsDialog(event: Event, state: EventsUiState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(event.name) },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                val mine = state.myPayments
                when {
                    state.loadingMyPayments -> CircularProgressIndicator()
                    mine == null -> Text("Couldn't load your payments.")
                    else -> {
                        PaymentProgress(
                            price = mine.price,
                            paidAmount = mine.paidAmount,
                            remainingAmount = mine.remainingAmount,
                            fullyPaid = mine.fullyPaid,
                        )
                        if (mine.payments.isEmpty()) {
                            Text(
                                if (mine.price <= 0.0) {
                                    "Nothing to pay for this one."
                                } else {
                                    "No payments recorded yet — see a moderator to pay, in full or in parts."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        } else {
                            Text(
                                "Payments",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                            LazyColumn { items(mine.payments, key = { it.id }) { PaymentRow(it) } }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun EventEditorDialog(
    initial: Event?,
    errorMessage: String?,
    uploading: Boolean,
    onDismiss: () -> Unit,
    onSave: (EventInput) -> Unit,
    onUploadImage: (bytes: ByteArray, mimeType: String, onDone: (String?) -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var location by remember { mutableStateOf(initial?.location.orEmpty()) }
    // Deliberately NOT formatMoney here — that adds thousands separators, and this string is
    // parsed straight back into a Double when the moderator saves.
    var priceText by remember {
        mutableStateOf(
            initial?.price?.let { if (it == Math.floor(it)) it.toLong().toString() else it.toString() }.orEmpty(),
        )
    }
    // A new event defaults to today's date, so the moderator only edits it when it isn't today.
    var dateText by remember { mutableStateOf(initial?.eventDate ?: todayIsoDate()) }
    var timeText by remember { mutableStateOf(initial?.eventTime.orEmpty()) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl.orEmpty()) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val (bytes, mimeType) = withContext(Dispatchers.IO) {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                data to mime
            }
            if (bytes != null) {
                onUploadImage(bytes, mimeType) { uploadedUrl -> if (uploadedUrl != null) imageUrl = uploadedUrl }
            }
        }
    }

    // The price is typed with the local decimal separator on some keyboards; accept a comma too
    // rather than silently reading "12,5" as nothing.
    val price = priceText.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = { if (!uploading) onDismiss() },
        title = { Text(if (initial == null) "New event" else "Edit event") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Price per person (0 for free)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("Start time (HH:MM, optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Poster image URL (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        enabled = !uploading,
                        onClick = {
                            pickImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Choose a photo", modifier = Modifier.padding(start = 4.dp))
                    }
                    if (uploading) {
                        CircularProgressIndicator(modifier = Modifier.padding(start = 12.dp).size(20.dp))
                    }
                }
                val previewUrl = normalizeImageUrl(imageUrl)
                if (previewUrl != null) {
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = "Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).padding(top = 8.dp),
                    )
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !uploading && name.isNotBlank() && price != null && dateText.isNotBlank(),
                onClick = {
                    onSave(
                        EventInput(
                            name = name.trim(),
                            description = description.ifBlank { null },
                            location = location.ifBlank { null },
                            price = price ?: 0.0,
                            eventDate = dateText.trim(),
                            eventTime = timeText.trim().ifBlank { null },
                            imageUrl = normalizeImageUrl(imageUrl),
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(enabled = !uploading, onClick = onDismiss) { Text("Cancel") } },
    )
}
