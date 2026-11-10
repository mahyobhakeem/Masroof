package com.masroofi.app.domain

import com.masroofi.app.domain.calculations.FinancialCalculator
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * اختبارات وحدة شاملة لمحرك الحسابات المالية (FinancialCalculatorTest)
 * التحقق من القواعد المالية السبعة وحالات تجاوز الميزانية والوقاية من القسمة على صفر
 */
class FinancialCalculatorTest {

    private val sampleFoodCategory = Category(
        id = "cat_food",
        name = "طعام ومشروبات",
        type = TransactionType.EXPENSE,
        iconKey = "fastfood"
    )

    private val sampleBillsCategory = Category(
        id = "cat_bills",
        name = "فواتير وخدمات",
        type = TransactionType.EXPENSE,
        iconKey = "receipt"
    )

    private val sampleSalaryCategory = Category(
        id = "cat_salary",
        name = "راتب شهري",
        type = TransactionType.INCOME,
        iconKey = "payments"
    )

    @Test
    fun `test standard case - budget 100k, expenses 35k, income 80k`() {
        val today = LocalDate.of(2026, 11, 10)
        val transactions = listOf(
            TransactionItem(
                id = "tx1",
                type = TransactionType.EXPENSE,
                amount = 20000,
                categoryId = sampleFoodCategory.id,
                categoryName = sampleFoodCategory.name,
                categoryIconKey = sampleFoodCategory.iconKey,
                transactionDate = "2026-11-10"
            ),
            TransactionItem(
                id = "tx2",
                type = TransactionType.EXPENSE,
                amount = 15000,
                categoryId = sampleBillsCategory.id,
                categoryName = sampleBillsCategory.name,
                categoryIconKey = sampleBillsCategory.iconKey,
                transactionDate = "2026-11-05"
            ),
            TransactionItem(
                id = "tx3",
                type = TransactionType.INCOME,
                amount = 80000,
                categoryId = sampleSalaryCategory.id,
                categoryName = sampleSalaryCategory.name,
                categoryIconKey = sampleSalaryCategory.iconKey,
                transactionDate = "2026-11-01"
            )
        )

        val summary = FinancialCalculator.buildFinancialSummary(
            yearMonth = YearMonth.of(2026, 11),
            transactions = transactions,
            budgetAmount = 100000L,
            targetDate = today
        )

        assertEquals("إجمالي المصروفات الشهرية يجب أن يكون 35,000", 35000L, summary.totalMonthlyExpenses)
        assertEquals("إجمالي الإيرادات الشهرية يجب أن يكون 80,000", 80000L, summary.totalMonthlyIncome)
        assertEquals("مصروفات اليوم يجب أن تكون 20,000", 20000L, summary.totalDailyExpenses)
        assertEquals("الميزانية يجب أن تكون 100,000", 100000L, summary.monthlyBudget)
        assertEquals("المتبقي من الميزانية يجب أن يكون 65,000", 65000L, summary.remainingBudget)
        assertFalse("يجب ألا تكون الميزانية متجاوزة", summary.isBudgetExceeded)
        assertEquals("مبلغ التجاوز يجب أن يكون 0", 0L, summary.budgetExcessAmount)
        assertEquals("نسبة الاستهلاك يجب أن تكون 35%", 35, summary.consumptionPercentage)
        assertEquals("صافي التدفق المالي يجب أن يكون 45,000", 45000L, summary.netCashflow)
    }

    @Test
    fun `test budget exceeded case - budget 50k, expenses 65k`() {
        val transactions = listOf(
            TransactionItem(
                id = "tx1",
                type = TransactionType.EXPENSE,
                amount = 65000,
                categoryId = sampleFoodCategory.id,
                categoryName = sampleFoodCategory.name,
                categoryIconKey = sampleFoodCategory.iconKey,
                transactionDate = "2026-11-08"
            )
        )

        val summary = FinancialCalculator.buildFinancialSummary(
            yearMonth = YearMonth.of(2026, 11),
            transactions = transactions,
            budgetAmount = 50000L
        )

        assertEquals("المصروفات 65,000", 65000L, summary.totalMonthlyExpenses)
        assertTrue("يجب أن تظهر حالة تجاوز الميزانية", summary.isBudgetExceeded)
        assertEquals("مبلغ التجاوز يجب أن يكون 15,000 ريال", 15000L, summary.budgetExcessAmount)
        assertEquals("المتبقي يجب أن يكون 0 عند التجاوز", 0L, summary.remainingBudget)
        assertEquals("نسبة الاستهلاك يجب أن تكون 130%", 130, summary.consumptionPercentage)
    }

    @Test
    fun `test no budget defined case`() {
        val transactions = listOf(
            TransactionItem(
                id = "tx1",
                type = TransactionType.EXPENSE,
                amount = 25000,
                categoryId = sampleFoodCategory.id,
                categoryName = sampleFoodCategory.name,
                categoryIconKey = sampleFoodCategory.iconKey,
                transactionDate = "2026-11-05"
            )
        )

        val summary = FinancialCalculator.buildFinancialSummary(
            yearMonth = YearMonth.of(2026, 11),
            transactions = transactions,
            budgetAmount = null
        )

        assertNull("الميزانية يجب أن تكون null", summary.monthlyBudget)
        assertNull("المتبقي يجب أن يكون null عند عدم وجود ميزانية", summary.remainingBudget)
        assertFalse("لا يعتبر متجاوزاً عند عدم وجود ميزانية", summary.isBudgetExceeded)
        assertEquals("مبلغ التجاوز 0", 0L, summary.budgetExcessAmount)
        assertEquals("نسبة الاستهلاك 0%", 0, summary.consumptionPercentage)
        assertEquals("المصروفات 25,000", 25000L, summary.totalMonthlyExpenses)
    }

    @Test
    fun `test empty month without transactions - safe from division by zero`() {
        val summary = FinancialCalculator.buildFinancialSummary(
            yearMonth = YearMonth.of(2026, 11),
            transactions = emptyList(),
            budgetAmount = 50000L
        )

        assertEquals(0L, summary.totalMonthlyExpenses)
        assertEquals(0L, summary.totalMonthlyIncome)
        assertEquals(0L, summary.totalDailyExpenses)
        assertEquals(50000L, summary.remainingBudget)
        assertEquals(0, summary.consumptionPercentage)
        assertEquals(0L, summary.netCashflow)
        assertFalse(summary.isBudgetExceeded)
    }

    @Test
    fun `test top categories aggregation and descending order`() {
        val transactions = listOf(
            TransactionItem(
                id = "tx1",
                type = TransactionType.EXPENSE,
                amount = 20000,
                categoryId = sampleFoodCategory.id,
                categoryName = sampleFoodCategory.name,
                categoryIconKey = sampleFoodCategory.iconKey,
                transactionDate = "2026-11-02"
            ),
            TransactionItem(
                id = "tx2",
                type = TransactionType.EXPENSE,
                amount = 30000,
                categoryId = sampleBillsCategory.id,
                categoryName = sampleBillsCategory.name,
                categoryIconKey = sampleBillsCategory.iconKey,
                transactionDate = "2026-11-04"
            ),
            TransactionItem(
                id = "tx3",
                type = TransactionType.INCOME,
                amount = 50000,
                categoryId = sampleSalaryCategory.id,
                categoryName = sampleSalaryCategory.name,
                categoryIconKey = sampleSalaryCategory.iconKey,
                transactionDate = "2026-11-01"
            )
        )

        val topCategories = FinancialCalculator.calculateTopCategories(
            transactions = transactions,
            totalMonthlyExpenses = 50000L
        )

        assertEquals("يجب أن يحتوي على تصنيفين فقط للمصروفات", 2, topCategories.size)
        assertEquals("التصنيف الأول يجب أن يكون الفواتير (30,000)", sampleBillsCategory.id, topCategories[0].categoryId)
        assertEquals(30000L, topCategories[0].totalSpent)
        assertEquals("نسبة الفواتير 60%", 60, topCategories[0].percentage)

        assertEquals("التصنيف الثاني يجب أن يكون الطعام (20,000)", sampleFoodCategory.id, topCategories[1].categoryId)
        assertEquals(20000L, topCategories[1].totalSpent)
        assertEquals("نسبة الطعام 40%", 40, topCategories[1].percentage)
    }

    @Test
    fun `test income does not affect remaining budget calculation`() {
        // التحقق من القاعدة الصارمة: المتبقي من الميزانية = الميزانية - المصروفات (دون خصم الإيرادات)
        val budget = 100000L
        val expenses = 40000L
        val income = 90000L

        val remaining = FinancialCalculator.calculateRemainingBudget(budget, expenses)
        val netFlow = FinancialCalculator.calculateNetCashflow(income, expenses)

        assertEquals("المتبقي يحسب فقط من المصروفات (100k - 40k = 60k)", 60000L, remaining)
        assertEquals("صافي التدفق يحسب بشكل مستقل (90k - 40k = 50k)", 50000L, netFlow)
    }

    @Test
    fun `test average daily expense calculation`() {
        // 1. الشهر الحالي: القسمة على عدد الأيام المنقضية (10 أيام مثلاً)
        val currentMonth = YearMonth.of(2026, 11)
        val currentDate = LocalDate.of(2026, 11, 10)
        val avgCurrent = FinancialCalculator.calculateAverageDailyExpense(
            yearMonth = currentMonth,
            monthlyExpenses = 50000L,
            currentDate = currentDate
        )
        assertEquals("50,000 / 10 = 5,000 ريال", 5000L, avgCurrent)

        // 2. شهر سابق: القسمة على إجمالي أيام الشهر (أكتوبر 31 يوماً)
        val pastMonth = YearMonth.of(2026, 10)
        val avgPast = FinancialCalculator.calculateAverageDailyExpense(
            yearMonth = pastMonth,
            monthlyExpenses = 62000L,
            currentDate = currentDate
        )
        assertEquals("62,000 / 31 = 2,000 ريال", 2000L, avgPast)

        // 3. حالة الصفر: لا توجد مصروفات
        val avgZero = FinancialCalculator.calculateAverageDailyExpense(
            yearMonth = currentMonth,
            monthlyExpenses = 0L,
            currentDate = currentDate
        )
        assertEquals("المتوسط صفر عند انعدام المصروفات", 0L, avgZero)
    }

    @Test
    fun `test weekly spending distribution across 4 periods`() {
        val month = YearMonth.of(2026, 11)
        val transactions = listOf(
            TransactionItem("t1", TransactionType.EXPENSE, 10000, sampleFoodCategory.id, transactionDate = "2026-11-03"), // الأسبوع 1
            TransactionItem("t2", TransactionType.EXPENSE, 20000, sampleFoodCategory.id, transactionDate = "2026-11-06"), // الأسبوع 1
            TransactionItem("t3", TransactionType.EXPENSE, 15000, sampleBillsCategory.id, transactionDate = "2026-11-10"), // الأسبوع 2
            TransactionItem("t4", TransactionType.EXPENSE, 25000, sampleBillsCategory.id, transactionDate = "2026-11-18"), // الأسبوع 3
            TransactionItem("t5", TransactionType.EXPENSE, 30000, sampleBillsCategory.id, transactionDate = "2026-11-25")  // الأسبوع 4+
        )
        val totalExpenses = 100000L

        val weeklyList = FinancialCalculator.calculateWeeklySpending(month, transactions, totalExpenses)

        assertEquals(4, weeklyList.size)
        assertEquals("الأسبوع 1: 30,000", 30000L, weeklyList[0].amount)
        assertEquals(30, weeklyList[0].percentage)

        assertEquals("الأسبوع 2: 15,000", 15000L, weeklyList[1].amount)
        assertEquals(15, weeklyList[1].percentage)

        assertEquals("الأسبوع 3: 25,000", 25000L, weeklyList[2].amount)
        assertEquals(25, weeklyList[2].percentage)

        assertEquals("الأسبوع 4+: 30,000", 30000L, weeklyList[3].amount)
        assertEquals(30, weeklyList[3].percentage)
    }

    @Test
    fun `test monthly comparison and percentage change`() {
        val selectedMonth = YearMonth.of(2026, 11)
        val allTransactions = listOf(
            TransactionItem("t1", TransactionType.EXPENSE, 40000, sampleFoodCategory.id, transactionDate = "2026-10-15"),
            TransactionItem("t2", TransactionType.EXPENSE, 60000, sampleFoodCategory.id, transactionDate = "2026-11-05")
        )

        val (comparisons, changePercentage) = FinancialCalculator.calculateMonthlyComparisons(
            selectedMonth = selectedMonth,
            allTransactions = allTransactions,
            monthsCount = 6
        )

        assertEquals("يجب أن يقارن آخر 6 أشهر", 6, comparisons.size)
        assertEquals("نوفمبر", comparisons.last().monthLabel)
        assertEquals(60000L, comparisons.last().amount)
        assertTrue("الشهر المختار يجب تمييزه", comparisons.last().isCurrentSelected)

        assertEquals("أكتوبر", comparisons[comparisons.size - 2].monthLabel)
        assertEquals(40000L, comparisons[comparisons.size - 2].amount)

        // نسبة التغير من 40,000 إلى 60,000 هي: ((60k - 40k) / 40k) * 100 = +50%
        assertEquals(50, changePercentage)
    }
}
