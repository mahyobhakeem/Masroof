package com.masroofi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.masroofi.app.data.preferences.ThemeMode
import com.masroofi.app.presentation.components.MasroofSplashScreen
import com.masroofi.app.presentation.navigation.MasroofiApp
import com.masroofi.app.presentation.theme.MasroofiTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as MasroofiApplication
            val themeMode by app.container.preferencesManager.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val systemInDark = isSystemInDarkTheme()

            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemInDark
            }

            var isSplashVisible by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                // إبقاء شاشة الإقلاع لفترة ناعمة تضمن تحميل التفضيلات والبيانات الأولية بسلاسة
                delay(800)
                isSplashVisible = false
            }

            MasroofiTheme(darkTheme = isDarkTheme) {
                // إجبار اتجاه الواجهة بالكامل لدعم اللغة العربية وتخطيط RTL
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MasroofiApp(isDarkMode = isDarkTheme)

                        AnimatedVisibility(
                            visible = isSplashVisible,
                            exit = fadeOut(animationSpec = tween(400))
                        ) {
                            MasroofSplashScreen()
                        }
                    }
                }
            }
        }
    }
}
