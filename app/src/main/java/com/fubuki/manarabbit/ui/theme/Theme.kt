package com.fubuki.manarabbit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Green40,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D20),
    onPrimaryContainer = Color(0xFFB7F5C8),
    secondary = GreenDark40,
    onSecondary = Color.Black,
    tertiary = GreenLight40,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkBackground,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkBackground,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF1A1A1A),
    surfaceContainerLow = DarkBackground,
    surfaceContainerLowest = DarkBackground,
    outline = Color(0xFF444444),
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F5C8),
    onPrimaryContainer = Color(0xFF004D20),
    secondary = GreenDark40,
    onSecondary = Color.White,
    tertiary = GreenLight40,
    onTertiary = Color.Black,
)

@Composable
fun ManaRabbitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}