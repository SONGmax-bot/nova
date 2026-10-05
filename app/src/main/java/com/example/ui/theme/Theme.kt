package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NovaPrimaryLight,
    onPrimary = NovaNavyDark,
    primaryContainer = NovaNavySurface,
    onPrimaryContainer = NovaPrimaryLight,
    secondary = NovaSecondary,
    onSecondary = Color.White,
    secondaryContainer = NovaNavyCard,
    onSecondaryContainer = Color(0xFFC7D2FE),
    tertiary = NovaAccentGold,
    background = NovaDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = NovaDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = NovaDarkCard,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = NovaDarkBorder,
    error = NovaErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = NovaPrimary,
    onPrimary = Color.White,
    primaryContainer = NovaPrimaryContainer,
    onPrimaryContainer = NovaOnPrimaryContainer,
    secondary = NovaSecondary,
    onSecondary = Color.White,
    secondaryContainer = NovaSecondaryContainer,
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = NovaAccentGold,
    background = NovaLightBg,
    onBackground = Color(0xFF0F172A),
    surface = NovaLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = NovaLightCard,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE2E8F0),
    error = NovaErrorRed
)

@Composable
fun NovaStoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use customized NOVA brand colors for distinctive identity
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
