package com.masroofi.app.data.backup

import com.masroofi.app.data.dao.CategoryDao
import com.masroofi.app.data.dao.MonthlyBudgetDao
import com.masroofi.app.data.dao.TransactionDao
import com.masroofi.app.data.database.DefaultCategories
import com.masroofi.app.data.entity.CategoryEntity
import com.masroofi.app.data.entity.MonthlyBudgetEntity
import com.masroofi.app.data.entity.TransactionEntity
import com.masroofi.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * اختبارات وحدة شاملة لمدير النسخ الاحتياطي والاستعادة الذرية (BackupRestoreTest)
 * يتحقق من سلامة البنية والتحقق الصارم والاستعادة الذرية وحماية البيانات
 */
class BackupRestoreTest {

    private lateinit var fakeCategoryDao: TestCategoryDao
    private lateinit var fakeTransactionDao: TestTransactionDao
    private lateinit var fakeBudgetDao: TestBudgetDao
    private lateinit var backupRestoreManager: BackupRestoreManager

    @Before
    fun setup() {
        fakeCategoryDao = TestCategoryDao()
        fakeTransactionDao = TestTransactionDao()
        fakeBudgetDao = TestBudgetDao()

        backupRestoreManager = BackupRestoreManager(
            database = null,
            categoryDaoProvider = { fakeCategoryDao },
            transactionDaoProvider = { fakeTransactionDao },
            budgetDaoProvider = { fakeBudgetDao }
        )
    }

    @Test
    fun `test create backup JSON contains expected structure, version, and appName`() = runTest {
        // تجهيز بيانات تجريبية
        fakeCategoryDao.categories.add(
            CategoryEntity("cat_food", "طعام ومشروبات", TransactionType.EXPENSE, "fastfood", isDefault = true)
        )
        fakeTransactionDao.transactions.add(
            TransactionEntity("tx_1", TransactionType.EXPENSE, 25000L, "cat_food", "2026-11-11", "غداء")
        )
        fakeBudgetDao.budgets.add(
            MonthlyBudgetEntity("b_1", "2026-11", 300000L)
        )

        val json = backupRestoreManager.createBackupJson()

        assertTrue(json.contains("\"backupFormatVersion\": 1"))
        assertTrue(json.contains("\"appName\": \"Masroof\""))
        assertTrue(json.contains("\"currency\": \"YER\""))
        assertTrue(json.contains("طعام ومشروبات"))
        assertTrue(json.contains("25000"))
        assertTrue(json.contains("300000"))
    }

    @Test
    fun `test round-trip serialization and deserialization retains 100 percent data accuracy`() = runTest {
        fakeCategoryDao.categories.add(
            CategoryEntity("cat_custom", "تصنيف مخصص", TransactionType.EXPENSE, "star", isDefault = false, isActive = true)
        )
        fakeTransactionDao.transactions.add(
            TransactionEntity("tx_rt", TransactionType.EXPENSE, 75000L, "cat_custom", "2026-11-10", "ملاحظة دقيقة")
        )
        fakeBudgetDao.budgets.add(
            MonthlyBudgetEntity("b_rt", "2026-10", 150000L)
        )

        val json = backupRestoreManager.createBackupJson()
        val validateResult = backupRestoreManager.validateBackupJson(json)

        assertTrue(validateResult.isSuccess)
        val summary = validateResult.getOrThrow()

        assertEquals(1, summary.transactionCount)
        assertEquals(1, summary.categoryCount)
        assertEquals(1, summary.budgetCount)

        val payload = summary.payload
        assertEquals("Masroof", payload.appName)
        assertEquals("YER", payload.currency)
        assertEquals("tx_rt", payload.transactions[0].id)
        assertEquals(75000L, payload.transactions[0].amount)
        assertEquals("ملاحظة دقيقة", payload.transactions[0].note)
        assertEquals("cat_custom", payload.categories[0].id)
        assertEquals("2026-10", payload.monthlyBudgets[0].yearMonth)
    }

    @Test
    fun `test validateBackupJson rejects malformed or corrupt JSON`() {
        val corruptJson = "{ corrupt_json_missing_quotes: true "
        val result = backupRestoreManager.validateBackupJson(corruptJson)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("صيغة JSON صالحة") == true)
    }

    @Test
    fun `test validateBackupJson rejects unsupported backup format version`() {
        val invalidVersionJson = """
            {
                "backupFormatVersion": 99,
                "createdAt": "2026-11-11",
                "appName": "Masroofi",
                "currency": "YER",
                "categories": [],
                "transactions": [],
                "monthlyBudgets": []
            }
        """.trimIndent()

        val result = backupRestoreManager.validateBackupJson(invalidVersionJson)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("إصدار النسخة الاحتياطية (99) غير متوافق") == true)
    }

    @Test
    fun `test validateBackupJson rejects mismatched appName`() {
        val wrongAppJson = """
            {
                "backupFormatVersion": 1,
                "createdAt": "2026-11-11",
                "appName": "OtherApp",
                "currency": "YER",
                "categories": [],
                "transactions": [],
                "monthlyBudgets": []
            }
        """.trimIndent()

        val result = backupRestoreManager.validateBackupJson(wrongAppJson)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("صالحة لتطبيق مصروف") == true)
    }

    @Test
    fun `test validateBackupJson rejects mismatched currency`() {
        val wrongCurrencyJson = """
            {
                "backupFormatVersion": 1,
                "createdAt": "2026-11-11",
                "appName": "Masroof",
                "currency": "XYZ_INVALID",
                "categories": [],
                "transactions": [],
                "monthlyBudgets": []
            }
        """.trimIndent()

        val result = backupRestoreManager.validateBackupJson(wrongCurrencyJson)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("غير معتمدة") == true)
    }

    @Test
    fun `test validateBackupJson rejects transaction with non-positive amount`() {
        val negativeAmountJson = """
            {
                "backupFormatVersion": 1,
                "createdAt": "2026-11-11",
                "appName": "Masroofi",
                "currency": "YER",
                "categories": [
                    { "id": "cat_test", "name": "اختبار", "type": "EXPENSE", "iconKey": "star", "isDefault": false, "isActive": true }
                ],
                "transactions": [
                    { "id": "tx_bad", "type": "EXPENSE", "amount": -500, "categoryId": "cat_test", "transactionDate": "2026-11-11" }
                ],
                "monthlyBudgets": []
            }
        """.trimIndent()

        val result = backupRestoreManager.validateBackupJson(negativeAmountJson)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("بمبلغ غير صالح") == true)
    }

    @Test
    fun `test validateBackupJson rejects transaction referencing nonexistent categoryId`() {
        val orphanTxJson = """
            {
                "backupFormatVersion": 1,
                "createdAt": "2026-11-11",
                "appName": "Masroofi",
                "currency": "YER",
                "categories": [
                    { "id": "cat_known", "name": "معروف", "type": "EXPENSE", "iconKey": "star", "isDefault": false, "isActive": true }
                ],
                "transactions": [
                    { "id": "tx_orphan", "type": "EXPENSE", "amount": 1000, "categoryId": "cat_unknown_ghost", "transactionDate": "2026-11-11" }
                ],
                "monthlyBudgets": []
            }
        """.trimIndent()

        val result = backupRestoreManager.validateBackupJson(orphanTxJson)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("تشير إلى تصنيف غير موجود") == true)
    }

    @Test
    fun `test validateBackupJson rejects invalid transaction date format`() {
        val invalidDateJson = """
            {
                "backupFormatVersion": 1,
                "createdAt": "2026-11-11",
                "appName": "Masroofi",
                "currency": "YER",
                "categories": [
                    { "id": "cat_date", "name": "تاريخ", "type": "EXPENSE", "iconKey": "star", "isDefault": false, "isActive": true }
                ],
                "transactions": [
                    { "id": "tx_date", "type": "EXPENSE", "amount": 1000, "categoryId": "cat_date", "transactionDate": "invalid-date-format" }
                ],
                "monthlyBudgets": []
            }
        """.trimIndent()

        val result = backupRestoreManager.validateBackupJson(invalidDateJson)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("تاريخ العملية") == true)
    }

    @Test
    fun `test restoreBackup atomically replaces all tables and preserves default categories`() = runTest {
        // إضافة بيانات سابقة قبل الاستعادة
        fakeCategoryDao.categories.add(
            CategoryEntity("old_cat", "قديم", TransactionType.EXPENSE, "icon", isDefault = false)
        )
        fakeTransactionDao.transactions.add(
            TransactionEntity("old_tx", TransactionType.EXPENSE, 5000L, "old_cat", "2026-11-01")
        )
        fakeBudgetDao.budgets.add(
            MonthlyBudgetEntity("old_b", "2026-10", 100000L)
        )

        // تجهيز حمولة النسخة الاحتياطية الجديدة
        val newPayload = BackupPayload(
            backupFormatVersion = 1,
            createdAt = "2026-11-11 12:00:00",
            appName = "Masroofi",
            currency = "YER",
            categories = listOf(
                CategoryBackupDto("cat_restored", "تصنيف مستعاد", "EXPENSE", "icon_new", isDefault = false, isActive = true)
            ),
            transactions = listOf(
                TransactionBackupDto("tx_restored", "EXPENSE", 32000L, "cat_restored", "2026-11-11", "عملية مستعادة")
            ),
            monthlyBudgets = listOf(
                MonthlyBudgetBackupDto("b_restored", "2026-11", 450000L)
            )
        )

        val restoreResult = backupRestoreManager.restoreBackup(newPayload)
        assertTrue(restoreResult.isSuccess)

        // التحقق من مسح البيانات القديمة بالكامل
        assertNull(fakeTransactionDao.transactions.find { it.id == "old_tx" })
        assertNull(fakeBudgetDao.budgets.find { it.id == "old_b" })

        // التحقق من وجود البيانات المستعادة الجديدة
        val restoredTx = fakeTransactionDao.transactions.find { it.id == "tx_restored" }
        assertNotNull(restoredTx)
        assertEquals(32000L, restoredTx?.amount)

        val restoredBudget = fakeBudgetDao.budgets.find { it.id == "b_restored" }
        assertNotNull(restoredBudget)
        assertEquals(450000L, restoredBudget?.amount)

        // التحقق من دمج التصنيفات الافتراضية لمنع ضياعها
        val defaultIds = DefaultCategories.allDefaultCategories.map { it.id }
        for (defaultId in defaultIds) {
            assertTrue("يجب أن يحتوي جدول التصنيفات على التصنيف الافتراضي: $defaultId",
                fakeCategoryDao.categories.any { it.id == defaultId }
            )
        }
    }
}

// ==========================================
// فئات تجريبية مزيفة لدعم الاختبارات المعزولة
// ==========================================

private class TestCategoryDao : CategoryDao {
    val categories = mutableListOf<CategoryEntity>()

    override suspend fun insertCategories(categories: List<CategoryEntity>) {
        this.categories.addAll(categories)
    }

    override suspend fun insertCategory(category: CategoryEntity): Long {
        categories.add(category)
        return 1L
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        val index = categories.indexOfFirst { it.id == category.id }
        if (index != -1) categories[index] = category
    }

    override fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = flowOf(categories)
    override fun getActiveCategoriesFlow(): Flow<List<CategoryEntity>> = flowOf(categories.filter { it.isActive })
    override fun getActiveCategoriesByTypeFlow(type: TransactionType): Flow<List<CategoryEntity>> =
        flowOf(categories.filter { it.isActive && it.type == type })

    override suspend fun getCategoryById(id: String): CategoryEntity? = categories.find { it.id == id }
    override fun getCategoryByIdFlow(id: String): Flow<CategoryEntity?> = flowOf(categories.find { it.id == id })

    override suspend fun setCategoryActive(id: String, isActive: Boolean): Int {
        val index = categories.indexOfFirst { it.id == id }
        if (index != -1) {
            categories[index] = categories[index].copy(isActive = isActive)
            return 1
        }
        return 0
    }

    override suspend fun countTransactionsForCategory(categoryId: String): Int = 0

    override suspend fun deleteCategory(category: CategoryEntity): Int {
        return if (categories.removeIf { it.id == category.id }) 1 else 0
    }

    override suspend fun deleteCategoryById(id: String): Int {
        return if (categories.removeIf { it.id == id }) 1 else 0
    }

    override suspend fun getAllCategoryEntities(): List<CategoryEntity> = categories.toList()

    override suspend fun insertOrReplaceCategoryEntities(categories: List<CategoryEntity>) {
        for (cat in categories) {
            this.categories.removeIf { it.id == cat.id }
            this.categories.add(cat)
        }
    }

    override suspend fun deleteAllCategories(): Int {
        val size = categories.size
        categories.clear()
        return size
    }
}

private class TestTransactionDao : TransactionDao {
    val transactions = mutableListOf<TransactionEntity>()

    override suspend fun insertTransaction(transaction: TransactionEntity): Long {
        transactions.add(transaction)
        return 1L
    }

    override suspend fun updateTransaction(transaction: TransactionEntity): Int {
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
            return 1
        }
        return 0
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity): Int {
        return if (transactions.removeIf { it.id == transaction.id }) 1 else 0
    }

    override suspend fun deleteTransactionById(id: String): Int {
        return if (transactions.removeIf { it.id == id }) 1 else 0
    }

    override suspend fun getTransactionById(id: String): TransactionEntity? = transactions.find { it.id == id }
    override fun getTransactionByIdFlow(id: String): Flow<TransactionEntity?> = flowOf(transactions.find { it.id == id })
    override fun getTransactionWithCategoryById(id: String): Flow<com.masroofi.app.data.entity.TransactionWithCategory?> = flowOf(null)
    override fun getAllTransactionsWithCategoryFlow(): Flow<List<com.masroofi.app.data.entity.TransactionWithCategory>> = flowOf(emptyList())
    override fun getTransactionsWithCategoryByMonthFlow(yearMonth: String): Flow<List<com.masroofi.app.data.entity.TransactionWithCategory>> = flowOf(emptyList())
    override fun getTransactionsWithCategoryByDateRangeFlow(startDate: String, endDate: String): Flow<List<com.masroofi.app.data.entity.TransactionWithCategory>> = flowOf(emptyList())
    override fun getTransactionsWithCategoryByTypeFlow(type: TransactionType): Flow<List<com.masroofi.app.data.entity.TransactionWithCategory>> = flowOf(emptyList())
    override fun getTransactionsWithCategoryByCategoryFlow(categoryId: String): Flow<List<com.masroofi.app.data.entity.TransactionWithCategory>> = flowOf(emptyList())

    override suspend fun getAllTransactionEntities(): List<TransactionEntity> = transactions.toList()

    override suspend fun insertTransactionEntities(transactions: List<TransactionEntity>) {
        this.transactions.addAll(transactions)
    }

    override suspend fun deleteAllTransactions(): Int {
        val size = transactions.size
        transactions.clear()
        return size
    }
}

private class TestBudgetDao : MonthlyBudgetDao {
    val budgets = mutableListOf<MonthlyBudgetEntity>()

    override suspend fun insertOrUpdateBudget(budget: MonthlyBudgetEntity): Long {
        budgets.removeIf { it.yearMonth == budget.yearMonth }
        budgets.add(budget)
        return 1L
    }

    override suspend fun insertBudget(budget: MonthlyBudgetEntity): Long {
        budgets.add(budget)
        return 1L
    }

    override suspend fun updateBudget(budget: MonthlyBudgetEntity): Int {
        val index = budgets.indexOfFirst { it.id == budget.id }
        if (index != -1) {
            budgets[index] = budget
            return 1
        }
        return 0
    }

    override suspend fun getBudgetForMonth(yearMonth: String): MonthlyBudgetEntity? = budgets.find { it.yearMonth == yearMonth }
    override fun getBudgetForMonthFlow(yearMonth: String): Flow<MonthlyBudgetEntity?> = flowOf(budgets.find { it.yearMonth == yearMonth })
    override fun getAllBudgetsFlow(): Flow<List<MonthlyBudgetEntity>> = flowOf(budgets)

    override suspend fun deleteBudget(budget: MonthlyBudgetEntity): Int {
        return if (budgets.removeIf { it.id == budget.id }) 1 else 0
    }

    override suspend fun deleteBudgetById(id: String): Int {
        return if (budgets.removeIf { it.id == id }) 1 else 0
    }

    override suspend fun getAllBudgetEntities(): List<MonthlyBudgetEntity> = budgets.toList()

    override suspend fun insertBudgetEntities(budgets: List<MonthlyBudgetEntity>) {
        for (b in budgets) {
            this.budgets.removeIf { it.yearMonth == b.yearMonth }
            this.budgets.add(b)
        }
    }

    override suspend fun deleteAllBudgets(): Int {
        val size = budgets.size
        budgets.clear()
        return size
    }
}
