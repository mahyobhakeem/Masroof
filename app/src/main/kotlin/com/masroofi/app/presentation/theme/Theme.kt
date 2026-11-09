package com.masroofi.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// نظام ألوان الوضع الداكن
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    secondary = IncomeGreen,
    tertiary = ExpenseRed,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = DarkOutline,
    surfaceVariant = Color(0xFF26354A),
    onSurfaceVariant = Color(0xFFD0D5DD)
)

// نظام ألوان الوضع الفاتح
private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    secondary = IncomeGreen,
    tertiary = ExpenseRed,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1D2939),
    onSurface = Color(0xFF1D2939),
    outline = LightOutline,
    surfaceVariant = Color(0xFFEAECF0),
    onSurfaceVariant = TextSecondary
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
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
