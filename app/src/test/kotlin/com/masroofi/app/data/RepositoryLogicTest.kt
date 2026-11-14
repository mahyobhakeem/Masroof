package com.masroofi.app.data

import com.masroofi.app.data.dao.CategoryDao
import com.masroofi.app.data.dao.MonthlyBudgetDao
import com.masroofi.app.data.dao.TransactionDao
import com.masroofi.app.data.entity.CategoryEntity
import com.masroofi.app.data.entity.MonthlyBudgetEntity
import com.masroofi.app.data.entity.TransactionEntity
import com.masroofi.app.data.entity.TransactionWithCategory
import com.masroofi.app.data.repository.CategoryRepositoryImpl
import com.masroofi.app.data.repository.MonthlyBudgetRepositoryImpl
import com.masroofi.app.data.repository.TransactionRepositoryImpl
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * اختبارات منطق المستودعات والتحقق من العمليات وقواعد البيانات
 */
class RepositoryLogicTest {

    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var fakeMonthlyBudgetDao: FakeMonthlyBudgetDao

    private lateinit var categoryRepository: CategoryRepositoryImpl
    private lateinit var transactionRepository: TransactionRepositoryImpl
    private lateinit var monthlyBudgetRepository: MonthlyBudgetRepositoryImpl

    @Before
    fun setup() {
        fakeCategoryDao = FakeCategoryDao()
        fakeTransactionDao = FakeTransactionDao()
        fakeMonthlyBudgetDao = FakeMonthlyBudgetDao()

        categoryRepository = CategoryRepositoryImpl(fakeCategoryDao)
        transactionRepository = TransactionRepositoryImpl(fakeTransactionDao)
        monthlyBudgetRepository = MonthlyBudgetRepositoryImpl(fakeMonthlyBudgetDao)
    }

    // 1. اختبار منع حذف تصنيف مستخدم في عمليات مالية سابقة
    @Test
    fun categoryRepository_rejectsDeletingCategoryWithTransactions() = runTest {
        val categoryId = "cat_used"
        fakeCategoryDao.categories.add(
            CategoryEntity(categoryId, "تصنيف مستخدم", TransactionType.EXPENSE, "fastfood")
        )
        // إضافة عملية ترتبط بهذا التصنيف
        fakeTransactionDao.transactions.add(
            TransactionEntity("tx_1", TransactionType.EXPENSE, 2000L, categoryId, "2026-11-10")
        )

        val result = categoryRepository.deleteCategory(categoryId)
        assertTrue("يجب أن يفشل حذف تصنيف مرتبط بعمليات", result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("لا يمكن حذف تصنيف مرتبط بعمليات مالية سابقة. يمكنك إلغاء تنشيط التصنيف بدلاً من حذفه.", result.exceptionOrNull()?.message)
    }

    // 2. اختبار السماح بحذف تصنيف غير مستخدم في أي عملية
    @Test
    fun categoryRepository_allowsDeletingUnusedCategory() = runTest {
        val categoryId = "cat_unused"
        fakeCategoryDao.categories.add(
            CategoryEntity(categoryId, "تصنيف حر", TransactionType.EXPENSE, "fastfood")
        )

        val result = categoryRepository.deleteCategory(categoryId)
        assertTrue("يجب أن ينجح حذف تصنيف غير مستخدم", result.isSuccess)
        assertNull(fakeCategoryDao.categories.find { it.id == categoryId })
    }

    // 3. اختبار تفعيل وإلغاء تفعيل التصنيف
    @Test
    fun categoryRepository_togglesActiveState() = runTest {
        val categoryId = "cat_toggle"
        fakeCategoryDao.categories.add(
            CategoryEntity(categoryId, "تصنيف قابل للتعطيل", TransactionType.EXPENSE, "fastfood", isActive = true)
        )

        val deactivateResult = categoryRepository.setCategoryActive(categoryId, false)
        assertTrue(deactivateResult.isSuccess)
        assertFalse(fakeCategoryDao.categories.first { it.id == categoryId }.isActive)

        val activateResult = categoryRepository.setCategoryActive(categoryId, true)
        assertTrue(activateResult.isSuccess)
        assertTrue(fakeCategoryDao.categories.first { it.id == categoryId }.isActive)
    }

    // 4. اختبار حفظ وتعديل وحذف عملية مالية
    @Test
    fun transactionRepository_crudOperations() = runTest {
        val tx = TransactionItem(
            id = "tx_crud",
            type = TransactionType.EXPENSE,
            amount = 4500L,
            categoryId = "cat_exp_food",
            transactionDate = "2026-11-10",
            note = "وجبة غداء"
        )

        // حفظ العملية
        val addResult = transactionRepository.addTransaction(tx)
        assertTrue(addResult.isSuccess)
        assertEquals(1, fakeTransactionDao.transactions.size)

        // تعديل العملية
        val updatedTx = tx.copy(amount = 5000L, note = "وجبة غداء مع مشروب")
        val updateResult = transactionRepository.updateTransaction(updatedTx)
        assertTrue(updateResult.isSuccess)
        assertEquals(5000L, fakeTransactionDao.transactions.first().amount)

        // حذف العملية
        val deleteResult = transactionRepository.deleteTransaction("tx_crud")
        assertTrue(deleteResult.isSuccess)
        assertTrue(fakeTransactionDao.transactions.isEmpty())
    }

    // 5. اختبار منع تكرار سجل الميزانية لنفس الشهر (تحديث السجل الحالي بدلاً من تكراره)
    @Test
    fun monthlyBudgetRepository_updatesBudgetForSameMonthWithoutDuplicate() = runTest {
        val month = "2026-11"

        // تعيين ميزانية أولى للشهر
        monthlyBudgetRepository.setBudgetForMonth(month, 100_000L)
        assertEquals(1, fakeMonthlyBudgetDao.budgets.size)
        assertEquals(100_000L, fakeMonthlyBudgetDao.budgets.first().amount)

        // تعيين ميزانية معدلة لنفس الشهر
        monthlyBudgetRepository.setBudgetForMonth(month, 130_000L)
        // يجب أن يظل عدد السجلات 1 فقط مع تحديث المبلغ
        assertEquals("يجب ألا يتكرر سجل الميزانية لنفس الشهر", 1, fakeMonthlyBudgetDao.budgets.size)
        assertEquals(130_000L, fakeMonthlyBudgetDao.budgets.first().amount)
        assertEquals(month, fakeMonthlyBudgetDao.budgets.first().yearMonth)
    }

    // 6. اختبار بذر التصنيفات الافتراضية
    @Test
    fun categoryRepository_seedsDefaultCategoriesSuccessfully() = runTest {
        val seedResult = categoryRepository.seedDefaultCategories()
        assertTrue(seedResult.isSuccess)
        assertEquals(16, fakeCategoryDao.categories.size)
    }
}

// محاكيات DAO للاختبارات السريعة والمستقلة
private class FakeCategoryDao : CategoryDao {
    val categories = mutableListOf<CategoryEntity>()
    private val flow = MutableStateFlow<List<CategoryEntity>>(categories)

    override suspend fun insertCategories(categories: List<CategoryEntity>) {
        categories.forEach { cat ->
            if (this.categories.none { it.id == cat.id }) {
                this.categories.add(cat)
            }
        }
        flow.value = this.categories.toList()
    }

    override suspend fun insertCategory(category: CategoryEntity): Long {
        categories.add(category)
        flow.value = categories.toList()
        return 1L
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        val index = categories.indexOfFirst { it.id == category.id }
        if (index != -1) {
            categories[index] = category
            flow.value = categories.toList()
        }
    }

    override fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = flow
    override fun getActiveCategoriesFlow(): Flow<List<CategoryEntity>> = flow.map { list -> list.filter { it.isActive } }
    override fun getActiveCategoriesByTypeFlow(type: TransactionType): Flow<List<CategoryEntity>> =
        flow.map { list -> list.filter { it.isActive && it.type == type } }

    override suspend fun getCategoryById(id: String): CategoryEntity? = categories.find { it.id == id }
    override fun getCategoryByIdFlow(id: String): Flow<CategoryEntity?> = flow.map { list -> list.find { it.id == id } }

    override suspend fun setCategoryActive(id: String, isActive: Boolean): Int {
        val cat = categories.find { it.id == id }
        return if (cat != null) {
            updateCategory(cat.copy(isActive = isActive))
            1
        } else 0
    }

    override suspend fun countTransactionsForCategory(categoryId: String): Int {
        return FakeTransactionDao.instance?.transactions?.count { it.categoryId == categoryId } ?: 0
    }

    override suspend fun deleteCategory(category: CategoryEntity): Int {
        val removed = categories.removeAll { it.id == category.id }
        flow.value = categories.toList()
        return if (removed) 1 else 0
    }

    override suspend fun deleteCategoryById(id: String): Int {
        val removed = categories.removeAll { it.id == id }
        flow.value = categories.toList()
        return if (removed) 1 else 0
    }

    override suspend fun getAllCategoryEntities(): List<CategoryEntity> = categories.toList()

    override suspend fun insertOrReplaceCategoryEntities(categories: List<CategoryEntity>) {
        this.categories.addAll(categories)
        flow.value = this.categories.toList()
    }

    override suspend fun deleteAllCategories(): Int {
        val count = categories.size
        categories.clear()
        flow.value = emptyList()
        return count
    }
}

private class FakeTransactionDao : TransactionDao {
    val transactions = mutableListOf<TransactionEntity>()
    private val flow = MutableStateFlow<List<TransactionEntity>>(transactions)

    init {
        instance = this
    }

    companion object {
        var instance: FakeTransactionDao? = null
    }

    override suspend fun insertTransaction(transaction: TransactionEntity): Long {
        transactions.add(transaction)
        flow.value = transactions.toList()
        return 1L
    }

    override suspend fun updateTransaction(transaction: TransactionEntity): Int {
        val index = transactions.indexOfFirst { it.id == transaction.id }
        return if (index != -1) {
            transactions[index] = transaction
            flow.value = transactions.toList()
            1
        } else 0
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity): Int {
        val removed = transactions.removeAll { it.id == transaction.id }
        flow.value = transactions.toList()
        return if (removed) 1 else 0
    }

    override suspend fun deleteTransactionById(id: String): Int {
        val removed = transactions.removeAll { it.id == id }
        flow.value = transactions.toList()
        return if (removed) 1 else 0
    }

    override suspend fun getTransactionById(id: String): TransactionEntity? = transactions.find { it.id == id }
    override fun getTransactionByIdFlow(id: String): Flow<TransactionEntity?> = flow.map { list -> list.find { it.id == id } }

    override fun getTransactionWithCategoryById(id: String): Flow<TransactionWithCategory?> =
        flow.map { list -> list.find { it.id == id }?.let { TransactionWithCategory(it, null) } }

    override fun getAllTransactionsWithCategoryFlow(): Flow<List<TransactionWithCategory>> =
        flow.map { list -> list.map { TransactionWithCategory(it, null) } }

    override fun getTransactionsWithCategoryByMonthFlow(yearMonth: String): Flow<List<TransactionWithCategory>> =
        flow.map { list -> list.filter { it.transactionDate.startsWith(yearMonth) }.map { TransactionWithCategory(it, null) } }

    override fun getTransactionsWithCategoryByDateRangeFlow(startDate: String, endDate: String): Flow<List<TransactionWithCategory>> =
        flow.map { list -> list.filter { it.transactionDate in startDate..endDate }.map { TransactionWithCategory(it, null) } }

    override fun getTransactionsWithCategoryByTypeFlow(type: TransactionType): Flow<List<TransactionWithCategory>> =
        flow.map { list -> list.filter { it.type == type }.map { TransactionWithCategory(it, null) } }

    override fun getTransactionsWithCategoryByCategoryFlow(categoryId: String): Flow<List<TransactionWithCategory>> =
        flow.map { list -> list.filter { it.categoryId == categoryId }.map { TransactionWithCategory(it, null) } }

    override suspend fun getAllTransactionEntities(): List<TransactionEntity> = transactions.toList()

    override suspend fun insertTransactionEntities(transactions: List<TransactionEntity>) {
        this.transactions.addAll(transactions)
        flow.value = this.transactions.toList()
    }

    override suspend fun deleteAllTransactions(): Int {
        val count = transactions.size
        transactions.clear()
        flow.value = emptyList()
        return count
    }
}

private class FakeMonthlyBudgetDao : MonthlyBudgetDao {
    val budgets = mutableListOf<MonthlyBudgetEntity>()
    private val flow = MutableStateFlow<List<MonthlyBudgetEntity>>(budgets)

    override suspend fun insertOrUpdateBudget(budget: MonthlyBudgetEntity): Long {
        val index = budgets.indexOfFirst { it.yearMonth == budget.yearMonth }
        if (index != -1) {
            budgets[index] = budget
        } else {
            budgets.add(budget)
        }
        flow.value = budgets.toList()
        return 1L
    }

    override suspend fun insertBudget(budget: MonthlyBudgetEntity): Long {
        budgets.add(budget)
        flow.value = budgets.toList()
        return 1L
    }

    override suspend fun updateBudget(budget: MonthlyBudgetEntity): Int {
        val index = budgets.indexOfFirst { it.id == budget.id }
        return if (index != -1) {
            budgets[index] = budget
            flow.value = budgets.toList()
            1
        } else 0
    }

    override suspend fun getBudgetForMonth(yearMonth: String): MonthlyBudgetEntity? =
        budgets.find { it.yearMonth == yearMonth }

    override fun getBudgetForMonthFlow(yearMonth: String): Flow<MonthlyBudgetEntity?> =
        flow.map { list -> list.find { it.yearMonth == yearMonth } }

    override fun getAllBudgetsFlow(): Flow<List<MonthlyBudgetEntity>> = flow

    override suspend fun deleteBudget(budget: MonthlyBudgetEntity): Int {
        val removed = budgets.removeAll { it.id == budget.id }
        flow.value = budgets.toList()
        return if (removed) 1 else 0
    }

    override suspend fun deleteBudgetById(id: String): Int {
        val removed = budgets.removeAll { it.id == id }
        flow.value = budgets.toList()
        return if (removed) 1 else 0
    }

    override suspend fun getAllBudgetEntities(): List<MonthlyBudgetEntity> = budgets.toList()

    override suspend fun insertBudgetEntities(budgets: List<MonthlyBudgetEntity>) {
        this.budgets.addAll(budgets)
        flow.value = this.budgets.toList()
    }

    override suspend fun deleteAllBudgets(): Int {
        val count = budgets.size
        budgets.clear()
        flow.value = emptyList()
        return count
    }
}
