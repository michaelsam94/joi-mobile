package com.joi.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * A "fill level" progress bar standing in for the ship/wave motif from the logo — used to show
 * progress toward the next level. `progress` is clamped to [0f, 1f].
 */
@Composable
fun WaveProgress(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "waveProgress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
