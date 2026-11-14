package com.masroofi.app.presentation

import com.masroofi.app.data.backup.BackupManager
import com.masroofi.app.data.backup.BackupPayload
import com.masroofi.app.data.backup.BackupSummary
import com.masroofi.app.data.preferences.ThemeMode
import com.masroofi.app.data.preferences.UserPreferences
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.MonthlyBudget
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.CategoryRepository
import com.masroofi.app.domain.repository.MonthlyBudgetRepository
import com.masroofi.app.domain.repository.TransactionRepository
import com.masroofi.app.presentation.screens.settings.SettingsViewModel
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.YearMonth

/**
 * اختبارات وحدة شاملة لنموذج عرض شاشة الإعدادات والتصنيفات والميزانيات (SettingsViewModelTest)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePreferencesManager: FakePreferencesManager
    private lateinit var fakeCategoryRepository: FakeSettingsCategoryRepo
    private lateinit var fakeBudgetRepository: FakeSettingsBudgetRepo
    private lateinit var fakeTransactionRepository: FakeSettingsTxRepo
    private lateinit var fakeBackupManager: FakeTestBackupManager
    private lateinit var viewModel: SettingsViewModel

    private val foodCategory = Category("cat_food", "طعام ومشروبات", TransactionType.EXPENSE, "fastfood", isDefault = true, isActive = true)
    private val salaryCategory = Category("cat_salary", "الراتب", TransactionType.INCOME, "payments", isDefault = true, isActive = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        fakePreferencesManager = FakePreferencesManager()
        fakeCategoryRepository = FakeSettingsCategoryRepo(mutableListOf(foodCategory, salaryCategory))
        fakeBudgetRepository = FakeSettingsBudgetRepo()
        fakeTransactionRepository = FakeSettingsTxRepo()
        fakeBackupManager = FakeTestBackupManager()

        viewModel = SettingsViewModel(
            preferencesManager = fakePreferencesManager,
            categoryRepository = fakeCategoryRepository,
            monthlyBudgetRepository = fakeBudgetRepository,
            transactionRepository = fakeTransactionRepository,
            backupManager = fakeBackupManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test save and switch theme modes (LIGHT, DARK, SYSTEM)`() = runTest {
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)

        // 1. التبديل إلى الوضع الداكن
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

        // 2. التبديل إلى الوضع الفاتح
        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)

        // 3. العودة إلى التلقائي
        viewModel.setThemeMode(ThemeMode.SYSTEM)
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `test add new category successfully`() = runTest {
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.allCategories.size)

        viewModel.saveCategory(
            id = null,
            name = "مواصلات ووقود",
            type = TransactionType.EXPENSE,
            iconKey = "directions_car"
        )
        advanceUntilIdle()

        val categories = viewModel.uiState.value.allCategories
        assertEquals(3, categories.size)
        val added = categories.find { it.name == "مواصلات ووقود" }
        assertNotNull(added)
        assertEquals(TransactionType.EXPENSE, added?.type)
        assertEquals("directions_car", added?.iconKey)
        assertTrue(added?.isActive == true)
        assertEquals("تمت إضافة التصنيف بنجاح", viewModel.uiState.value.successMessage)
    }

    @Test
    fun `test add category prevents empty or blank name`() = runTest {
        advanceUntilIdle()
        viewModel.saveCategory(
            id = null,
            name = "   ",
            type = TransactionType.EXPENSE,
            iconKey = "fastfood"
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.allCategories.size)
        assertEquals("يرجى كتابة اسم التصنيف بالعربية", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `test add category prevents duplicate name for same type`() = runTest {
        advanceUntilIdle()
        viewModel.saveCategory(
            id = null,
            name = "طعام ومشروبات",
            type = TransactionType.EXPENSE,
            iconKey = "fastfood"
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.allCategories.size)
        assertEquals("يوجد تصنيف آخر بنفس الاسم لنفس النوع", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `test toggle category active state`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.allCategories.first().isActive)

        // تعطيل التصنيف
        viewModel.toggleCategoryActive(foodCategory)
        advanceUntilIdle()

        val updated = viewModel.uiState.value.allCategories.find { it.id == foodCategory.id }
        assertFalse("يجب أن يصبح التصنيف معطلاً", updated?.isActive ?: true)

        // إعادة التفعيل
        viewModel.toggleCategoryActive(updated!!)
        advanceUntilIdle()

        val reactivated = viewModel.uiState.value.allCategories.find { it.id == foodCategory.id }
        assertTrue("يجب إعادة تفعيل التصنيف", reactivated?.isActive ?: false)
    }

    @Test
    fun `test category with past transactions cannot change type or be deleted`() = runTest {
        // تسجيل وجود عمليات مرتبطة بتصنيف الطعام
        fakeCategoryRepository.setTransactionCount(foodCategory.id, 5)
        advanceUntilIdle()

        // 1. محاولة حذف التصنيف المرتبط بعمليات
        viewModel.deleteCategory(foodCategory)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.errorMessage?.contains("لا يمكن حذف تصنيف مسجل به") == true)

        // 2. محاولة تغيير نوع التصنيف المرتبط بعمليات من EXPENSE إلى INCOME
        viewModel.saveCategory(
            id = foodCategory.id,
            name = foodCategory.name,
            type = TransactionType.INCOME,
            iconKey = foodCategory.iconKey
        )
        advanceUntilIdle()
        assertEquals("لا يمكن تغيير نوع تصنيف مرتبط بعمليات مالية سابقة", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `test set and update monthly budget and review history`() = runTest {
        val currentYm = YearMonth.now().toString()
        val pastYm = YearMonth.now().minusMonths(1).toString()

        // تحديد ميزانية الشهر الحالي
        viewModel.setBudgetForMonth(currentYm, 120000L)
        advanceUntilIdle()
        assertEquals(120000L, viewModel.uiState.value.currentMonthBudget)

        // إضافة ميزانية للشهر السابق
        viewModel.setBudgetForMonth(pastYm, 95000L)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.allBudgets.size)
        val pastBudget = viewModel.uiState.value.allBudgets.find { it.yearMonth == pastYm }
        assertEquals(95000L, pastBudget?.amount)

        // تعديل ميزانية الشهر الحالي والتأكد من عدم تكرار السجل
        viewModel.setBudgetForMonth(currentYm, 150000L)
        advanceUntilIdle()
        assertEquals(150000L, viewModel.uiState.value.currentMonthBudget)
        assertEquals("يجب ألا يتكرر الشهر في السجل", 2, viewModel.uiState.value.allBudgets.size)
    }

    @Test
    fun `test exportBackup writes json to stream and sets successMessage`() = runTest {
        fakeBackupManager.backupJsonToReturn = """{"appName":"Masroof"}"""
        val outStream = ByteArrayOutputStream()

        viewModel.exportBackup(outStream)
        advanceUntilIdle()

        val written = outStream.toString(Charsets.UTF_8.name())
        assertEquals("""{"appName":"Masroof"}""", written)
        assertEquals("تم إنشاء النسخة الاحتياطية بنجاح", viewModel.uiState.value.successMessage)
    }

    @Test
    fun `test prepareRestore with valid json sets pendingRestoreSummary and opens confirmation`() = runTest {
        val summary = BackupSummary(
            createdAt = "2026-11-11",
            transactionCount = 10,
            categoryCount = 5,
            budgetCount = 2,
            payload = BackupPayload(createdAt = "2026-11-11")
        )
        fakeBackupManager.validateResult = Result.success(summary)
        val inStream = ByteArrayInputStream("valid_json".toByteArray(Charsets.UTF_8))

        viewModel.prepareRestore(inStream)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isRestoreConfirmationOpen)
        assertEquals(summary, viewModel.uiState.value.pendingRestoreSummary)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `test prepareRestore with invalid json sets errorMessage and does not open confirmation`() = runTest {
        fakeBackupManager.validateResult = Result.failure(IllegalArgumentException("تنسيق غير صالح"))
        val inStream = ByteArrayInputStream("bad_json".toByteArray(Charsets.UTF_8))

        viewModel.prepareRestore(inStream)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRestoreConfirmationOpen)
        assertNull(viewModel.uiState.value.pendingRestoreSummary)
        assertEquals("تنسيق غير صالح", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `test confirmRestore calls backupManager and shows success message`() = runTest {
        val summary = BackupSummary(
            createdAt = "2026-11-11",
            transactionCount = 10,
            categoryCount = 5,
            budgetCount = 2,
            payload = BackupPayload(createdAt = "2026-11-11")
        )
        fakeBackupManager.validateResult = Result.success(summary)
        fakeBackupManager.restoreResult = Result.success(Unit)

        val inStream = ByteArrayInputStream("valid_json".toByteArray(Charsets.UTF_8))
        viewModel.prepareRestore(inStream)
        advanceUntilIdle()

        viewModel.confirmRestore()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRestoreConfirmationOpen)
        assertNull(viewModel.uiState.value.pendingRestoreSummary)
        assertEquals(summary.payload, fakeBackupManager.restoredPayload)
        assertTrue(viewModel.uiState.value.successMessage?.contains("تمت استعادة البيانات بنجاح") == true)
    }

    @Test
    fun `test cancelRestore closes confirmation and clears pending summary`() = runTest {
        val summary = BackupSummary(
            createdAt = "2026-11-11",
            transactionCount = 3,
            categoryCount = 2,
            budgetCount = 1,
            payload = BackupPayload(createdAt = "2026-11-11")
        )
        fakeBackupManager.validateResult = Result.success(summary)

        viewModel.prepareRestore(ByteArrayInputStream("valid".toByteArray(Charsets.UTF_8)))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isRestoreConfirmationOpen)

        viewModel.cancelRestore()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRestoreConfirmationOpen)
        assertNull(viewModel.uiState.value.pendingRestoreSummary)
    }

    @Test
    fun `test exportCsv exports transactions and sets successMessage`() = runTest {
        val outStream = ByteArrayOutputStream()
        viewModel.exportCsv(outStream)
        advanceUntilIdle()

        val csv = outStream.toString(Charsets.UTF_8.name())
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("التاريخ,النوع,التصنيف,المبلغ (ريال يمني),الملاحظات"))
        assertTrue(viewModel.uiState.value.successMessage?.contains("تم تصدير") == true)
    }

    @Test
    fun `test currency selection sheet open and dismiss`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isCurrencySelectionOpen)

        viewModel.openCurrencySelection()
        assertTrue(viewModel.uiState.value.isCurrencySelectionOpen)

        viewModel.dismissCurrencySelection()
        assertFalse(viewModel.uiState.value.isCurrencySelectionOpen)
    }

    @Test
    fun `test select currency with zero transactions updates directly without warning`() = runTest {
        advanceUntilIdle()
        assertEquals("YER", viewModel.uiState.value.selectedCurrencyCode)
        assertFalse(viewModel.uiState.value.hasExistingTransactions)

        // اختيار الريال السعودي SAR عندما لا توجد أي عمليات
        viewModel.selectCurrency("SAR")
        advanceUntilIdle()

        assertEquals("SAR", viewModel.uiState.value.selectedCurrencyCode)
        assertFalse(viewModel.uiState.value.showCurrencyChangeWarningDialog)
        assertFalse(viewModel.uiState.value.isCurrencySelectionOpen)
    }

    @Test
    fun `test select currency with existing transactions triggers confirmation warning`() = runTest {
        // إنشاء نموذج عرض بحساب يحتوي على عمليات مسبقة
        val txRepoWithData = FakeSettingsTxRepo(
            mutableListOf(
                TransactionItem(
                    id = "tx_1",
                    type = TransactionType.EXPENSE,
                    amount = 5000L,
                    categoryId = "cat_food",
                    categoryName = "طعام",
                    transactionDate = "2026-11-10"
                )
            )
        )
        val testViewModel = SettingsViewModel(
            preferencesManager = fakePreferencesManager,
            categoryRepository = fakeCategoryRepository,
            monthlyBudgetRepository = fakeBudgetRepository,
            transactionRepository = txRepoWithData,
            backupManager = fakeBackupManager
        )
        advanceUntilIdle()
        assertTrue(testViewModel.uiState.value.hasExistingTransactions)

        // محاولة تغيير العملة إلى الدينار الكويتي KWD
        testViewModel.selectCurrency("KWD")
        advanceUntilIdle()

        // يجب ألا تتغير العملة فوراً، بل تظهر نافذة تأكيد التحذير
        assertEquals("YER", testViewModel.uiState.value.selectedCurrencyCode)
        assertTrue(testViewModel.uiState.value.showCurrencyChangeWarningDialog)
        assertEquals("KWD", testViewModel.uiState.value.pendingCurrencyCode)

        // إلغاء التغيير
        testViewModel.dismissCurrencyChangeWarning()
        advanceUntilIdle()

        assertEquals("YER", testViewModel.uiState.value.selectedCurrencyCode)
        assertFalse(testViewModel.uiState.value.showCurrencyChangeWarningDialog)
        assertNull(testViewModel.uiState.value.pendingCurrencyCode)

        // المحاولة مرة أخرى وتأكيد التغيير
        testViewModel.selectCurrency("KWD")
        advanceUntilIdle()
        assertTrue(testViewModel.uiState.value.showCurrencyChangeWarningDialog)

        testViewModel.confirmCurrencyChange()
        advanceUntilIdle()

        assertEquals("KWD", testViewModel.uiState.value.selectedCurrencyCode)
        assertFalse(testViewModel.uiState.value.showCurrencyChangeWarningDialog)
        assertNull(testViewModel.uiState.value.pendingCurrencyCode)
        assertFalse(testViewModel.uiState.value.isCurrencySelectionOpen)
    }

    @Test
    fun `test google sign in dialog state toggle`() = runTest {
        assertFalse(viewModel.uiState.value.showGoogleSignInDialog)

        viewModel.openGoogleSignInDialog()
        assertTrue(viewModel.uiState.value.showGoogleSignInDialog)

        viewModel.dismissGoogleSignInDialog()
        assertFalse(viewModel.uiState.value.showGoogleSignInDialog)
    }

    @Test
    fun `test dismiss cloud restore confirmation dialog`() = runTest {
        assertFalse(viewModel.uiState.value.isCloudRestoreConfirmationOpen)
        assertNull(viewModel.uiState.value.pendingCloudRestoreMetadata)

        viewModel.dismissCloudRestoreConfirmation()
        assertFalse(viewModel.uiState.value.isCloudRestoreConfirmationOpen)
        assertNull(viewModel.uiState.value.pendingCloudRestoreMetadata)
    }

    @Test
    fun `test pdf export dialog open close and state`() = runTest {
        assertFalse(viewModel.uiState.value.isPdfExportDialogOpen)
        assertNull(viewModel.uiState.value.generatedPdfFile)

        viewModel.openPdfExportDialog()
        assertTrue(viewModel.uiState.value.isPdfExportDialogOpen)

        viewModel.closePdfExportDialog()
        assertFalse(viewModel.uiState.value.isPdfExportDialogOpen)

        viewModel.dismissGeneratedPdfDialog()
        assertNull(viewModel.uiState.value.generatedPdfFile)
    }
}

/**
 * فئة وهمية لإدارة التفضيلات في اختبارات الوحدة دون استدعاء نظام ملفات أندرويد
 */
class FakePreferencesManager : UserPreferences {
    private val _themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    private val _currencyFlow = MutableStateFlow("YER")
    private val _hasSelectedCurrencyFlow = MutableStateFlow(false)

    override val themeMode: Flow<ThemeMode> = _themeModeFlow
    override val isDarkMode: Flow<Boolean> = _themeModeFlow.map { it == ThemeMode.DARK }
    override val selectedCurrencyCode: Flow<String> = _currencyFlow
    override val hasSelectedCurrency: Flow<Boolean> = _hasSelectedCurrencyFlow

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeModeFlow.value = mode
    }

    override suspend fun setDarkMode(isDark: Boolean) {
        setThemeMode(if (isDark) ThemeMode.DARK else ThemeMode.LIGHT)
    }

    override suspend fun setSelectedCurrencyCode(code: String) {
        _currencyFlow.value = code
    }

    override suspend fun setHasSelectedCurrency(hasSelected: Boolean) {
        _hasSelectedCurrencyFlow.value = hasSelected
    }

    override suspend fun setCloudCredentials(apiKey: String, projectId: String) {
        // لا حاجة لعمليات إضافية في بيئة الاختبار
    }
}

class FakeSettingsCategoryRepo(
    private val categories: MutableList<Category>
) : CategoryRepository {
    private val flow = MutableStateFlow<List<Category>>(categories)
    private val countsMap = mutableMapOf<String, Int>()

    fun setTransactionCount(id: String, count: Int) {
        countsMap[id] = count
    }

    override fun getAllCategories(): Flow<List<Category>> = flow

    override fun getActiveCategories(type: TransactionType?): Flow<List<Category>> {
        return flow.map { list ->
            list.filter { it.isActive && (type == null || it.type == type) }
        }
    }

    override fun getCategoryById(id: String): Flow<Category?> = flow.map { list -> list.find { it.id == id } }

    override suspend fun addCategory(category: Category): Result<Unit> {
        categories.add(category)
        flow.value = ArrayList(categories)
        return Result.success(Unit)
    }

    override suspend fun updateCategory(category: Category): Result<Unit> {
        val index = categories.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            categories[index] = category
            flow.value = ArrayList(categories)
        }
        return Result.success(Unit)
    }

    override suspend fun setCategoryActive(id: String, isActive: Boolean): Result<Unit> {
        val index = categories.indexOfFirst { it.id == id }
        if (index >= 0) {
            categories[index] = categories[index].copy(isActive = isActive)
            flow.value = ArrayList(categories)
        }
        return Result.success(Unit)
    }

    override suspend fun deleteCategory(id: String): Result<Unit> {
        val count = countsMap[id] ?: 0
        if (count > 0) return Result.failure(IllegalStateException("مرتبط بعمليات"))
        categories.removeAll { it.id == id }
        flow.value = ArrayList(categories)
        return Result.success(Unit)
    }

    override suspend fun getTransactionCountForCategory(id: String): Int {
        return countsMap[id] ?: 0
    }

    override suspend fun seedDefaultCategories(): Result<Unit> = Result.success(Unit)
}

class FakeSettingsBudgetRepo : MonthlyBudgetRepository {
    private val budgets = mutableListOf<MonthlyBudget>()
    private val flow = MutableStateFlow<List<MonthlyBudget>>(emptyList())

    override fun getBudgetForMonth(yearMonth: String): Flow<MonthlyBudget?> {
        return flow.map { list -> list.find { it.yearMonth == yearMonth } }
    }

    override fun getAllBudgets(): Flow<List<MonthlyBudget>> = flow

    override suspend fun setBudgetForMonth(yearMonth: String, amount: Long): Result<Unit> {
        val index = budgets.indexOfFirst { it.yearMonth == yearMonth }
        if (index >= 0) {
            budgets[index] = budgets[index].copy(amount = amount)
        } else {
            budgets.add(MonthlyBudget("id_$yearMonth", yearMonth, amount))
        }
        flow.value = ArrayList(budgets)
        return Result.success(Unit)
    }

    override suspend fun deleteBudget(id: String): Result<Unit> {
        budgets.removeAll { it.id == id }
        flow.value = ArrayList(budgets)
        return Result.success(Unit)
    }
}

class FakeSettingsTxRepo(
    private val transactions: MutableList<TransactionItem> = mutableListOf()
) : TransactionRepository {
    private val flow = MutableStateFlow<List<TransactionItem>>(transactions)

    fun setTransactions(list: List<TransactionItem>) {
        transactions.clear()
        transactions.addAll(list)
        flow.value = ArrayList(transactions)
    }

    override fun getAllTransactions(): Flow<List<TransactionItem>> = flow
    override fun getTransactionsByMonth(yearMonth: String): Flow<List<TransactionItem>> = MutableStateFlow(emptyList())
    override fun getTransactionsByDateRange(startDate: String, endDate: String): Flow<List<TransactionItem>> = MutableStateFlow(emptyList())
    override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionItem>> = MutableStateFlow(emptyList())
    override fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionItem>> = MutableStateFlow(emptyList())
    override fun getTransactionById(id: String): Flow<TransactionItem?> = MutableStateFlow(null)
    override suspend fun addTransaction(transaction: TransactionItem): Result<Unit> = Result.success(Unit)
    override suspend fun updateTransaction(transaction: TransactionItem): Result<Unit> = Result.success(Unit)
    override suspend fun deleteTransaction(id: String): Result<Unit> = Result.success(Unit)
}

class FakeTestBackupManager : BackupManager {
    var backupJsonToReturn: String = "{}"
    var validateResult: Result<BackupSummary> = Result.failure(Exception("Error"))
    var restoreResult: Result<Unit> = Result.success(Unit)
    var restoredPayload: BackupPayload? = null

    override suspend fun createBackupJson(): String = backupJsonToReturn

    override fun validateBackupJson(jsonString: String): Result<BackupSummary> = validateResult

    override suspend fun restoreBackup(payload: BackupPayload): Result<Unit> {
        restoredPayload = payload
        return restoreResult
    }
}
