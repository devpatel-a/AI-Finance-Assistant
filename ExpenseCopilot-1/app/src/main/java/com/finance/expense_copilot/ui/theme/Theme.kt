package com.finance.expense_copilot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FintasticsPurpleLight,
    secondary = FintasticsPurpleMain,
    tertiary = GlassWhite,
    background = BackgroundDark,
    surface = BackgroundDark,
    onPrimary = FintasticsPurpleDark,
    onBackground = PureWhite,
    onSurface = PureWhite,
    surfaceVariant = GlassDark,
    onSurfaceVariant = PureWhite
)

private val LightColorScheme = lightColorScheme(
    primary = FintasticsPurpleMain,
    secondary = FintasticsPurpleDark,
    tertiary = GlassDark,
    background = OffWhite,
    surface = OffWhite,
    onPrimary = PureWhite,
    onBackground = FintasticsPurpleDark,
    onSurface = FintasticsPurpleDark,
    surfaceVariant = GlassWhite,
    onSurfaceVariant = FintasticsPurpleDark
)

@Composable
fun ExpenseCopilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}