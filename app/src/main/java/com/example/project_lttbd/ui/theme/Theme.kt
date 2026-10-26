package com.example.project_lttbd.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = MmraPrimary,
    onPrimary = MmraOnPrimary,
    primaryContainer = MmraPrimaryContainer,
    onPrimaryContainer = MmraOnPrimaryContainer,

    secondary = MmraSecondary,
    onSecondary = Color.White,
    secondaryContainer = MmraSurfaceVariant,
    onSecondaryContainer = MmraTextPrimary,

    background = MmraBackground,
    onBackground = MmraTextPrimary,
    surface = MmraSurface,
    onSurface = MmraTextPrimary,
    surfaceVariant = MmraSurfaceVariant,
    onSurfaceVariant = MmraTextSecondary,
    outline = MmraOutline,
    outlineVariant = Color(0xFFD0DEDA),

    error = MmraError,
    onError = Color.White,
    errorContainer = MmraErrorContainer,
    onErrorContainer = MmraOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF85D5C7),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFFA1F2E3),

    secondary = Color(0xFFB1CCC4),
    onSecondary = Color(0xFF1D352F),
    secondaryContainer = Color(0xFF344C45),
    onSecondaryContainer = Color(0xFFCDE9E0),

    background = Color(0xFF101513),
    onBackground = Color(0xFFDFE4E0),
    surface = Color(0xFF171D1A),
    onSurface = Color(0xFFDFE4E0),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBFC9C3),
    outline = Color(0xFF89938D),
    outlineVariant = Color(0xFF3F4945),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun MMRATheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}