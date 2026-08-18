package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SophisticatedDarkColorScheme = darkColorScheme(
    primary = AccentLavender,
    onPrimary = OnAccentLavender,
    primaryContainer = AccentLavenderContainer,
    onPrimaryContainer = OnAccentLavenderContainer,
    secondary = AccentLavenderContainer,
    onSecondary = OnAccentLavenderContainer,
    secondaryContainer = AccentLavenderContainer,
    onSecondaryContainer = OnAccentLavenderContainer,
    tertiary = EmeraldGreen,
    onTertiary = SophisticatedBg,
    background = SophisticatedBg,
    onBackground = TextPrimary,
    surface = SophisticatedSurface,
    onSurface = TextPrimary,
    surfaceVariant = SophisticatedSurface,
    onSurfaceVariant = TextSecondary,
    outline = SophisticatedBorder,
    outlineVariant = SophisticatedBorderSubtle,
    error = CrimsonGlow,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = SophisticatedDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SophisticatedBg.toArgb()
            window.navigationBarColor = SophisticatedNavBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

