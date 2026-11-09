package com.masroofi.app.presentation.previews

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.masroofi.app.presentation.screens.home.HomeScreen
import com.masroofi.app.presentation.screens.reports.ReportsScreen
import com.masroofi.app.presentation.screens.settings.SettingsScreen
import com.masroofi.app.presentation.screens.transactions.AddTransactionScreen
import com.masroofi.app.presentation.screens.transactions.TransactionsHistoryScreen
import com.masroofi.app.presentation.theme.MasroofiTheme

// 1. معاينة الشاشة الرئيسية (الوضع الفاتح)
@Preview(name = "1. الرئيسية - وضع فاتح", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun HomeScreenLightPreview() {
    MasroofiTheme(darkTheme = false) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                HomeScreen(onNavigateToTransactions = {})
            }
        }
    }
}

// 1. معاينة الشاشة الرئيسية (الوضع الداكن)
@Preview(name = "1. الرئيسية - وضع داكن", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun HomeScreenDarkPreview() {
    MasroofiTheme(darkTheme = true) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                HomeScreen(onNavigateToTransactions = {})
            }
        }
    }
}

// 2. معاينة شاشة إضافة عملية (الوضع الفاتح)
@Preview(name = "2. إضافة عملية - وضع فاتح", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun AddTransactionScreenLightPreview() {
    MasroofiTheme(darkTheme = false) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AddTransactionScreen(onDismiss = {})
            }
        }
    }
}

// 2. معاينة شاشة إضافة عملية (الوضع الداكن)
@Preview(name = "2. إضافة عملية - وضع داكن", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun AddTransactionScreenDarkPreview() {
    MasroofiTheme(darkTheme = true) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AddTransactionScreen(onDismiss = {})
            }
        }
    }
}

// 3. معاينة شاشة سجل العمليات (الوضع الفاتح)
@Preview(name = "3. سجل العمليات - وضع فاتح", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun TransactionsHistoryScreenLightPreview() {
    MasroofiTheme(darkTheme = false) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                TransactionsHistoryScreen()
            }
        }
    }
}

// 3. معاينة شاشة سجل العمليات (الوضع الداكن)
@Preview(name = "3. سجل العمليات - وضع داكن", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun TransactionsHistoryScreenDarkPreview() {
    MasroofiTheme(darkTheme = true) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                TransactionsHistoryScreen()
            }
        }
    }
}

// 4. معاينة شاشة التقارير (الوضع الفاتح)
@Preview(name = "4. التقارير - وضع فاتح", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun ReportsScreenLightPreview() {
    MasroofiTheme(darkTheme = false) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                ReportsScreen()
            }
        }
    }
}

// 4. معاينة شاشة التقارير (الوضع الداكن)
@Preview(name = "4. التقارير - وضع داكن", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun ReportsScreenDarkPreview() {
    MasroofiTheme(darkTheme = true) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                ReportsScreen()
            }
        }
    }
}

// 5. معاينة شاشة الإعدادات (الوضع الفاتح)
@Preview(name = "5. الإعدادات - وضع فاتح", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun SettingsScreenLightPreview() {
    MasroofiTheme(darkTheme = false) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                SettingsScreen(isDarkMode = false, onToggleDarkMode = {})
            }
        }
    }
}

// 5. معاينة شاشة الإعدادات (الوضع الداكن)
@Preview(name = "5. الإعدادات - وضع داكن", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun SettingsScreenDarkPreview() {
    MasroofiTheme(darkTheme = true) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                SettingsScreen(isDarkMode = true, onToggleDarkMode = {})
            }
        }
    }
}
