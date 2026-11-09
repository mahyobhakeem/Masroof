package com.masroofi.app.ui.theme

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
    primary = PrimaryGreen,
    onPrimary = LightSurface,
    primaryContainer = PrimaryGreen.copy(alpha = 0.12f),
    onPrimaryContainer = PrimaryGreen,
    secondary = IncomeGreen,
    onSecondary = LightSurface,
    error = ExpenseRed,
    onError = LightSurface,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightBackground,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    outlineVariant = BorderLight.copy(alpha = 0.6f)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = DarkBackground,
    primaryContainer = PrimaryGreen.copy(alpha = 0.2f),
    onPrimaryContainer = PrimaryGreen,
    secondary = IncomeGreen,
    onSecondary = DarkBackground,
    error = ExpenseRed,
    onError = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    outlineVariant = BorderDark.copy(alpha = 0.6f)
)

@Composable
fun MasroofiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MasroofiTypography,
        content = content
    )
}
