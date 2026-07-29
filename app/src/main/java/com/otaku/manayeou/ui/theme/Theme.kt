package com.otaku.manayeou.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.otaku.manayeou.data.local.ThemeMode
import com.otaku.manayeou.data.local.ThemeState

private val DarkScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Primary,
    onSecondary = OnPrimary,
    secondaryContainer = Primary,
    onSecondaryContainer = OnPrimary,
    background = DarkBg,
    surface = Surface,
    // 하단 네비게이션 바/바텀시트가 기본으로 쓰는 톤 — 지정 안 하면 Material 기본 뉴트럴(연보라 느낌) 새서 기존 톤 재사용
    surfaceContainer = Surface,
    surfaceContainerLow = DarkBg,
    surfaceVariant = SurfaceVariant,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error
)

private val LightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    secondary = LightPrimary,
    onSecondary = LightOnPrimary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background = LightBg,
    surface = LightSurface,
    surfaceContainer = LightBg,
    surfaceContainerLow = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = LightError
)

@Composable
fun ManayeouTheme(content: @Composable () -> Unit) {
    val mode by ThemeState.mode.collectAsState()
    val useDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkScheme else LightScheme,
        content = content
    )
}
