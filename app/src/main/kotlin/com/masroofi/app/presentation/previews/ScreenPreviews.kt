package com.masroofi.app.presentation.previews

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.masroofi.app.domain.calculations.CategoryExpenseSummary
import com.masroofi.app.domain.calculations.FinancialSummary
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.presentation.screens.home.HomeScreenContent
import com.masroofi.app.presentation.screens.home.HomeUiState
import com.masroofi.app.presentation.screens.reports.ReportsScreen
import com.masroofi.app.presentation.screens.settings.SettingsScreen
import com.masroofi.app.presentation.screens.transactions.AddTransactionContent
import com.masroofi.app.presentation.screens.transactions.TransactionFormUiState
import com.masroofi.app.presentation.screens.transactions.TransactionsHistoryContent
import com.masroofi.app.presentation.screens.transactions.TransactionsHistoryUiState
import com.masroofi.app.presentation.theme.MasroofiTheme
import java.time.LocalDate
import java.time.YearMonth

private val sampleCategories = listOf(
    Category("1", "طعام ومشروبات", TransactionType.EXPENSE, "fastfood"),
    Category("2", "فواتير وخدمات", TransactionType.EXPENSE, "receipt"),
    Category("3", "مواصلات ووقود", TransactionType.EXPENSE, "directions_car")
)

private val sampleTransactions = listOf(
    TransactionItem(
        id = "1",
        type = TransactionType.EXPENSE,
        amount = 2500,
        categoryId = "1",
        categoryName = "طعام ومشروبات",
        categoryIconKey = "fastfood",
        transactionDate = "2026-11-10",
        note = "وجبة غداء"
    ),
    TransactionItem(
        id = "2",
        type = TransactionType.EXPENSE,
        amount = 6000,
        categoryId = "2",
        categoryName = "فواتير وخدمات",
        categoryIconKey = "receipt",
        transactionDate = "2026-11-09",
        note = "اشتراك إنترنت"
    )
)

private val sampleHomeUiState = HomeUiState(
    selectedMonth = YearMonth.of(2026, 11),
    summary = FinancialSummary(
        yearMonth = YearMonth.of(2026, 11),
        totalDailyExpenses = 2500,
        totalMonthlyExpenses = 35000,
        totalMonthlyIncome = 75000,
        monthlyBudget = 100000,
        remainingBudget = 65000,
        isBudgetExceeded = false,
        consumptionPercentage = 35,
        netCashflow = 40000
    ),
    topCategories = listOf(
        CategoryExpenseSummary("1", "طعام ومشروبات", "fastfood", "#FF6B6B", 15000, 43),
        CategoryExpenseSummary("2", "فواتير وخدمات", "receipt", "#4ECDC4", 12000, 34),
        CategoryExpenseSummary("3", "مواصلات ووقود", "directions_car", "#FFD166", 8000, 23)
    ),
    recentTransactions = sampleTransactions
)

// 1. معاينة الشاشة الرئيسية (الوضع الفاتح)
@Preview(name = "1. الرئيسية - وضع فاتح", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun HomeScreenLightPreview() {
    MasroofiTheme(darkTheme = false) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                HomeScreenContent(
                    uiState = sampleHomeUiState,
                    onNavigateToTransactions = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onOpenBudgetDialog = {},
                    onTransactionClick = {}
                )
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
                HomeScreenContent(
                    uiState = sampleHomeUiState,
                    onNavigateToTransactions = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onOpenBudgetDialog = {},
                    onTransactionClick = {}
                )
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
                AddTransactionContent(
                    uiState = TransactionFormUiState(
                        availableCategories = sampleCategories,
                        selectedCategory = sampleCategories.first(),
                        amount = "5000",
                        transactionDate = LocalDate.now()
                    ),
                    onTypeChange = {},
                    onAmountChange = {},
                    onCategorySelect = {},
                    onDateSelect = {},
                    onNoteChange = {},
                    onSaveClick = {},
                    onDismiss = {}
                )
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
                AddTransactionContent(
                    uiState = TransactionFormUiState(
                        availableCategories = sampleCategories,
                        selectedCategory = sampleCategories.first(),
                        amount = "5000",
                        transactionDate = LocalDate.now()
                    ),
                    onTypeChange = {},
                    onAmountChange = {},
                    onCategorySelect = {},
                    onDateSelect = {},
                    onNoteChange = {},
                    onSaveClick = {},
                    onDismiss = {}
                )
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
                TransactionsHistoryContent(
                    uiState = TransactionsHistoryUiState(
                        transactions = sampleTransactions,
                        categories = sampleCategories
                    ),
                    onSearchQueryChange = {},
                    onFilterIndexChange = {},
                    onCategorySelect = {},
                    onResetFilters = {},
                    onEditTransaction = {},
                    onDeleteTransaction = {}
                )
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
                TransactionsHistoryContent(
                    uiState = TransactionsHistoryUiState(
                        transactions = sampleTransactions,
                        categories = sampleCategories
                    ),
                    onSearchQueryChange = {},
                    onFilterIndexChange = {},
                    onCategorySelect = {},
                    onResetFilters = {},
                    onEditTransaction = {},
                    onDeleteTransaction = {}
                )
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
