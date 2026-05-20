package com.arqcdemo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF6EE7FF)
val Pass = Color(0xFF3FFF7D)
val Rework = Color(0xFFFFD54A)
val Scrap = Color(0xFFFF6B6B)
val HudDim = Color(0xB3FFFFFF) // 70% white

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF001620),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
)

@Composable
fun ArQcDemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content,
    )
}
