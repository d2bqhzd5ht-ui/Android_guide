package com.example.scenicguide.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ScenicGreen = Color(0xFF0E7C66)
private val ScenicGreenDark = Color(0xFF00513F)
private val ScenicSand = Color(0xFFFFD166)

private val LightColors = lightColorScheme(
    primary = ScenicGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7EADB),
    onPrimaryContainer = ScenicGreenDark,
    secondary = Color(0xFF4A635B),
    onSecondary = Color.White,
    tertiary = ScenicSand,
    background = Color(0xFFF7FBF8),
    onBackground = Color(0xFF191C1B),
    surface = Color.White,
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDCE5DF),
    onSurfaceVariant = Color(0xFF404944),
    error = Color(0xFFBA1A1A),
)

@Composable
fun ScenicGuideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
