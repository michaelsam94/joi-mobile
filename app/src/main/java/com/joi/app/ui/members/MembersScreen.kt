package com.joi.app.ui.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joi.app.di.AppContainer
import com.joi.app.util.viewModelFactoryOf
import com.joi.designsystem.components.EmptyState
import com.joi.designsystem.components.ErrorState
import com.joi.designsystem.components.JoiTopBar
import com.joi.designsystem.components.LevelBadge
import com.joi.designsystem.components.LoadingState
import com.joi.designsystem.components.PointsPill

/** Moderator-only: browse/search everyone, tap through to a member's detail, or register someone new via the FAB. */
@Composable
fun MembersScreen(container: AppContainer, onOpenMember: (String) -> Unit, onRegisterMember: () -> Unit) {
    val viewModel: MembersViewModel = viewModel(
        factory = viewModelFactoryOf { MembersViewModel(container.listMembersUseCase) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Registering, editing, or deactivating a member all happen on a screen pushed on top of this
    // one (Register/MemberDetail) — reload every time we come back to the top of the back stack so
    // the list reflects those changes the moment the "Save"/back action returns here, instead of
    // showing stale data until the user manually refreshes.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = { JoiTopBar(title = "Members") },
        floatingActionButton = {
            FloatingActionButton(onClick = onRegisterMember) {
                Icon(Icons.Default.Add, contentDescription = "Register new member")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Search by name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )

            when {
                uiState.loading -> LoadingState()
                uiState.errorMessage != null -> ErrorState(uiState.errorMessage!!, onRetry = viewModel::load)
                uiState.filtered.isEmpty() -> EmptyState("No members match \"${uiState.query}\"")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.filtered, key = { it.id }) { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOpenMember(user.id) },
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = user.fullName + if (!user.active) " (inactive)" else "",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    LevelBadge(level = user.level)
                                    PointsPill(points = user.totalPoints)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
