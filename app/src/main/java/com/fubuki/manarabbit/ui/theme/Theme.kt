package com.fubuki.manarabbit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── 다크 테마 ──────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    // 강조색
    primary               = Accent,
    onPrimary             = Color.Black,
    primaryContainer      = AccentContainerDark,
    onPrimaryContainer    = AccentDark,
    inversePrimary        = Accent,

    // 보조 (무채색)
    secondary             = Color(0xFF8A8A8A),
    onSecondary           = Color.Black,
    secondaryContainer    = Dark3,
    onSecondaryContainer  = DarkText,

    // 세 번째 (강조색과 동일하게 통일)
    tertiary              = Accent,
    onTertiary            = Color.Black,
    tertiaryContainer     = AccentContainerDark,
    onTertiaryContainer   = AccentDark,

    // 배경 / 서피스
    background            = Dark0,
    onBackground          = DarkText,
    surface               = Dark1,
    onSurface             = DarkText,
    surfaceVariant        = Dark2,
    onSurfaceVariant      = DarkTextSub,
    inverseSurface        = Light2,
    inverseOnSurface      = LightText,
    surfaceTint           = Accent,

    // 컨테이너 단계별 명도
    surfaceContainer         = Dark3,
    surfaceContainerHigh     = Dark4,
    surfaceContainerHighest  = Dark5,
    surfaceContainerLow      = Dark2,
    surfaceContainerLowest   = Dark1,

    // 구분선 / 외곽선
    outline               = DarkOutline,
    outlineVariant        = DarkOutlineVar,
    scrim                 = Color.Black,

    // 에러
    error                 = Color(0xFFCF6679),
    onError               = Color.Black,
    errorContainer        = Color(0xFF5C1A22),
    onErrorContainer      = Color(0xFFFFB3BD),
)

// ── 라이트 테마 ────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    // 강조색
    primary               = Accent,
    onPrimary             = Color.Black,
    primaryContainer      = AccentContainerLight,
    onPrimaryContainer    = AccentContainerDark,
    inversePrimary        = AccentDark,

    // 보조 (무채색)
    secondary             = Color(0xFF5E5E5E),
    onSecondary           = Color.White,
    secondaryContainer    = Light3,
    onSecondaryContainer  = LightText,

    // 세 번째
    tertiary              = Accent,
    onTertiary            = Color.Black,
    tertiaryContainer     = AccentContainerLight,
    onTertiaryContainer   = AccentContainerDark,

    // 배경 / 서피스
    background            = Light1,
    onBackground          = LightText,
    surface               = Light0,
    onSurface             = LightText,
    surfaceVariant        = Light2,
    onSurfaceVariant      = LightTextSub,
    inverseSurface        = Dark2,
    inverseOnSurface      = DarkText,
    surfaceTint           = Accent,

    // 컨테이너 단계별 명도
    surfaceContainer         = Light3,
    surfaceContainerHigh     = Light4,
    surfaceContainerHighest  = Light5,
    surfaceContainerLow      = Light2,
    surfaceContainerLowest   = Light0,

    // 구분선 / 외곽선
    outline               = LightOutline,
    outlineVariant        = LightOutlineVar,
    scrim                 = Color.Black,

    // 에러
    error                 = Color(0xFFB3261E),
    onError               = Color.White,
    errorContainer        = Color(0xFFF9DEDC),
    onErrorContainer      = Color(0xFF410E0B),
)

@Composable
fun ManaRabbitTheme(
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
