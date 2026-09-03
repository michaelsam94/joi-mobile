package com.joi.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A small chip marking someone as a moderator — shown next to their name wherever a list mixes
 * members and moderators, so a moderator is identifiable at a glance without opening their
 * profile. Mirrors LevelBadge's pill style for visual consistency. Only ever appears for a
 * moderator; an ordinary member gets no badge rather than a "Member" one, since that's the
 * default everyone already assumes.
 */
@Composable
fun ModeratorBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
    ) {
        Text(
            text = "🛡️ Moderator",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
