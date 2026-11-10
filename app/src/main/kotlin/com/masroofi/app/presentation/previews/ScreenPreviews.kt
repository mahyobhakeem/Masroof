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
import com.masroofi.app.domain.calculations.MonthlyComparisonItem
import com.masroofi.app.domain.calculations.MonthlyReportData
import com.masroofi.app.domain.calculations.WeeklySpendingItem
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.presentation.screens.home.HomeScreenContent
import com.masroofi.app.presentation.screens.home.HomeUiState
import com.masroofi.app.presentation.screens.reports.ReportsContent
import com.masroofi.app.presentation.screens.reports.ReportsUiState
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
                        allCategories = sampleCategories,
                        availableFilterCategories = sampleCategories
                    ),
                    onSearchQueryChange = {},
                    onFilterIndexChange = {},
                    onCategorySelect = {},
                    onDatePeriodSelect = {},
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
                        allCategories = sampleCategories,
                        availableFilterCategories = sampleCategories
                    ),
                    onSearchQueryChange = {},
                    onFilterIndexChange = {},
                    onCategorySelect = {},
                    onDatePeriodSelect = {},
                    onResetFilters = {},
                    onEditTransaction = {},
                    onDeleteTransaction = {}
                )
            }
        }
    }
}

private val sampleReportsUiState = ReportsUiState(
    selectedYearMonth = YearMonth.of(2026, 11),
    reportData = MonthlyReportData(
        yearMonth = YearMonth.of(2026, 11),
        totalExpenses = 48000L,
        totalIncome = 105000L,
        netCashflow = 57000L,
        averageDailyExpense = 4800L,
        averageWeeklyExpense = 12000L,
        categoryBreakdown = listOf(
            CategoryExpenseSummary("cat_exp_food", "الطعام والمشروبات", "fastfood", "#E76F51", 21500L, 45, 3),
            CategoryExpenseSummary("cat_exp_shopping", "التسوق والملابس", "shopping_bag", "#F4A261", 12000L, 25, 2),
            CategoryExpenseSummary("cat_exp_bills", "الفواتير والخدمات", "receipt", "#2A9D8F", 11000L, 23, 1),
            CategoryExpenseSummary("cat_exp_other", "مصروفات أخرى", "more_horiz", "#6C757D", 3500L, 7, 1)
        ),
        weeklySpendingList = listOf(
            WeeklySpendingItem("الأسبوع 1", "1 - 7", 23000L, 48),
            WeeklySpendingItem("الأسبوع 2", "8 - 14", 25000L, 52),
            WeeklySpendingItem("الأسبوع 3", "15 - 21", 0L, 0),
            WeeklySpendingItem("الأسبوع 4+", "22 - 30", 0L, 0)
        ),
        peakSpendingPeriod = "الأسبوع 2",
        monthlyComparisons = listOf(
            MonthlyComparisonItem(YearMonth.of(2026, 6), "يونيو", 35000L),
            MonthlyComparisonItem(YearMonth.of(2026, 7), "يوليو", 42000L),
            MonthlyComparisonItem(YearMonth.of(2026, 8), "أغسطس", 38000L),
            MonthlyComparisonItem(YearMonth.of(2026, 9), "سبتمبر", 50000L),
            MonthlyComparisonItem(YearMonth.of(2026, 10), "أكتوبر", 40000L),
            MonthlyComparisonItem(YearMonth.of(2026, 11), "نوفمبر", 48000L, isCurrentSelected = true)
        ),
        monthChangePercentage = 20,
        hasExpenses = true
    )
)

// 4. معاينة شاشة التقارير (الوضع الفاتح)
@Preview(name = "4. التقارير - وضع فاتح", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun ReportsScreenLightPreview() {
    MasroofiTheme(darkTheme = false) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                ReportsContent(
                    uiState = sampleReportsUiState,
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onCategorySelect = {}
                )
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
                ReportsContent(
                    uiState = sampleReportsUiState,
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onCategorySelect = {}
                )
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
