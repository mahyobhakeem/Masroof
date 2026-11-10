package com.masroofi.app.domain.calculations

import java.time.YearMonth

/**
 * نموذج ملخص المؤشرات المالية للشهر المختار في تطبيق مصروفي
 */
data class FinancialSummary(
    val yearMonth: YearMonth,
    val totalDailyExpenses: Long = 0L,
    val totalMonthlyExpenses: Long = 0L,
    val totalMonthlyIncome: Long = 0L,
    val monthlyBudget: Long? = null,
    val remainingBudget: Long? = null,
    val isBudgetExceeded: Boolean = false,
    val budgetExcessAmount: Long = 0L,
    val consumptionPercentage: Int = 0,
    val netCashflow: Long = 0L
)
