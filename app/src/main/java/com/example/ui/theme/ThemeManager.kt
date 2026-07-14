package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ThemeManager {
    val DarkBackground = Color(0xFF101216)
    val DarkSurface = Color(0xFF1B1F24)
    val DarkPrimary = Color(0xFF2F80ED) 
    val DarkSecondary = Color(0xFF00E676) 
    val DarkTertiary = Color(0xFFF2C94C) 
    val DarkError = Color(0xFFFF4D4D) 

    private val DarkColorScheme = darkColorScheme(
        primary = DarkPrimary,
        secondary = DarkSecondary,
        tertiary = DarkTertiary,
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = Color(0xFF262C35),
        error = DarkError,
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onTertiary = Color.Black,
        onBackground = Color(0xFFF2F2F2),
        onSurface = Color(0xFFF2F2F2),
        errorContainer = Color(0xFF5A1E1E),
        onErrorContainer = Color(0xFFFFD1D1)
    )

    val LightBackground = Color(0xFFF7F9FC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightPrimary = Color(0xFF2F80ED)
    val LightSecondary = Color(0xFF00B0FF)
    val LightTertiary = Color(0xFF828282)
    val LightError = Color(0xFFD32F2F)

    private val LightColorScheme = lightColorScheme(
        primary = LightPrimary,
        secondary = LightSecondary,
        tertiary = LightTertiary,
        background = LightBackground,
        surface = LightSurface,
        surfaceVariant = Color(0xFFEEF2F6),
        error = LightError,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.Black,
        onBackground = Color(0xFF1B1F24),
        onSurface = Color(0xFF1B1F24),
        errorContainer = Color(0xFFFFEBEE),
        onErrorContainer = Color(0xFFC62828)
    )

    @Composable
    fun LockMinderTheme(
        themeMode: String = "SYSTEM", 
        content: @Composable () -> Unit
    ) {
        val darkTheme = when (themeMode) {
            "LIGHT" -> false
            "DARK" -> true
            else -> isSystemInDarkTheme()
        }

        val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
