package com.example.csvtango.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** 어두운 리더 배색 하나만 쓴다. (색 정의는 Color.kt) */
private val PaperScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Accent,
    background = Paper,
    onBackground = Ink,
    surface = Sheet,
    onSurface = Ink,
    surfaceVariant = FieldBg,
    surfaceContainer = Sheet,
    surfaceContainerHigh = Hover,
    surfaceContainerHighest = Hover,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    scrim = Scrim,
    onSurfaceVariant = Muted,
    outline = Muted,
    outlineVariant = Line,
    error = ErrInk,
    errorContainer = ErrBg,
    onErrorContainer = ErrInk,
)

@Composable
fun CsvTangoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PaperScheme,
        typography = Typography,
        content = content,
    )
}
