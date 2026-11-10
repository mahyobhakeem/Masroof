package com.masroofi.app.presentation

import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.TransactionRepository
import com.masroofi.app.domain.usecase.GetMonthlyReportUseCase
import com.masroofi.app.presentation.screens.reports.ReportsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * اختبارات وحدة شاملة لنموذج عرض شاشة التقارير والإحصائيات (ReportsViewModelTest)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTransactionRepository: FakeReportsTxRepo
    private lateinit var getMonthlyReportUseCase: GetMonthlyReportUseCase
    private lateinit var viewModel: ReportsViewModel

    private val november = YearMonth.of(2026, 11)
    private val october = YearMonth.of(2026, 10)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTransactionRepository = FakeReportsTxRepo()

        val initialTransactions = listOf(
            // معاملات شهر نوفمبر 2026
            TransactionItem("tx1", TransactionType.EXPENSE, 20000, "cat_food", "طعام ومشروبات", "fastfood", "2026-11-02"),
            TransactionItem("tx2", TransactionType.EXPENSE, 15000, "cat_bills", "فواتير", "receipt", "2026-11-09"),
            TransactionItem("tx3", TransactionType.EXPENSE, 10000, "cat_transport", "مواصلات", "directions_car", "2026-11-16"),
            TransactionItem("tx4", TransactionType.INCOME, 80000, "cat_salary", "الراتب", "payments", "2026-11-01"),

            // معاملات شهر أكتوبر 2026
            TransactionItem("tx5", TransactionType.EXPENSE, 30000, "cat_food", "طعام ومشروبات", "fastfood", "2026-10-15"),
            TransactionItem("tx6", TransactionType.INCOME, 80000, "cat_salary", "الراتب", "payments", "2026-10-01")
        )
        fakeTransactionRepository.setTransactions(initialTransactions)

        getMonthlyReportUseCase = GetMonthlyReportUseCase(fakeTransactionRepository)
        viewModel = ReportsViewModel(getMonthlyReportUseCase)
        viewModel.setSelectedYearMonth(november)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state loads november report correctly`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        val report = state.reportData

        assertEquals(november, state.selectedYearMonth)
        assertEquals("إجمالي المصروفات لشهر نوفمبر: 45,000 ريال", 45000L, report.totalExpenses)
        assertEquals("إجمالي الإيرادات لشهر نوفمبر: 80,000 ريال", 80000L, report.totalIncome)
        assertEquals("صافي التدفق المالي: 35,000 ريال", 35000L, report.netCashflow)
        assertTrue("توجد مصروفات مسجلة", report.hasExpenses)
        assertEquals(3, report.categoryBreakdown.size)
        assertEquals("طعام ومشروبات", report.categoryBreakdown.first().categoryName)
        assertEquals(20000L, report.categoryBreakdown.first().totalSpent)
    }

    @Test
    fun `test month navigation - previous month loads october data`() = runTest {
        advanceUntilIdle()
        viewModel.selectPreviousMonth()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val report = state.reportData

        assertEquals(october, state.selectedYearMonth)
        assertEquals("إجمالي المصروفات لشهر أكتوبر: 30,000 ريال", 30000L, report.totalExpenses)
        assertEquals("إجمالي الإيرادات لشهر أكتوبر: 80,000 ريال", 80000L, report.totalIncome)
        assertEquals("صافي التدفق المالي: 50,000 ريال", 50000L, report.netCashflow)
        assertEquals(1, report.categoryBreakdown.size)
    }

    @Test
    fun `test month navigation - next month advances correctly`() = runTest {
        advanceUntilIdle()
        viewModel.selectNextMonth()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(YearMonth.of(2026, 12), state.selectedYearMonth)
        assertFalse("شهر ديسمبر لا يحتوي على مصروفات", state.reportData.hasExpenses)
        assertEquals(0L, state.reportData.totalExpenses)
    }

    @Test
    fun `test category selection and toggling in pie chart details`() = runTest {
        advanceUntilIdle()
        val firstCategory = viewModel.uiState.value.reportData.categoryBreakdown.first()

        // 1. تحديد التصنيف
        viewModel.selectCategory(firstCategory)
        advanceUntilIdle()
        assertEquals(firstCategory.categoryId, viewModel.uiState.value.selectedCategoryForDetails?.categoryId)

        // 2. النقر على نفس التصنيف مجدداً يلغي التحديد
        viewModel.selectCategory(firstCategory)
        advanceUntilIdle()
        assertNull("يجب إلغاء التحديد بالنقر مجدداً", viewModel.uiState.value.selectedCategoryForDetails)
    }

    @Test
    fun `test empty month handles zero expenses safely without crash`() = runTest {
        val emptyMonth = YearMonth.of(2025, 1)
        viewModel.setSelectedYearMonth(emptyMonth)
        advanceUntilIdle()

        val report = viewModel.uiState.value.reportData
        assertEquals(0L, report.totalExpenses)
        assertEquals(0L, report.totalIncome)
        assertEquals(0L, report.averageDailyExpense)
        assertEquals(0L, report.averageWeeklyExpense)
        assertFalse("يجب أن تكون hasExpenses كاذبة", report.hasExpenses)
        assertTrue("قائمة التصنيفات يجب أن تكون فارغة", report.categoryBreakdown.isEmpty())
    }

    @Test
    fun `test monthly comparisons include 6 months and percentage change`() = runTest {
        advanceUntilIdle()
        val report = viewModel.uiState.value.reportData

        assertEquals("يجب أن تحتوي المقارنة على 6 أشهر", 6, report.monthlyComparisons.size)
        val selectedMonthItem = report.monthlyComparisons.find { it.isCurrentSelected }
        assertNotNull("يجب أن يوجد الشهر المختار في المقارنة", selectedMonthItem)
        assertEquals(november, selectedMonthItem?.yearMonth)
        assertEquals(45000L, selectedMonthItem?.amount)

        // أكتوبر كان 30,000 ونوفمبر 45,000 -> نسبة الزيادة +50%
        assertEquals(50, report.monthChangePercentage)
    }
}

/**
 * مستودع وهمي لاختبارات تقارير العمليات المالية
 */
class FakeReportsTxRepo : TransactionRepository {
    private val transactionsFlow = MutableStateFlow<List<TransactionItem>>(emptyList())

    fun setTransactions(list: List<TransactionItem>) {
        transactionsFlow.value = list
    }

    override fun getAllTransactions(): Flow<List<TransactionItem>> = transactionsFlow

    override fun getTransactionsByMonth(yearMonth: String): Flow<List<TransactionItem>> {
        return transactionsFlow.map { list -> list.filter { it.transactionDate.startsWith(yearMonth) } }
    }

    override fun getTransactionsByDateRange(startDate: String, endDate: String): Flow<List<TransactionItem>> {
        return transactionsFlow.map { list -> list.filter { it.transactionDate in startDate..endDate } }
    }

    override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionItem>> {
        return transactionsFlow.map { list -> list.filter { it.type == type } }
    }

    override fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionItem>> {
        return transactionsFlow.map { list -> list.filter { it.categoryId == categoryId } }
    }

    override fun getTransactionById(id: String): Flow<TransactionItem?> {
        return transactionsFlow.map { list -> list.find { it.id == id } }
    }

    override suspend fun addTransaction(transaction: TransactionItem): Result<Unit> {
        transactionsFlow.value = transactionsFlow.value + transaction
        return Result.success(Unit)
    }

    override suspend fun updateTransaction(transaction: TransactionItem): Result<Unit> {
        transactionsFlow.value = transactionsFlow.value.map { if (it.id == transaction.id) transaction else it }
        return Result.success(Unit)
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> {
        transactionsFlow.value = transactionsFlow.value.filter { it.id != id }
        return Result.success(Unit)
    }
}
