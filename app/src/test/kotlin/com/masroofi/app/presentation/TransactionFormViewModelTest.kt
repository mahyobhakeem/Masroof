package com.masroofi.app.presentation

import com.masroofi.app.data.database.DefaultCategories
import com.masroofi.app.data.repository.toDomain
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.CategoryRepository
import com.masroofi.app.domain.repository.TransactionRepository
import com.masroofi.app.presentation.screens.transactions.TransactionFormViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTransactionRepository: FakeTransactionRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var viewModel: TransactionFormViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTransactionRepository = FakeTransactionRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        viewModel = TransactionFormViewModel(fakeTransactionRepository, fakeCategoryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. التحقق من التهيئة الافتراضية للإضافة عندما يكون الرصيد 0: التوجيه التلقائي للإيراد مع تنبيه الرصيد
    @Test
    fun initForAdd_withZeroBalance_defaultsToIncomeAndWarns() = runTest {
        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("عندما يكون الرصيد 0 يجب أن يفتح افتراضياً على تبويب إيراد", TransactionType.INCOME, state.type)
        assertEquals(LocalDate.now(), state.transactionDate)
        assertEquals("", state.amount)
        assertFalse(state.isEditing)
        assertNull(state.transactionId)
        assertNotNull(state.selectedCategory)
        assertEquals("cat_inc_opening_balance", state.selectedCategory?.id)
        assertNotNull("يجب إظهار تنبيه لطيف للمستخدم بأن الرصيد 0", state.userMessage)
    }

    // 2. التحقق من التهيئة الافتراضية للإضافة عندما يتوفر رصيد متاح في المحفظة
    @Test
    fun initForAdd_withAvailableBalance_allowsExpenseByDefault() = runTest {
        // إضافة رصيد إيراد مسبق في المستودع
        fakeTransactionRepository.transactions.add(
            TransactionItem(
                id = "tx_income_init",
                type = TransactionType.INCOME,
                amount = 50000L,
                categoryId = "cat_inc_salary",
                transactionDate = LocalDate.now().toString()
            )
        )
        fakeTransactionRepository.notifyChange()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TransactionType.EXPENSE, state.type)
        assertEquals(50000L, state.currentAvailableBalance)
        assertEquals("cat_exp_food", state.selectedCategory?.id)
    }

    // 3. منع التبديل إلى مصروف أو حفظ مصروف إذا كان الرصيد صفراً
    @Test
    fun setType_andSave_preventsExpenseWhenBalanceIsZero() = runTest {
        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        // محاولة التبديل إلى مصروف
        viewModel.setType(TransactionType.EXPENSE)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("يجب البقاء على إيراد عند انعدام الرصيد", TransactionType.INCOME, state.type)
        assertTrue(state.amountError?.contains("رصيدك المتاح هو 0") == true)
    }

    // 4. منع حفظ مصروف إذا كان المبلغ المدخل يتجاوز الرصيد المتاح
    @Test
    fun saveTransaction_rejectsExpenseExceedingAvailableBalance() = runTest {
        fakeTransactionRepository.transactions.add(
            TransactionItem(
                id = "tx_income_10k",
                type = TransactionType.INCOME,
                amount = 10000L,
                categoryId = "cat_inc_salary",
                transactionDate = LocalDate.now().toString()
            )
        )
        fakeTransactionRepository.notifyChange()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        // محاولة تسجيل مصروف بمبلغ 15,000 (أكبر من 10,000)
        viewModel.setAmount("15000")
        viewModel.saveTransaction()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("يجب منع حفظ المصروف المتجاوز للرصيد", state.isSavedSuccess)
        assertNotNull("يجب إظهار رسالة خطأ صريحة للمبلغ المتجاوز", state.amountError)
        assertTrue(state.amountError?.contains("يتجاوز رصيدك الصافي المتاح") == true)
        assertEquals(1, fakeTransactionRepository.transactions.size)
    }

    // 5. حفظ مصروف جديد بنجاح عندما يكون المبلغ ضمن الرصيد المتاح
    @Test
    fun saveTransaction_addsNewExpenseWithinBalanceSuccessfully() = runTest {
        fakeTransactionRepository.transactions.add(
            TransactionItem(
                id = "tx_income_20k",
                type = TransactionType.INCOME,
                amount = 20000L,
                categoryId = "cat_inc_salary",
                transactionDate = LocalDate.now().toString()
            )
        )
        fakeTransactionRepository.notifyChange()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setAmount("3500")
        viewModel.setDate(LocalDate.now())
        viewModel.setNote("وجبة غداء عمل")
        viewModel.saveTransaction()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("يجب نجاح الحفظ", state.isSavedSuccess)
        assertEquals("تم حفظ العملية بنجاح", state.userMessage)
        assertEquals(2, fakeTransactionRepository.transactions.size)

        val saved = fakeTransactionRepository.transactions.last()
        assertEquals(3500L, saved.amount)
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals("cat_exp_food", saved.categoryId)
    }

    // 6. حفظ إيراد جديد بنجاح
    @Test
    fun saveTransaction_addsNewIncomeSuccessfully() = runTest {
        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setAmount("120000")
        viewModel.setNote("الرصيد الافتتاحي للمحفظة")
        viewModel.saveTransaction()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSavedSuccess)
        assertEquals(1, fakeTransactionRepository.transactions.size)

        val saved = fakeTransactionRepository.transactions.first()
        assertEquals(120000L, saved.amount)
        assertEquals(TransactionType.INCOME, saved.type)
        assertEquals("cat_inc_opening_balance", saved.categoryId)
    }

    // 7. رفض المبلغ الصفري أو الفارغ
    @Test
    fun saveTransaction_rejectsZeroAndEmptyAmount() = runTest {
        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setAmount("0")
        viewModel.saveTransaction()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.amountError)

        viewModel.setAmount("")
        viewModel.saveTransaction()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.amountError)
    }

    // 8. رفض التاريخ المستقبلي
    @Test
    fun setDate_rejectsFutureDate() = runTest {
        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        val futureDate = LocalDate.now().plusDays(2)
        viewModel.setDate(futureDate)

        val state = viewModel.uiState.value
        assertEquals(futureDate, state.transactionDate)
        assertEquals("لا يمكن تسجيل عملية بتاريخ مستقبلي", state.dateError)
    }

    // 9. منع حذف الإيراد إذا كانت هناك مصروفات اعتمدت عليه (منع الصافي السالب)
    @Test
    fun deleteTransaction_preventsDeletingIncome_whenExpensesRelyOnIt() = runTest {
        val income = TransactionItem(
            id = "tx_inc_1",
            type = TransactionType.INCOME,
            amount = 10000L,
            categoryId = "cat_inc_salary",
            transactionDate = LocalDate.now().toString()
        )
        val expense = TransactionItem(
            id = "tx_exp_1",
            type = TransactionType.EXPENSE,
            amount = 8000L,
            categoryId = "cat_exp_food",
            transactionDate = LocalDate.now().toString()
        )
        fakeTransactionRepository.transactions.addAll(listOf(income, expense))
        fakeTransactionRepository.notifyChange()
        testDispatcher.scheduler.advanceUntilIdle()

        // محاولة حذف الإيراد: سيجعل الصافي -8000
        viewModel.deleteTransaction("tx_inc_1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("يجب منع حذف الإيراد الذي تعتمد عليه مصروفات", state.isDeletedSuccess)
        assertTrue(state.userMessage?.contains("سيجعل الصافي بالسالب") == true)
        assertEquals(2, fakeTransactionRepository.transactions.size)
    }

    // 10. منع تقليل الإيراد عند التعديل إذا كان سيجعل الصافي سالباً
    @Test
    fun saveTransaction_preventsReducingIncome_whenExpensesRelyOnIt() = runTest {
        val income = TransactionItem(
            id = "tx_inc_2",
            type = TransactionType.INCOME,
            amount = 10000L,
            categoryId = "cat_inc_salary",
            transactionDate = LocalDate.now().toString()
        )
        val expense = TransactionItem(
            id = "tx_exp_2",
            type = TransactionType.EXPENSE,
            amount = 8000L,
            categoryId = "cat_exp_food",
            transactionDate = LocalDate.now().toString()
        )
        fakeTransactionRepository.transactions.addAll(listOf(income, expense))
        fakeTransactionRepository.notifyChange()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.initForEdit(income)
        testDispatcher.scheduler.advanceUntilIdle()

        // محاولة تقليل الإيراد إلى 5000 (الصافي سيصبح 5000 - 8000 = -3000)
        viewModel.setAmount("5000")
        viewModel.saveTransaction()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("يجب منع تقليل الإيراد بما يسبب عجزاً سالباً", state.isSavedSuccess)
        assertTrue(state.amountError?.contains("سيجعل الصافي بالسالب") == true)
    }
}

// محاكي لمستودع العمليات لاختبارات نموذج العرض
private class FakeTransactionRepository : TransactionRepository {
    val transactions = mutableListOf<TransactionItem>()
    private val flow = MutableStateFlow<List<TransactionItem>>(transactions)

    fun notifyChange() {
        flow.value = transactions.toList()
    }

    override fun getAllTransactions(): Flow<List<TransactionItem>> = flow

    override fun getTransactionsByMonth(yearMonth: String): Flow<List<TransactionItem>> =
        flow.map { list -> list.filter { it.transactionDate.startsWith(yearMonth) } }

    override fun getTransactionsByDateRange(startDate: String, endDate: String): Flow<List<TransactionItem>> =
        flow.map { list -> list.filter { it.transactionDate in startDate..endDate } }

    override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionItem>> =
        flow.map { list -> list.filter { it.type == type } }

    override fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionItem>> =
        flow.map { list -> list.filter { it.categoryId == categoryId } }

    override fun getTransactionById(id: String): Flow<TransactionItem?> =
        flow.map { list -> list.find { it.id == id } }

    override suspend fun addTransaction(transaction: TransactionItem): Result<Unit> {
        transactions.add(transaction)
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
        return Result.failure(NoSuchElementException("العملية غير موجودة"))
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> {
        val removed = transactions.removeAll { it.id == id }
        flow.value = transactions.toList()
        return if (removed) Result.success(Unit) else Result.failure(NoSuchElementException("العملية غير موجودة"))
    }
}

// محاكي لمستودع التصنيفات لاختبارات نموذج العرض
private class FakeCategoryRepository : CategoryRepository {
    val categories = DefaultCategories.allDefaultCategories.map { it.toDomain() }.toMutableList()
    private val flow = MutableStateFlow<List<Category>>(categories)

    override fun getAllCategories(): Flow<List<Category>> = flow

    override fun getActiveCategories(type: TransactionType?): Flow<List<Category>> {
        return flow.map { list ->
            list.filter { it.isActive && (type == null || it.type == type) }
        }
    }

    override fun getCategoryById(id: String): Flow<Category?> =
        flow.map { list -> list.find { it.id == id } }

    override suspend fun addCategory(category: Category): Result<Unit> {
        categories.add(category)
        flow.value = categories.toList()
        return Result.success(Unit)
    }

    override suspend fun updateCategory(category: Category): Result<Unit> {
        val index = categories.indexOfFirst { it.id == category.id }
        if (index != -1) {
            categories[index] = category
            flow.value = categories.toList()
        }
        return Result.success(Unit)
    }

    override suspend fun setCategoryActive(id: String, isActive: Boolean): Result<Unit> {
        val cat = categories.find { it.id == id }
        if (cat != null) {
            updateCategory(cat.copy(isActive = isActive))
        }
        return Result.success(Unit)
    }

    override suspend fun deleteCategory(id: String): Result<Unit> {
        categories.removeAll { it.id == id }
        flow.value = categories.toList()
        return Result.success(Unit)
    }

    override suspend fun getTransactionCountForCategory(id: String): Int = 0

    override suspend fun seedDefaultCategories(): Result<Unit> = Result.success(Unit)
}
