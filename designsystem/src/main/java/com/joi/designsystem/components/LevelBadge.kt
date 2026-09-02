package com.joi.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joi.designsystem.theme.LevelBronze
import com.joi.designsystem.theme.LevelDiamond
import com.joi.designsystem.theme.LevelGold
import com.joi.designsystem.theme.LevelSilver
import com.joi.domain.model.Level

private fun Level.color(): Color = when (this) {
    Level.BRONZE -> LevelBronze
    Level.SILVER -> LevelSilver
    Level.GOLD -> LevelGold
    Level.DIAMOND -> LevelDiamond
}

private fun Level.emoji(): String = when (this) {
    Level.BRONZE -> "🥉"
    Level.SILVER -> "🥈"
    Level.GOLD -> "🥇"
    Level.DIAMOND -> "💎"
}

private fun Level.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

/** A small chip showing a player's gamified level — used on the leaderboard, profile, and member cards. */
@Composable
fun LevelBadge(level: Level, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = level.color().copy(alpha = 0.18f),
    ) {
        Text(
            text = "${level.emoji()} ${level.label()}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = level.color(),
        )
    }
}
