package com.canadulce.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors =
    lightColorScheme(
        primary = Forest,
        onPrimary = Color.White,
        primaryContainer = Mint,
        onPrimaryContainer = Ink,
        secondary = Gold,
        secondaryContainer = Color(0xFFFFE9AF),
        onSecondaryContainer = Ink,
        background = Cream,
        onBackground = Ink,
        surface = Color.White,
        onSurface = Ink,
        surfaceVariant = Color(0xFFF0F1E9),
        onSurfaceVariant = Color(0xFF536459),
        outline = Color(0xFF788A7C),
        error = Color(0xFF9C3026),
    )

@Composable
fun CañaDulceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = AppTypography,
        content = content,
    )
}
