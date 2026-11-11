package com.masroofi.app.domain.calculations

import androidx.compose.runtime.Immutable
import java.time.YearMonth

/**
 * بيانات الإنفاق لفترة أسبوعية محددة خلال الشهر
 */
@Immutable
data class WeeklySpendingItem(
    val periodName: String, // مثال: "الأسبوع 1"
    val daysLabel: String,  // مثال: "1 - 7"
    val amount: Long = 0L,
    val percentage: Int = 0
)

/**
 * بيانات مقارنة الإنفاق عبر الأشهر (آخر 6 أشهر)
 */
@Immutable
data class MonthlyComparisonItem(
    val yearMonth: YearMonth,
    val monthLabel: String, // مثال: "نوفمبر"
    val amount: Long = 0L,
    val isCurrentSelected: Boolean = false
)

/**
 * التقرير المالي والإحصائي الشامل للشهر المختار في شاشة التقارير
 */
@Immutable
data class MonthlyReportData(
    val yearMonth: YearMonth,
    val totalExpenses: Long = 0L,
    val totalIncome: Long = 0L,
    val netCashflow: Long = 0L,
    val averageDailyExpense: Long = 0L,
    val averageWeeklyExpense: Long = 0L,
    val categoryBreakdown: List<CategoryExpenseSummary> = emptyList(),
    val weeklySpendingList: List<WeeklySpendingItem> = emptyList(),
    val peakSpendingPeriod: String? = null,
    val monthlyComparisons: List<MonthlyComparisonItem> = emptyList(),
    val monthChangePercentage: Int? = null,
    val hasExpenses: Boolean = false
)
