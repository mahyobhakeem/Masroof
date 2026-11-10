package com.masroofi.app.presentation

import com.masroofi.app.data.database.DefaultCategories
import com.masroofi.app.data.repository.toDomain
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.CategoryRepository
import com.masroofi.app.domain.repository.TransactionRepository
import com.masroofi.app.presentation.screens.transactions.DateRangeFilter
import com.masroofi.app.presentation.screens.transactions.TransactionsHistoryViewModel
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
 * اختبارات وحدة شاملة لشاشة وسجل العمليات والبحث والتصفية (TransactionsHistoryViewModelTest)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTransactionRepository: FakeHistoryTxRepo
    private lateinit var fakeCategoryRepository: FakeHistoryCatRepo
    private lateinit var viewModel: TransactionsHistoryViewModel

    private val foodCategory = Category("cat_exp_food", "الطعام والمشروبات", TransactionType.EXPENSE, "fastfood")
    private val transportCategory = Category("cat_exp_transport", "المواصلات", TransactionType.EXPENSE, "directions_car")
    private val otherExpCategory = Category("cat_exp_other", "مصروفات أخرى", TransactionType.EXPENSE, "more_horiz")
    private val salaryCategory = Category("cat_inc_salary", "الراتب", TransactionType.INCOME, "payments")
    private val otherIncCategory = Category("cat_inc_other", "إيرادات أخرى", TransactionType.INCOME, "savings")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTransactionRepository = FakeHistoryTxRepo()
        fakeCategoryRepository = FakeHistoryCatRepo(
            listOf(foodCategory, transportCategory, otherExpCategory, salaryCategory, otherIncCategory)
        )
        viewModel = TransactionsHistoryViewModel(fakeTransactionRepository, fakeCategoryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test categories logical ordering - other categories placed at the end`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        val allCats = state.allCategories

        assertFalse("تصنيف 'مصروفات أخرى' لا يجب أن يكون في بداية القائمة", allCats.first().name.contains("أخرى"))
        assertEquals("التصنيف الأول يجب أن يكون الطعام والمشروبات", "الطعام والمشروبات", allCats.first().name)

        val expOtherIndex = allCats.indexOfFirst { it.id == "cat_exp_other" }
        val incOtherIndex = allCats.indexOfFirst { it.id == "cat_inc_other" }

        assertTrue("مصروفات أخرى يجب أن تأتي بعد تصنيفات المصروفات الأساسية", expOtherIndex > 1)
        assertTrue("إيرادات أخرى يجب أن تأتي في نهاية القائمة بعد الراتب", incOtherIndex > expOtherIndex)
    }

    @Test
    fun `test filtering categories by transaction type`() = runTest {
        advanceUntilIdle()

        // عند اختيار نوع "المصروفات"
        viewModel.setFilterIndex(1)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertTrue("يجب أن تكون جميع التصنيفات المعروضة من نوع المصروفات",
            state.availableFilterCategories.all { it.type == TransactionType.EXPENSE })
        assertFalse("لا يجب ظهور تصنيف الراتب ضمن فلاتر المصروفات",
            state.availableFilterCategories.any { it.id == "cat_inc_salary" })

        // عند اختيار نوع "الإيرادات"
        viewModel.setFilterIndex(2)
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue("يجب أن تكون جميع التصنيفات المعروضة من نوع الإيرادات",
            state.availableFilterCategories.all { it.type == TransactionType.INCOME })
        assertFalse("لا يجب ظهور تصنيف الطعام ضمن فلاتر الإيرادات",
            state.availableFilterCategories.any { it.id == "cat_exp_food" })
    }

    @Test
    fun `test text search by note and category name`() = runTest {
        val today = LocalDate.now().toString()
        fakeTransactionRepository.addTransaction(
            TransactionItem("1", TransactionType.EXPENSE, 5000, foodCategory.id, foodCategory.name, foodCategory.iconKey, today, "وجبة غداء عمل")
        )
        fakeTransactionRepository.addTransaction(
            TransactionItem("2", TransactionType.EXPENSE, 3000, transportCategory.id, transportCategory.name, transportCategory.iconKey, today, "أجرة تكسي")
        )
        advanceUntilIdle()

        // بحث بالملاحظة
        viewModel.setSearchQuery("غداء")
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(1, state.transactions.size)
        assertEquals("1", state.transactions.first().id)

        // بحث باسم التصنيف
        viewModel.setSearchQuery("المواصلات")
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(1, state.transactions.size)
        assertEquals("2", state.transactions.first().id)
    }

    @Test
    fun `test numeric search by amount`() = runTest {
        val today = LocalDate.now().toString()
        fakeTransactionRepository.addTransaction(
            TransactionItem("1", TransactionType.EXPENSE, 12500, foodCategory.id, foodCategory.name, foodCategory.iconKey, today, "تسوق")
        )
        fakeTransactionRepository.addTransaction(
            TransactionItem("2", TransactionType.EXPENSE, 4000, foodCategory.id, foodCategory.name, foodCategory.iconKey, today, "عصير")
        )
        advanceUntilIdle()

        viewModel.setSearchQuery("12500")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.transactions.size)
        assertEquals("1", state.transactions.first().id)
        assertEquals(12500L, state.transactions.first().amount)
    }

    @Test
    fun `test filter by categoryId prevents mixing between different types`() = runTest {
        val today = LocalDate.now().toString()
        fakeTransactionRepository.addTransaction(
            TransactionItem("1", TransactionType.EXPENSE, 4000, otherExpCategory.id, otherExpCategory.name, otherExpCategory.iconKey, today, "صيانة")
        )
        fakeTransactionRepository.addTransaction(
            TransactionItem("2", TransactionType.INCOME, 9000, otherIncCategory.id, otherIncCategory.name, otherIncCategory.iconKey, today, "مكافأة")
        )
        advanceUntilIdle()

        // اختيار تصنيف "مصروفات أخرى" عبر معرفه الفريد
        viewModel.setSelectedCategoryId("cat_exp_other")
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(1, state.transactions.size)
        assertEquals("cat_exp_other", state.transactions.first().categoryId)

        // اختيار تصنيف "إيرادات أخرى" عبر معرفه الفريد
        viewModel.setSelectedCategoryId("cat_inc_other")
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(1, state.transactions.size)
        assertEquals("cat_inc_other", state.transactions.first().categoryId)
    }

    @Test
    fun `test filtered totals calculation - expenses, income, and net total`() = runTest {
        val today = LocalDate.now().toString()
        fakeTransactionRepository.addTransaction(
            TransactionItem("1", TransactionType.EXPENSE, 15000, foodCategory.id, foodCategory.name, foodCategory.iconKey, today, "وليمة")
        )
        fakeTransactionRepository.addTransaction(
            TransactionItem("2", TransactionType.EXPENSE, 5000, transportCategory.id, transportCategory.name, transportCategory.iconKey, today, "وقود")
        )
        fakeTransactionRepository.addTransaction(
            TransactionItem("3", TransactionType.INCOME, 50000, salaryCategory.id, salaryCategory.name, salaryCategory.iconKey, today, "دفعة راتب")
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("إجمالي المصروفات يجب أن يكون 20,000", 20000L, state.totalExpenses)
        assertEquals("إجمالي الإيرادات يجب أن يكون 50,000", 50000L, state.totalIncome)
        assertEquals("صافي التدفق يجب أن يكون 30,000", 30000L, state.netTotal)
    }
}

// محاكيات المستودع للاختبارات
private class FakeHistoryTxRepo : TransactionRepository {
    val items = mutableListOf<TransactionItem>()
    private val flow = MutableStateFlow<List<TransactionItem>>(emptyList())

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
        items.add(0, transaction)
        flow.value = items.toList()
        return Result.success(Unit)
    }

    override suspend fun updateTransaction(transaction: TransactionItem): Result<Unit> {
        val index = items.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            items[index] = transaction
            flow.value = items.toList()
            return Result.success(Unit)
        }
        return Result.failure(NoSuchElementException())
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> {
        val removed = items.removeAll { it.id == id }
        flow.value = items.toList()
        return if (removed) Result.success(Unit) else Result.failure(NoSuchElementException())
    }
}

private class FakeHistoryCatRepo(initialCategories: List<Category>) : CategoryRepository {
    private val categories = initialCategories.toMutableList()
    private val flow = MutableStateFlow<List<Category>>(categories)

    override fun getAllCategories(): Flow<List<Category>> = flow

    override fun getActiveCategories(type: TransactionType?): Flow<List<Category>> =
        flow.map { list -> list.filter { it.isActive && (type == null || it.type == type) } }

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

    override suspend fun seedDefaultCategories(): Result<Unit> = Result.success(Unit)
}
