package com.masroofi.app.presentation

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

        viewModel = SettingsViewModel(
            preferencesManager = fakePreferencesManager,
            categoryRepository = fakeCategoryRepository,
            monthlyBudgetRepository = fakeBudgetRepository,
            transactionRepository = fakeTransactionRepository
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
}

/**
 * فئة وهمية لإدارة التفضيلات في اختبارات الوحدة دون استدعاء نظام ملفات أندرويد
 */
class FakePreferencesManager : UserPreferences {
    private val _themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: Flow<ThemeMode> = _themeModeFlow
    override val isDarkMode: Flow<Boolean> = _themeModeFlow.map { it == ThemeMode.DARK }

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeModeFlow.value = mode
    }

    override suspend fun setDarkMode(isDark: Boolean) {
        setThemeMode(if (isDark) ThemeMode.DARK else ThemeMode.LIGHT)
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

class FakeSettingsTxRepo : TransactionRepository {
    override fun getAllTransactions(): Flow<List<TransactionItem>> = MutableStateFlow(emptyList())
    override fun getTransactionsByMonth(yearMonth: String): Flow<List<TransactionItem>> = MutableStateFlow(emptyList())
    override fun getTransactionsByDateRange(startDate: String, endDate: String): Flow<List<TransactionItem>> = MutableStateFlow(emptyList())
    override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionItem>> = MutableStateFlow(emptyList())
    override fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionItem>> = MutableStateFlow(emptyList())
    override fun getTransactionById(id: String): Flow<TransactionItem?> = MutableStateFlow(null)
    override suspend fun addTransaction(transaction: TransactionItem): Result<Unit> = Result.success(Unit)
    override suspend fun updateTransaction(transaction: TransactionItem): Result<Unit> = Result.success(Unit)
    override suspend fun deleteTransaction(id: String): Result<Unit> = Result.success(Unit)
}
