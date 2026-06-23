package com.smchess.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val BubbleOutgoing = Color(0xFF2A5C45)
val BubbleIncoming = Color(0xFF262626)
val AccentGreen = Color(0xFF25D366)
val BoardLight = Color(0xFFE8C99B)
val BoardDark = Color(0xFF8B5A2B)

private val SmChessColorScheme = darkColorScheme(
    primary = AccentGreen,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun SMChessTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SmChessColorScheme,
        content = content
    )
}
