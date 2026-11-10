package com.masroofi.app.presentation

import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.MonthlyBudget
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.MonthlyBudgetRepository
import com.masroofi.app.domain.repository.TransactionRepository
import com.masroofi.app.domain.usecase.GetMonthlyFinancialSummaryUseCase
import com.masroofi.app.domain.usecase.SetMonthlyBudgetUseCase
import com.masroofi.app.presentation.screens.home.HomeViewModel
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
import java.util.UUID

/**
 * اختبارات وحدة شاملة لنموذج العرض للشاشة الرئيسية (HomeViewModelTest)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTransactionRepository: FakeHomeTransactionRepository
    private lateinit var fakeBudgetRepository: FakeHomeBudgetRepository
    private lateinit var getSummaryUseCase: GetMonthlyFinancialSummaryUseCase
    private lateinit var setBudgetUseCase: SetMonthlyBudgetUseCase
    private lateinit var viewModel: HomeViewModel

    private val sampleFoodCategory = Category(
        id = "cat_food",
        name = "طعام ومشروبات",
        type = TransactionType.EXPENSE,
        iconKey = "fastfood"
    )

    private val sampleSalaryCategory = Category(
        id = "cat_salary",
        name = "راتب شهري",
        type = TransactionType.INCOME,
        iconKey = "payments"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTransactionRepository = FakeHomeTransactionRepository()
        fakeBudgetRepository = FakeHomeBudgetRepository()
        getSummaryUseCase = GetMonthlyFinancialSummaryUseCase(fakeTransactionRepository, fakeBudgetRepository)
        setBudgetUseCase = SetMonthlyBudgetUseCase(fakeBudgetRepository)
        viewModel = HomeViewModel(getSummaryUseCase, setBudgetUseCase, fakeTransactionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state loads data for current month`() = runTest {
        val currentMonth = YearMonth.now()
        val currentMonthStr = currentMonth.toString()

        // إضافة ميزانية للشهر الحالي
        fakeBudgetRepository.setBudgetForMonth(currentMonthStr, 120000L)

        // إضافة مصروف وإيراد للشهر الحالي
        fakeTransactionRepository.addTransaction(
            TransactionItem(
                id = "tx1",
                type = TransactionType.EXPENSE,
                amount = 40000L,
                categoryId = sampleFoodCategory.id,
                categoryName = sampleFoodCategory.name,
                categoryIconKey = sampleFoodCategory.iconKey,
                transactionDate = "$currentMonthStr-05"
            )
        )
        fakeTransactionRepository.addTransaction(
            TransactionItem(
                id = "tx2",
                type = TransactionType.INCOME,
                amount = 150000L,
                categoryId = sampleSalaryCategory.id,
                categoryName = sampleSalaryCategory.name,
                categoryIconKey = sampleSalaryCategory.iconKey,
                transactionDate = "$currentMonthStr-01"
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("الشهر المختار الافتراضي يجب أن يكون الشهر الحالي", currentMonth, state.selectedMonth)
        assertEquals("الميزانية يجب أن تكون 120,000", 120000L, state.summary.monthlyBudget)
        assertEquals("المصروفات الشهرية 40,000", 40000L, state.summary.totalMonthlyExpenses)
        assertEquals("الإيرادات الشهرية 150,000", 150000L, state.summary.totalMonthlyIncome)
        assertEquals("المتبقي 80,000", 80000L, state.summary.remainingBudget)
        assertEquals("نسبة الاستهلاك 33%", 33, state.summary.consumptionPercentage)
        assertEquals("صافي التدفق 110,000", 110000L, state.summary.netCashflow)
        assertEquals("آخر العمليات يجب أن تحتوي على عمليتين", 2, state.recentTransactions.size)
    }

    @Test
    fun `test month navigation isolates transactions per month`() = runTest {
        val currentMonth = YearMonth.now()
        val previousMonth = currentMonth.minusMonths(1)

        // عملية في الشهر السابق
        fakeTransactionRepository.addTransaction(
            TransactionItem(
                id = "tx_prev",
                type = TransactionType.EXPENSE,
                amount = 18000L,
                categoryId = sampleFoodCategory.id,
                categoryName = sampleFoodCategory.name,
                categoryIconKey = sampleFoodCategory.iconKey,
                transactionDate = "$previousMonth-15"
            )
        )

        advanceUntilIdle()

        // الانتقال للشهر السابق
        viewModel.selectPreviousMonth()
        advanceUntilIdle()

        val prevState = viewModel.uiState.value
        assertEquals("الشهر المختار يجب أن يكون الشهر السابق", previousMonth, prevState.selectedMonth)
        assertEquals("مصروفات الشهر السابق 18,000", 18000L, prevState.summary.totalMonthlyExpenses)
        assertEquals("يجب أن تظهر عملية واحدة في الشهر السابق", 1, prevState.recentTransactions.size)

        // العودة للشهر الحالي
        viewModel.selectNextMonth()
        advanceUntilIdle()

        val currentState = viewModel.uiState.value
        assertEquals("الشهر الحالي يجب أن يكون معزولاً عن الشهر السابق", 0L, currentState.summary.totalMonthlyExpenses)
        assertTrue("لا توجد عمليات مسجلة في الشهر الحالي", currentState.recentTransactions.isEmpty())
    }

    @Test
    fun `test open, change and save monthly budget successfully`() = runTest {
        advanceUntilIdle()

        viewModel.openBudgetDialog()
        val stateOpen = viewModel.uiState.value
        assertTrue("يجب أن تفتح نافذة الميزانية", stateOpen.isBudgetDialogOpen)

        viewModel.onBudgetInputChanged("95000")
        viewModel.saveMonthlyBudget()
        advanceUntilIdle()

        val stateSaved = viewModel.uiState.value
        assertFalse("يجب أن تغلق نافذة الميزانية بعد الحفظ", stateSaved.isBudgetDialogOpen)
        assertEquals("الميزانية يجب أن تتحدث إلى 95,000", 95000L, stateSaved.summary.monthlyBudget)
    }

    @Test
    fun `test saving invalid budget displays validation error`() = runTest {
        viewModel.openBudgetDialog()

        viewModel.onBudgetInputChanged("")
        viewModel.saveMonthlyBudget()
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertTrue("يجب أن تبقى النافذة مفتوحة", state.isBudgetDialogOpen)
        assertNotNull("يجب أن تظهر رسالة خطأ", state.budgetError)

        viewModel.onBudgetInputChanged("0")
        viewModel.saveMonthlyBudget()
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue(state.isBudgetDialogOpen)
        assertEquals("يرجى إدخال مبلغ صحيح أكبر من الصفر", state.budgetError)
    }

    @Test
    fun `test delete transaction flow from home screen`() = runTest {
        val currentMonth = YearMonth.now()
        val tx = TransactionItem(
            id = "tx_delete_me",
            type = TransactionType.EXPENSE,
            amount = 12000L,
            categoryId = sampleFoodCategory.id,
            categoryName = sampleFoodCategory.name,
            categoryIconKey = sampleFoodCategory.iconKey,
            transactionDate = "$currentMonth-02"
        )
        fakeTransactionRepository.addTransaction(tx)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.recentTransactions.size)

        // طلب الحذف
        viewModel.requestDeleteTransaction(tx)
        assertTrue("يجب أن تفتح نافذة تأكيد الحذف", viewModel.uiState.value.isDeleteDialogOpen)
        assertEquals(tx, viewModel.uiState.value.transactionToDelete)

        // تأكيد الحذف
        viewModel.confirmDeleteTransaction()
        advanceUntilIdle()

        val afterState = viewModel.uiState.value
        assertFalse("يجب أن تغلق نافذة الحذف", afterState.isDeleteDialogOpen)
        assertNull("يجب مسح العملية المحددة للحذف", afterState.transactionToDelete)
        assertTrue("يجب أن تصبح القائمة فارغة بعد الحذف", afterState.recentTransactions.isEmpty())
    }
}

// محاكي لمستودع العمليات للشاشة الرئيسية
private class FakeHomeTransactionRepository : TransactionRepository {
    private val transactions = mutableListOf<TransactionItem>()
    private val flow = MutableStateFlow<List<TransactionItem>>(emptyList())

    override fun getAllTransactions(): Flow<List<TransactionItem>> = flow

    override fun getTransactionsByMonth(yearMonth: String): Flow<List<TransactionItem>> {
        return flow.map { list ->
            list.filter { it.transactionDate.startsWith(yearMonth) }
        }
    }

    override fun getTransactionsByDateRange(startDate: String, endDate: String): Flow<List<TransactionItem>> {
        return flow.map { list ->
            list.filter { it.transactionDate in startDate..endDate }
        }
    }

    override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionItem>> {
        return flow.map { list -> list.filter { it.type == type } }
    }

    override fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionItem>> {
        return flow.map { list -> list.filter { it.categoryId == categoryId } }
    }

    override fun getTransactionById(id: String): Flow<TransactionItem?> {
        return flow.map { list -> list.find { it.id == id } }
    }

    override suspend fun addTransaction(transaction: TransactionItem): Result<Unit> {
        transactions.add(0, transaction)
        flow.value = transactions.toList()
        return Result.success(Unit)
    }

    override suspend fun updateTransaction(transaction: TransactionItem): Result<Unit> {
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
            flow.value = transactions.toList()
            return Result.success(Unit)
        }
        return Result.failure(NoSuchElementException())
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> {
        val removed = transactions.removeAll { it.id == id }
        flow.value = transactions.toList()
        return if (removed) Result.success(Unit) else Result.failure(NoSuchElementException())
    }
}

// محاكي لمستودع الميزانيات الشهرية للشاشة الرئيسية
private class FakeHomeBudgetRepository : MonthlyBudgetRepository {
    private val budgets = mutableListOf<MonthlyBudget>()
    private val flow = MutableStateFlow<List<MonthlyBudget>>(emptyList())

    override fun getBudgetForMonth(yearMonth: String): Flow<MonthlyBudget?> {
        return flow.map { list -> list.find { it.yearMonth == yearMonth } }
    }

    override fun getAllBudgets(): Flow<List<MonthlyBudget>> = flow

    override suspend fun setBudgetForMonth(yearMonth: String, amount: Long): Result<Unit> {
        val index = budgets.indexOfFirst { it.yearMonth == yearMonth }
        if (index != -1) {
            budgets[index] = budgets[index].copy(amount = amount)
        } else {
            budgets.add(MonthlyBudget(id = UUID.randomUUID().toString(), yearMonth = yearMonth, amount = amount))
        }
        flow.value = budgets.toList()
        return Result.success(Unit)
    }

    override suspend fun deleteBudget(id: String): Result<Unit> {
        budgets.removeAll { it.id == id }
        flow.value = budgets.toList()
        return Result.success(Unit)
    }
}
