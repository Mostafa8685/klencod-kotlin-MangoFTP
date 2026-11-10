package com.mango.ftp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MangoOrange,
    onPrimary = MangoCreamSurface,
    primaryContainer = MangoCardBg,
    onPrimaryContainer = MangoTextDark,
    secondary = MangoAmber,
    onSecondary = MangoTextDark,
    tertiary = MangoLeafGreen,
    background = MangoCreamSurface,
    surface = MangoCreamSurface,
    onBackground = MangoTextDark,
    onSurface = MangoTextDark,
    surfaceVariant = MangoCardBg,
    onSurfaceVariant = MangoTextMedium,
    outline = MangoCardStroke
)

private val DarkColorScheme = darkColorScheme(
    primary = MangoOrangeLight,
    onPrimary = MangoDarkSurface,
    primaryContainer = MangoDarkCardBg,
    onPrimaryContainer = MangoDarkText,
    secondary = MangoGold,
    onSecondary = MangoDarkSurface,
    tertiary = MangoLeafGreenLight,
    background = MangoDarkSurface,
    surface = MangoDarkSurface,
    onBackground = MangoDarkText,
    onSurface = MangoDarkText,
    surfaceVariant = MangoDarkCardBg,
    onSurfaceVariant = MangoTextLight,
    outline = MangoDarkCardStroke
)

@Composable
fun MangoFTPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
