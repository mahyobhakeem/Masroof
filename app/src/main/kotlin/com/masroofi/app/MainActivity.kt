package com.masroofi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.masroofi.app.ui.navigation.MasroofiApp
import com.masroofi.app.ui.theme.MasroofiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemInDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemInDark) }

            MasroofiTheme(darkTheme = isDarkMode) {
                // إجبار اتجاه الواجهة بالكامل لدعم اللغة العربية وتخطيط RTL
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MasroofiApp(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { isDarkMode = it }
                    )
                }
            }
        }
    }
}
