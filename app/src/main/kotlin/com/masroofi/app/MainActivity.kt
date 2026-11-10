package com.masroofi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.masroofi.app.data.preferences.ThemeMode
import com.masroofi.app.presentation.navigation.MasroofiApp
import com.masroofi.app.presentation.theme.MasroofiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as MasroofiApplication
            val themeMode by app.container.preferencesManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val systemInDark = isSystemInDarkTheme()

            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemInDark
            }

            MasroofiTheme(darkTheme = isDarkTheme) {
                // إجبار اتجاه الواجهة بالكامل لدعم اللغة العربية وتخطيط RTL
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MasroofiApp(isDarkMode = isDarkTheme)
                }
            }
        }
    }
}
