package com.joi.app.ui.raffle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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

/** How long the wheel takes to come to rest. Long enough to feel like a draw, short enough that a
 * room of people doesn't lose interest between rounds. */
private const val SPIN_MILLIS = 4200

/**
 * Moderator-only. Spins over the draw numbers handed out at check-in, showing the numbers and
 * nothing else — no names anywhere on this screen, so picking a winner can't be steered towards a
 * person. Matching the drawn number back to whoever holds it is a separate, deliberate step on the
 * Members tab.
 */
@Composable
fun LuckyWheelScreen(container: AppContainer, onBack: () -> Unit) {
    val viewModel: LuckyWheelViewModel = viewModel(
        factory = viewModelFactoryOf { LuckyWheelViewModel(container.listRaffleNumbersUseCase) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rotation = remember { Animatable(0f) }
    val inPlay = uiState.inPlay

    // The pick is already made; this only animates the wheel to where it lands. Keyed on spinId so
    // drawing the same number twice in a row still spins.
    val currentInPlay by rememberUpdatedState(inPlay)
    LaunchedEffect(uiState.spinId) {
        val winner = uiState.pendingWinner ?: return@LaunchedEffect
        val index = currentInPlay.indexOf(winner)
        if (index < 0) {
            viewModel.onSpinSettled()
            return@LaunchedEffect
        }
        rotation.animateTo(
            targetValue = targetRotationFor(index, currentInPlay.size, rotation.value),
            animationSpec = tween(durationMillis = SPIN_MILLIS, easing = FastOutSlowInEasing),
        )
        viewModel.onSpinSettled()
    }

    Scaffold(
        topBar = {
            JoiTopBar(
                title = "Lucky Wheel",
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::load, enabled = !uiState.spinning) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload numbers")
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.loading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.errorMessage != null && uiState.allNumbers.isEmpty() ->
                ErrorState(uiState.errorMessage!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            uiState.allNumbers.isEmpty() ->
                EmptyState(
                    "No draw numbers yet — hand some out from the check-in popup first.",
                    modifier = Modifier.padding(padding),
                )
            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    countLine(inPlay.size, uiState.removed.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                if (inPlay.isEmpty()) {
                    Text(
                        "Every number has been drawn and taken out.",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                } else {
                    WheelOfNumbers(
                        numbers = inPlay,
                        rotation = rotation.value,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                JoiPrimaryButton(
                    text = if (uiState.spinning) "Spinning…" else "Spin",
                    onClick = viewModel::spin,
                    enabled = !uiState.spinning && inPlay.isNotEmpty(),
                    modifier = Modifier.padding(top = 24.dp),
                )

                if (uiState.removed.isNotEmpty()) {
                    OutlinedButton(
                        onClick = viewModel::restoreRemoved,
                        enabled = !uiState.spinning,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Text("Put the ${uiState.removed.size} drawn number${plural(uiState.removed.size)} back")
                    }
                }

                Card(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(
                        "Numbers only — nobody's name appears here, so the draw can't be steered. " +
                            "Search the number on the Members tab to find who holds it.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }

    val winner = uiState.winner
    if (winner != null) {
        WinnerDialog(
            winner = winner,
            remainingIfRemoved = inPlay.size - 1,
            onKeep = viewModel::keepWinner,
            onRemove = viewModel::removeWinner,
        )
    }
}

/** The decision the moderator makes on every draw: does this number stay eligible next round? */
@Composable
private fun WinnerDialog(
    winner: Int,
    remainingIfRemoved: Int,
    onKeep: () -> Unit,
    onRemove: () -> Unit,
) {
    AlertDialog(
        // Not dismissible by tapping away: keep-or-remove decides what the next round draws from,
        // so it shouldn't be possible to skip it by accident.
        onDismissRequest = {},
        title = { Text("🎉 Number $winner") },
        text = {
            Column {
                Text(
                    "Keep it in for the next round, or take it out so it can't win again.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    if (remainingIfRemoved > 0) {
                        "Taking it out leaves $remainingIfRemoved number${plural(remainingIfRemoved)} in the wheel."
                    } else {
                        "This is the last number in the wheel."
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = onRemove) { Text("Take it out") } },
        dismissButton = { TextButton(onClick = onKeep) { Text("Keep it in") } },
    )
}

private fun plural(count: Int) = if (count == 1) "" else "s"

private fun countLine(inPlay: Int, removed: Int): String {
    val base = "$inPlay number${plural(inPlay)} in the wheel"
    return if (removed == 0) base else "$base · $removed taken out"
}
