package com.masroofi.app.domain.calculations

import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.utils.FormatUtils
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * محرك الحسابات المالية الدقيقة لتطبيق مصروفي
 * يقوم بحساب المؤشرات المالية السبعة وقواعد الميزانية والتدفق النقدي وتقارير الإحصائيات
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
     * 8. حساب إجمالي الرصيد الصافي المتاح في المحفظة لكافة العمليات المسجلة
     * (إجمالي الإيرادات المسجلة - إجمالي المصروفات المسجلة)
     */
    fun calculateAvailableBalance(transactions: List<TransactionItem>): Long {
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        return totalIncome - totalExpense
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
                    categoryName = first.categoryName ?: "مصروفات أخرى",
                    iconKey = first.categoryIconKey ?: "category",
                    colorHex = "#2A9D8F",
                    totalSpent = totalSpent,
                    percentage = percentage,
                    transactionCount = categoryTransactions.size
                )
            }
            .sortedByDescending { it.totalSpent }
    }

    /**
     * حساب متوسط المصروف اليومي في الشهر المختار
     * يُقسم إجمالي المصروفات على عدد الأيام المنقضية للشهر الحالي، أو إجمالي أيام الشهر للشهر السابق
     * مع الحماية التامة من القسمة على صفر
     */
    fun calculateAverageDailyExpense(
        yearMonth: YearMonth,
        monthlyExpenses: Long,
        currentDate: LocalDate = LocalDate.now()
    ): Long {
        if (monthlyExpenses <= 0L) return 0L
        val currentYearMonth = YearMonth.from(currentDate)
        val elapsedDays = when {
            yearMonth == currentYearMonth -> currentDate.dayOfMonth
            yearMonth.isBefore(currentYearMonth) -> yearMonth.lengthOfMonth()
            else -> yearMonth.lengthOfMonth()
        }
        if (elapsedDays <= 0) return 0L
        return (monthlyExpenses.toDouble() / elapsedDays.toDouble()).roundToLong()
    }

    /**
     * حساب توزيع المصروفات عبر أسابيع الشهر الأربعة
     */
    fun calculateWeeklySpending(
        yearMonth: YearMonth,
        transactions: List<TransactionItem>,
        totalMonthlyExpenses: Long
    ): List<WeeklySpendingItem> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val daysInMonth = yearMonth.lengthOfMonth()

        // الفترات الأربع في الشهر: (1-7)، (8-14)، (15-21)، (22-نهاية الشهر)
        val periods = listOf(
            Triple("الأسبوع 1", "1 - 7", 1..7),
            Triple("الأسبوع 2", "8 - 14", 8..14),
            Triple("الأسبوع 3", "15 - 21", 15..21),
            Triple("الأسبوع 4+", "22 - $daysInMonth", 22..daysInMonth)
        )

        return periods.map { (name, label, dayRange) ->
            val periodAmount = expenses.filter { transaction ->
                val day = try {
                    LocalDate.parse(transaction.transactionDate).dayOfMonth
                } catch (_: Exception) {
                    1
                }
                day in dayRange
            }.sumOf { it.amount }

            val percentage = if (totalMonthlyExpenses > 0L) {
                ((periodAmount.toDouble() / totalMonthlyExpenses.toDouble()) * 100.0).roundToInt()
            } else 0

            WeeklySpendingItem(
                periodName = name,
                daysLabel = label,
                amount = periodAmount,
                percentage = percentage
            )
        }
    }

    /**
     * حساب مقارنة المصروفات عبر الأشهر (آخر 6 أشهر تسبق الشهر المختار أو تنتهي به)
     * مع نسبة التغير مقارنة بالشهر السابق مباشرة
     */
    fun calculateMonthlyComparisons(
        selectedMonth: YearMonth,
        allTransactions: List<TransactionItem>,
        monthsCount: Int = 6
    ): Pair<List<MonthlyComparisonItem>, Int?> {
        val expenses = allTransactions.filter { it.type == TransactionType.EXPENSE }
        val months = (monthsCount - 1 downTo 0).map { selectedMonth.minusMonths(it.toLong()) }

        val comparisonItems = months.map { ym ->
            val prefix = ym.toString()
            val monthAmount = expenses
                .filter { it.transactionDate.startsWith(prefix) }
                .sumOf { it.amount }

            MonthlyComparisonItem(
                yearMonth = ym,
                monthLabel = FormatUtils.formatMonthName(ym.monthValue),
                amount = monthAmount,
                isCurrentSelected = (ym == selectedMonth)
            )
        }

        // حساب نسبة التغير مقارنة بالشهر السابق مباشرة
        val prevMonth = selectedMonth.minusMonths(1)
        val prevPrefix = prevMonth.toString()
        val prevAmount = expenses
            .filter { it.transactionDate.startsWith(prevPrefix) }
            .sumOf { it.amount }

        val currentAmount = expenses
            .filter { it.transactionDate.startsWith(selectedMonth.toString()) }
            .sumOf { it.amount }

        val changePercentage: Int? = when {
            prevAmount > 0L -> {
                val diff = currentAmount - prevAmount
                ((diff.toDouble() / prevAmount.toDouble()) * 100.0).roundToInt()
            }
            prevAmount == 0L && currentAmount > 0L -> 100
            else -> 0
        }

        return Pair(comparisonItems, changePercentage)
    }

    /**
     * بناء تقرير الإحصائيات المالي الشامل للشهر المختار
     */
    fun buildMonthlyReport(
        selectedMonth: YearMonth,
        monthTransactions: List<TransactionItem>,
        allTransactions: List<TransactionItem>,
        currentDate: LocalDate = LocalDate.now()
    ): MonthlyReportData {
        val totalExpenses = calculateMonthlyExpenses(monthTransactions)
        val totalIncome = calculateMonthlyIncome(monthTransactions)
        val netCashflow = calculateNetCashflow(totalIncome, totalExpenses)
        val averageDaily = calculateAverageDailyExpense(selectedMonth, totalExpenses, currentDate)
        val averageWeekly = if (totalExpenses > 0L) (totalExpenses.toDouble() / 4.0).roundToLong() else 0L

        val categoryBreakdown = calculateTopCategories(monthTransactions, totalExpenses)
        val weeklySpending = calculateWeeklySpending(selectedMonth, monthTransactions, totalExpenses)
        val (monthlyComparisons, changePercentage) = calculateMonthlyComparisons(selectedMonth, allTransactions)

        val peakPeriod = weeklySpending.maxByOrNull { it.amount }?.let {
            if (it.amount > 0L) it.periodName else null
        }

        return MonthlyReportData(
            yearMonth = selectedMonth,
            totalExpenses = totalExpenses,
            totalIncome = totalIncome,
            netCashflow = netCashflow,
            averageDailyExpense = averageDaily,
            averageWeeklyExpense = averageWeekly,
            categoryBreakdown = categoryBreakdown,
            weeklySpendingList = weeklySpending,
            peakSpendingPeriod = peakPeriod,
            monthlyComparisons = monthlyComparisons,
            monthChangePercentage = changePercentage,
            hasExpenses = totalExpenses > 0L
        )
    }

    /**
     * تصفية العمليات المالية بدقة بين تاريخين (شاملة يوم البداية والنهاية)
     */
    fun filterTransactionsByDateRange(
        transactions: List<TransactionItem>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TransactionItem> {
        return transactions.filter { tx ->
            try {
                val date = LocalDate.parse(tx.transactionDate)
                !date.isBefore(startDate) && !date.isAfter(endDate)
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * حساب إجمالي المصروفات لفترة زمنية محددة بين تاريخين
     */
    fun calculateExpensesForRange(
        transactions: List<TransactionItem>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Long {
        return filterTransactionsByDateRange(transactions, startDate, endDate)
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    }

    /**
     * حساب إجمالي الإيرادات لفترة زمنية محددة بين تاريخين
     */
    fun calculateIncomeForRange(
        transactions: List<TransactionItem>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Long {
        return filterTransactionsByDateRange(transactions, startDate, endDate)
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    }

    /**
     * بناء تقرير مالي وإحصائي لفترة زمنية مرنة (يوم محدد، أسبوع، أو فترة مخصصة)
     */
    fun buildPeriodReport(
        startDate: LocalDate,
        endDate: LocalDate,
        periodTransactions: List<TransactionItem>,
        allTransactions: List<TransactionItem> = emptyList(),
        currentDate: LocalDate = LocalDate.now()
    ): MonthlyReportData {
        val totalExpenses = calculateMonthlyExpenses(periodTransactions)
        val totalIncome = calculateMonthlyIncome(periodTransactions)
        val netCashflow = calculateNetCashflow(totalIncome, totalExpenses)

        val daysCount = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
        val averageDaily = if (totalExpenses > 0L && daysCount > 0) {
            (totalExpenses.toDouble() / daysCount.toDouble()).roundToLong()
        } else 0L

        val categoryBreakdown = calculateTopCategories(periodTransactions, totalExpenses)

        return MonthlyReportData(
            yearMonth = YearMonth.from(startDate),
            totalExpenses = totalExpenses,
            totalIncome = totalIncome,
            netCashflow = netCashflow,
            averageDailyExpense = averageDaily,
            averageWeeklyExpense = if (totalExpenses > 0L) (totalExpenses.toDouble() / 4.0).roundToLong() else 0L,
            categoryBreakdown = categoryBreakdown,
            weeklySpendingList = emptyList(),
            peakSpendingPeriod = null,
            monthlyComparisons = emptyList(),
            monthChangePercentage = null,
            hasExpenses = totalExpenses > 0L
        )
    }
}
