package com.example.csvtango.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 웹 버전과 같은 종이 느낌의 밝은 배색 하나만 쓴다.
 * (원본 웹앱에도 어두운 배색이 없다.)
 */
private val PaperScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Accent,
    background = Paper,
    onBackground = Ink,
    surface = Sheet,
    onSurface = Ink,
    surfaceVariant = FieldBg,
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
