package com.masroofi.app.domain.calculations

import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

/**
 * محرك الحسابات المالية الدقيقة لتطبيق مصروفي
 * يقوم بحساب المؤشرات المالية السبعة وقواعد الميزانية والتدفق النقدي
 */
object FinancialCalculator {

    /**
     * 1. حساب إجمالي المصروفات اليومية لتاريخ محدد (اليوم الحالي)
     */
    fun calculateDailyExpenses(
        transactions: List<TransactionItem>,
        targetDate: LocalDate = LocalDate.now()
    ): Long {
        val targetDateStr = targetDate.toString()
        return transactions
            .filter { it.type == TransactionType.EXPENSE && it.transactionDate == targetDateStr }
            .sumOf { it.amount }
    }

    /**
     * 2. حساب إجمالي المصروفات الشهرية للشهر المختار
     */
    fun calculateMonthlyExpenses(transactions: List<TransactionItem>): Long {
        return transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    }

    /**
     * 3. حساب إجمالي الإيرادات الشهرية للشهر المختار
     */
    fun calculateMonthlyIncome(transactions: List<TransactionItem>): Long {
        return transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    }

    /**
     * 4. فحص ما إذا كانت المصروفات الشهرية قد تجاوزت الميزانية
     */
    fun isBudgetExceeded(budget: Long?, monthlyExpenses: Long): Boolean {
        if (budget == null) return false
        return monthlyExpenses > budget
    }

    /**
     * 5. حساب المتبقي من الميزانية (الميزانية الشهرية - مصروفات الشهر)
     * يعود بقيمة فارغة null إذا لم تكن هناك ميزانية محددة، أو صفر إذا تم التجاوز
     */
    fun calculateRemainingBudget(budget: Long?, monthlyExpenses: Long): Long? {
        if (budget == null) return null
        val diff = budget - monthlyExpenses
        return if (diff < 0L) 0L else diff
    }

    /**
     * حساب مبلغ التجاوز إذا تجاوزت المصروفات الميزانية
     */
    fun calculateBudgetExcess(budget: Long?, monthlyExpenses: Long): Long {
        if (budget == null) return 0L
        val diff = monthlyExpenses - budget
        return if (diff > 0L) diff else 0L
    }

    /**
     * 6. حساب نسبة استهلاك الميزانية: (مصروفات الشهر / الميزانية الشهرية) * 100
     * آمن تماماً ضد القسمة على صفر
     */
    fun calculateConsumptionPercentage(budget: Long?, monthlyExpenses: Long): Int {
        if (budget == null || budget <= 0L) return 0
        val percentage = (monthlyExpenses.toDouble() / budget.toDouble()) * 100.0
        return percentage.roundToInt()
    }

    /**
     * 7. حساب صافي التدفق المالي: إيرادات الشهر - مصروفات الشهر
     */
    fun calculateNetCashflow(monthlyIncome: Long, monthlyExpenses: Long): Long {
        return monthlyIncome - monthlyExpenses
    }

    /**
     * تجميع وحساب الملخص المالي الشامل
     */
    fun buildFinancialSummary(
        yearMonth: YearMonth,
        transactions: List<TransactionItem>,
        budgetAmount: Long?,
        targetDate: LocalDate = LocalDate.now()
    ): FinancialSummary {
        val dailyExpenses = calculateDailyExpenses(transactions, targetDate)
        val monthlyExpenses = calculateMonthlyExpenses(transactions)
        val monthlyIncome = calculateMonthlyIncome(transactions)
        val isExceeded = isBudgetExceeded(budgetAmount, monthlyExpenses)
        val remaining = calculateRemainingBudget(budgetAmount, monthlyExpenses)
        val excess = calculateBudgetExcess(budgetAmount, monthlyExpenses)
        val consumption = calculateConsumptionPercentage(budgetAmount, monthlyExpenses)
        val netCashflow = calculateNetCashflow(monthlyIncome, monthlyExpenses)

        return FinancialSummary(
            yearMonth = yearMonth,
            totalDailyExpenses = dailyExpenses,
            totalMonthlyExpenses = monthlyExpenses,
            totalMonthlyIncome = monthlyIncome,
            monthlyBudget = budgetAmount,
            remainingBudget = remaining,
            isBudgetExceeded = isExceeded,
            budgetExcessAmount = excess,
            consumptionPercentage = consumption,
            netCashflow = netCashflow
        )
    }

    /**
     * حساب وترتيب أبرز التصنيفات إنفاقاً للشهر المختار
     */
    fun calculateTopCategories(
        transactions: List<TransactionItem>,
        totalMonthlyExpenses: Long
    ): List<CategoryExpenseSummary> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        if (expenses.isEmpty()) return emptyList()

        return expenses
            .groupBy { it.categoryId }
            .map { (catId, categoryTransactions) ->
                val first = categoryTransactions.first()
                val totalSpent = categoryTransactions.sumOf { it.amount }
                val percentage = if (totalMonthlyExpenses > 0L) {
                    ((totalSpent.toDouble() / totalMonthlyExpenses.toDouble()) * 100.0).roundToInt()
                } else 0

                CategoryExpenseSummary(
                    categoryId = catId,
                    categoryName = first.categoryName ?: "أخرى",
                    iconKey = first.categoryIconKey ?: "category",
                    colorHex = "#2A9D8F",
                    totalSpent = totalSpent,
                    percentage = percentage
                )
            }
            .sortedByDescending { it.totalSpent }
    }
}
