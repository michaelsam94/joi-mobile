package com.joi.app.ui.prizes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The backend requires a full URL (`https://...`) for a prize's image, and a bare `example.com/x.jpg`
 * typed without a scheme isn't something Coil can load either — this fills in `https://` for
 * anything that doesn't already start with http(s):// so pasting just a domain still works. */
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
                uploadPrizeImageUseCase = container.uploadPrizeImageUseCase,
                getRedeemedPrizeIdsUseCase = container.getRedeemedPrizeIdsUseCase,
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
            uiState.errorMessage != null && uiState.prizes.isEmpty() ->
                ErrorState(uiState.errorMessage!!, modifier = Modifier.padding(padding), onRetry = { viewModel.load() })
            else -> PullToRefreshBox(
                isRefreshing = uiState.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                if (uiState.prizes.isEmpty()) {
                    EmptyState("No prizes yet — check back soon!")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.prizes, key = { it.id }) { prize ->
                            PrizeCard(
                                prize = prize,
                                isModerator = isModerator,
                                isRedeemedByMe = prize.id in uiState.redeemedPrizeIds,
                                onEdit = { viewModel.openEdit(prize) },
                                onDelete = { viewModel.deletePrize(prize) },
                                onRedeem = { viewModel.openRedeem(prize) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showEditor) {
        PrizeEditorDialog(
            initial = uiState.editingPrize,
            errorMessage = uiState.actionError,
            uploading = uiState.uploadingImage,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::savePrize,
            onUploadImage = viewModel::uploadImage,
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
    isRedeemedByMe: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRedeem: () -> Unit,
) {
    // Prize.quantity is a val declared in a different module (domain), so Kotlin won't smart-cast
    // it across the two `prize.quantity` reads above — read it into a local first.
    val quantity = prize.quantity
    val outOfStock = quantity != null && quantity <= 0
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val cardImageUrl = prize.imageUrl?.let(::normalizeImageUrl)
            if (cardImageUrl != null) {
                coil.compose.AsyncImage(
                    model = cardImageUrl,
                    contentDescription = prize.name,
                    // Fit (not Crop) keeps the whole image visible at its real aspect ratio —
                    // Crop was slicing the top/bottom off images that didn't match the box shape.
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
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
            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isRedeemedByMe) {
                    Text(
                        "✅ You've redeemed this",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (outOfStock) {
                    Text("Out of stock", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                } else if (quantity != null) {
                    Text(
                        "$quantity left",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (!prize.active) {
                    Text("Inactive", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
            }

            if (isModerator) {
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
                JoiPrimaryButton(
                    text = if (outOfStock) "Out of stock" else "Redeem for a member",
                    onClick = onRedeem,
                    enabled = !outOfStock,
                )
            }
        }
    }
}

@Composable
private fun PrizeEditorDialog(
    initial: Prize?,
    errorMessage: String?,
    uploading: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, pointsCost: Int, imageUrl: String?, quantity: Int?) -> Unit,
    onUploadImage: (bytes: ByteArray, mimeType: String, onDone: (String?) -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var pointsText by remember { mutableStateOf(initial?.pointsCost?.toString().orEmpty()) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl.orEmpty()) }
    var quantityText by remember { mutableStateOf(initial?.quantity?.toString().orEmpty()) }

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

    AlertDialog(
        onDismissRequest = { if (!uploading) onDismiss() },
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
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                    label = { Text("Quantity in stock (leave blank for unlimited)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        enabled = !uploading,
                        onClick = {
                            pickImageLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
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
                    coil.compose.AsyncImage(
                        model = previewUrl,
                        contentDescription = "Preview",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .padding(top = 8.dp),
                    )
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !uploading && name.isNotBlank() && (pointsText.toIntOrNull() ?: 0) > 0,
                onClick = {
                    onSave(name, description, pointsText.toInt(), normalizeImageUrl(imageUrl), quantityText.toIntOrNull())
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(enabled = !uploading, onClick = onDismiss) { Text("Cancel") } },
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
