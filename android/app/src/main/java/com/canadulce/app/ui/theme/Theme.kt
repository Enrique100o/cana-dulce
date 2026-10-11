package com.canadulce.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Forest = Color(0xFF245C42)
val Leaf = Color(0xFF427A4B)
val Cream = Color(0xFFF8F6EF)
val Mint = Color(0xFFEAF2E4)
val Ink = Color(0xFF24372D)
val Gold = Color(0xFFAA6D00)
private val Colors = lightColorScheme(
    primary = Forest, onPrimary = Color.White, primaryContainer = Mint, onPrimaryContainer = Ink,
    secondary = Gold, secondaryContainer = Color(0xFFFFE9AF), onSecondaryContainer = Ink,
    background = Cream, onBackground = Ink, surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFF0F1E9), onSurfaceVariant = Color(0xFF536459),
    outline = Color(0xFF788A7C), error = Color(0xFF9C3026)
)

@Composable
fun CañaDulceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = Typography(
            headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold),
            headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
            titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 26.sp),
            bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
            labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        ),
        content = content
    )
}
