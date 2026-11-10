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
import kotlinx.coroutines.flow.flowOf
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

    // 1. التحقق من التهيئة الافتراضية للإضافة (مصروف، تاريخ اليوم، اختيار أول تصنيف)
    @Test
    fun initForAdd_setsDefaultStateProperly() = runTest {
        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TransactionType.EXPENSE, state.type)
        assertEquals(LocalDate.now(), state.transactionDate)
        assertEquals("", state.amount)
        assertFalse(state.isEditing)
        assertNull(state.transactionId)
        assertNotNull(state.selectedCategory)
        assertEquals("cat_exp_food", state.selectedCategory?.id)
    }

    // 2. التحقق من التبديل بين المصروف والإيراد وتحديث التصنيفات تلقائياً
    @Test
    fun setType_switchesCategoriesDynamically() = runTest {
        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        // التبديل إلى إيراد
        viewModel.setType(TransactionType.INCOME)
        testDispatcher.scheduler.advanceUntilIdle()

        val incomeState = viewModel.uiState.value
        assertEquals(TransactionType.INCOME, incomeState.type)
        assertEquals(5, incomeState.availableCategories.size)
        assertTrue(incomeState.availableCategories.all { it.type == TransactionType.INCOME })
        assertEquals("cat_inc_salary", incomeState.selectedCategory?.id)

        // العودة إلى مصروف
        viewModel.setType(TransactionType.EXPENSE)
        testDispatcher.scheduler.advanceUntilIdle()

        val expenseState = viewModel.uiState.value
        assertEquals(TransactionType.EXPENSE, expenseState.type)
        assertEquals(10, expenseState.availableCategories.size)
        assertTrue(expenseState.availableCategories.all { it.type == TransactionType.EXPENSE })
        assertEquals("cat_exp_food", expenseState.selectedCategory?.id)
    }

    // 3. التحقق من إدخال المبلغ وتجاهل الرموز غير الرقمية
    @Test
    fun setAmount_filtersNonDigits() {
        viewModel.setAmount("150abc0")
        assertEquals("1500", viewModel.uiState.value.amount)

        viewModel.setAmount("25000")
        assertEquals("25000", viewModel.uiState.value.amount)
    }

    // 4. رفض المبلغ الصفري
    @Test
    fun saveTransaction_rejectsZeroAmount() = runTest {
        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setAmount("0")
        viewModel.saveTransaction()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("يجب إظهار رسالة خطأ للمبلغ الصفري", state.amountError)
        assertFalse("يجب ألا يتم حفظ العملية", state.isSavedSuccess)
        assertEquals(0, fakeTransactionRepository.transactions.size)
    }

    // 5. رفض المبلغ الفارغ
    @Test
    fun saveTransaction_rejectsEmptyAmount() = runTest {
        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setAmount("")
        viewModel.saveTransaction()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("يجب إظهار رسالة خطأ للمبلغ الفارغ", state.amountError)
        assertFalse(state.isSavedSuccess)
        assertEquals(0, fakeTransactionRepository.transactions.size)
    }

    // 6. رفض التاريخ المستقبلي
    @Test
    fun setDate_rejectsFutureDate() = runTest {
        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        val futureDate = LocalDate.now().plusDays(2)
        viewModel.setDate(futureDate)

        val state = viewModel.uiState.value
        assertEquals(futureDate, state.transactionDate)
        assertNotNull("يجب إظهار رسالة خطأ للتاريخ المستقبلي", state.dateError)
        assertEquals("لا يمكن تسجيل عملية بتاريخ مستقبلي", state.dateError)

        viewModel.setAmount("1000")
        viewModel.saveTransaction()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("يجب منع حفظ عملية ذات تاريخ مستقبلي", viewModel.uiState.value.isSavedSuccess)
        assertEquals(0, fakeTransactionRepository.transactions.size)
    }

    // 7. حفظ مصروف جديد بنجاح
    @Test
    fun saveTransaction_addsNewExpenseSuccessfully() = runTest {
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
        assertEquals(1, fakeTransactionRepository.transactions.size)

        val saved = fakeTransactionRepository.transactions.first()
        assertEquals(3500L, saved.amount)
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals("cat_exp_food", saved.categoryId)
        assertEquals("وجبة غداء عمل", saved.note)
    }

    // 8. حفظ إيراد جديد بنجاح
    @Test
    fun saveTransaction_addsNewIncomeSuccessfully() = runTest {
        viewModel.initForAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setType(TransactionType.INCOME)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setAmount("120000")
        viewModel.setNote("دفعة مستحقات")
        viewModel.saveTransaction()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSavedSuccess)
        assertEquals(1, fakeTransactionRepository.transactions.size)

        val saved = fakeTransactionRepository.transactions.first()
        assertEquals(120000L, saved.amount)
        assertEquals(TransactionType.INCOME, saved.type)
        assertEquals("cat_inc_salary", saved.categoryId)
    }

    // 9. تعديل عملية: الحفاظ على id الأصلي و createdAt وتحديث updatedAt فقط
    @Test
    fun saveTransaction_inEditMode_preservesIdAndCreatedAt_andUpdatesUpdatedAt() = runTest {
        val originalCreatedAt = 1700000000000L
        val originalUpdatedAt = 1700000000000L
        val existingTx = TransactionItem(
            id = "tx_original_123",
            type = TransactionType.EXPENSE,
            amount = 5000L,
            categoryId = "cat_exp_bills",
            transactionDate = "2026-11-08",
            note = "فاتورة قديمة",
            createdAt = originalCreatedAt,
            updatedAt = originalUpdatedAt
        )
        fakeTransactionRepository.transactions.add(existingTx)

        // فتح شاشة التعديل
        viewModel.initForEdit(existingTx)
        testDispatcher.scheduler.advanceUntilIdle()

        val editState = viewModel.uiState.value
        assertTrue(editState.isEditing)
        assertEquals("tx_original_123", editState.transactionId)
        assertEquals("5000", editState.amount)
        assertEquals(originalCreatedAt, editState.createdAt)

        // تعديل المبلغ والملاحظة
        viewModel.setAmount("7500")
        viewModel.setNote("فاتورة معدلة")
        viewModel.saveTransaction()
        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertTrue(finalState.isSavedSuccess)
        assertEquals("تم تعديل العملية بنجاح", finalState.userMessage)

        val updatedInRepo = fakeTransactionRepository.transactions.first()
        assertEquals("يجب الحفاظ على نفس المعرف id", "tx_original_123", updatedInRepo.id)
        assertEquals("يجب الحفاظ على نفس وقت الإنشاء createdAt", originalCreatedAt, updatedInRepo.createdAt)
        assertTrue("يجب تحديث وقت التعديل updatedAt إلى وقت أحدث", updatedInRepo.updatedAt > originalUpdatedAt)
        assertEquals(7500L, updatedInRepo.amount)
        assertEquals("فاتورة معدلة", updatedInRepo.note)
    }

    // 10. حذف عملية مالية
    @Test
    fun deleteTransaction_removesTransactionAndNotifies() = runTest {
        val tx = TransactionItem(
            id = "tx_to_delete",
            type = TransactionType.EXPENSE,
            amount = 2000L,
            categoryId = "cat_exp_food",
            transactionDate = "2026-11-10"
        )
        fakeTransactionRepository.transactions.add(tx)

        viewModel.deleteTransaction("tx_to_delete")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isDeletedSuccess)
        assertEquals("تم حذف العملية بنجاح", state.userMessage)
        assertTrue(fakeTransactionRepository.transactions.isEmpty())
    }
}

// محاكي لمستودع العمليات لاختبارات نموذج العرض
private class FakeTransactionRepository : TransactionRepository {
    val transactions = mutableListOf<TransactionItem>()
    private val flow = MutableStateFlow<List<TransactionItem>>(transactions)

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
