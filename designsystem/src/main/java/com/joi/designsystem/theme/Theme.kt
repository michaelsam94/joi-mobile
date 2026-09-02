package com.joi.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = JoiTeal40,
    onPrimary = JoiNeutral99,
    primaryContainer = JoiTeal90,
    onPrimaryContainer = JoiTeal10,
    secondary = JoiGold40,
    onSecondary = JoiNeutral99,
    secondaryContainer = JoiGold90,
    onSecondaryContainer = JoiGold40,
    tertiary = JoiCoral40,
    onTertiary = JoiNeutral99,
    tertiaryContainer = JoiCoral90,
    onTertiaryContainer = JoiCoral40,
    background = JoiNeutral99,
    onBackground = JoiNeutral10,
    surface = JoiNeutral99,
    onSurface = JoiNeutral10,
)

private val DarkColors = darkColorScheme(
    primary = JoiTeal80,
    onPrimary = JoiTeal20,
    primaryContainer = JoiTeal30,
    onPrimaryContainer = JoiTeal90,
    secondary = JoiGold80,
    onSecondary = JoiGold40,
    secondaryContainer = JoiGold40,
    onSecondaryContainer = JoiGold90,
    tertiary = JoiCoral80,
    onTertiary = JoiCoral40,
    tertiaryContainer = JoiCoral40,
    onTertiaryContainer = JoiCoral90,
    background = JoiNeutral10,
    onBackground = JoiNeutral90,
    surface = JoiNeutral10,
    onSurface = JoiNeutral90,
)

/**
 * The app's single theme entry point. Dynamic color (Android 12+) is intentionally left OFF by
 * default — Joi's brand palette is part of its game identity, and letting the wallpaper override
 * it would wash that out. Flip `useDynamicColor` on if you'd rather each phone feel native.
 */
@Composable
fun JoiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = JoiTypography,
        shapes = JoiShapes,
        content = content,
    )
}
