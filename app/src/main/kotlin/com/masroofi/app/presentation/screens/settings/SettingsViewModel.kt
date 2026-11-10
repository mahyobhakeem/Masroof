package com.masroofi.app.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masroofi.app.data.preferences.ThemeMode
import com.masroofi.app.data.preferences.UserPreferences
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.MonthlyBudget
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.CategoryRepository
import com.masroofi.app.domain.repository.MonthlyBudgetRepository
import com.masroofi.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.UUID

/**
 * حالة واجهة شاشة الإعدادات وإدارة التصنيفات والميزانيات (SettingsUiState)
 */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currentMonthBudget: Long? = null,
    val allBudgets: List<MonthlyBudget> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val categoryTransactionCounts: Map<String, Int> = emptyMap(),
    val isCategoryManagementOpen: Boolean = false,
    val isBudgetManagementOpen: Boolean = false,
    val isAboutDialogOpen: Boolean = false,
    val categoryToEdit: Category? = null,
    val showAddEditCategoryDialog: Boolean = false,
    val addEditCategoryType: TransactionType = TransactionType.EXPENSE,
    val budgetToEdit: MonthlyBudget? = null,
    val showEditBudgetDialog: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val expenseCategories: List<Category>
        get() = allCategories.filter { it.type == TransactionType.EXPENSE }

    val incomeCategories: List<Category>
        get() = allCategories.filter { it.type == TransactionType.INCOME }
}

/**
 * نموذج العرض لشاشة الإعدادات وإدارة التصنيفات والميزانيات (SettingsViewModel)
 */
class SettingsViewModel(
    private val preferencesManager: UserPreferences,
    private val categoryRepository: CategoryRepository,
    private val monthlyBudgetRepository: MonthlyBudgetRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val currentMonthStr = YearMonth.now().toString()

    init {
        // مراقبة وضع المظهر من DataStore
        viewModelScope.launch {
            preferencesManager.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }

        // مراقبة ميزانية الشهر الحالي
        viewModelScope.launch {
            monthlyBudgetRepository.getBudgetForMonth(currentMonthStr).collect { currentBudget ->
                _uiState.update { it.copy(currentMonthBudget = currentBudget?.amount) }
            }
        }

        // مراقبة سجل الميزانيات لجميع الأشهر
        viewModelScope.launch {
            monthlyBudgetRepository.getAllBudgets().collect { budgets ->
                _uiState.update { it.copy(allBudgets = budgets) }
            }
        }

        // مراقبة جميع التصنيفات وتحديث عدد العمليات المرتبطة بكل تصنيف
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                // تحديث عدادات العمليات لكل تصنيف
                val counts = categories.associate { cat ->
                    cat.id to categoryRepository.getTransactionCountForCategory(cat.id)
                }
                _uiState.update {
                    it.copy(
                        allCategories = categories,
                        categoryTransactionCounts = counts
                    )
                }
            }
        }
    }

    // ==========================================
    // 1. إدارة مظهر التطبيق (Theme Mode)
    // ==========================================

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    // ==========================================
    // 2. إدارة الميزانية الشهرية (Budget Management)
    // ==========================================

    fun setBudgetForMonth(yearMonth: String, amount: Long) {
        viewModelScope.launch {
            if (amount < 0) {
                _uiState.update { it.copy(errorMessage = "يجب ألا يكون مبلغ الميزانية سالباً") }
                return@launch
            }
            monthlyBudgetRepository.setBudgetForMonth(yearMonth, amount)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showEditBudgetDialog = false,
                            budgetToEdit = null,
                            successMessage = "تم حفظ ميزانية شهر $yearMonth بنجاح"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "فشل حفظ الميزانية") }
                }
        }
    }

    fun openBudgetManagement() {
        _uiState.update { it.copy(isBudgetManagementOpen = true) }
    }

    fun closeBudgetManagement() {
        _uiState.update { it.copy(isBudgetManagementOpen = false) }
    }

    fun openEditBudget(budget: MonthlyBudget?) {
        _uiState.update {
            it.copy(
                budgetToEdit = budget,
                showEditBudgetDialog = true
            )
        }
    }

    fun closeEditBudgetDialog() {
        _uiState.update {
            it.copy(
                showEditBudgetDialog = false,
                budgetToEdit = null
            )
        }
    }

    // ==========================================
    // 3. إدارة التصنيفات (Categories Management)
    // ==========================================

    fun openCategoryManagement() {
        _uiState.update { it.copy(isCategoryManagementOpen = true) }
    }

    fun closeCategoryManagement() {
        _uiState.update { it.copy(isCategoryManagementOpen = false) }
    }

    fun openAddCategory(type: TransactionType) {
        _uiState.update {
            it.copy(
                categoryToEdit = null,
                addEditCategoryType = type,
                showAddEditCategoryDialog = true
            )
        }
    }

    fun openEditCategory(category: Category) {
        _uiState.update {
            it.copy(
                categoryToEdit = category,
                addEditCategoryType = category.type,
                showAddEditCategoryDialog = true
            )
        }
    }

    fun closeAddEditCategoryDialog() {
        _uiState.update {
            it.copy(
                showAddEditCategoryDialog = false,
                categoryToEdit = null
            )
        }
    }

    fun toggleCategoryActive(category: Category) {
        viewModelScope.launch {
            val newStatus = !category.isActive
            categoryRepository.setCategoryActive(category.id, newStatus)
                .onSuccess {
                    val statusText = if (newStatus) "تفعيل" else "تعطيل"
                    _uiState.update { it.copy(successMessage = "تم $statusText تصنيف \"${category.name}\" بنجاح") }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "فشل تحديث حالة التصنيف") }
                }
        }
    }

    fun saveCategory(
        id: String?,
        name: String,
        type: TransactionType,
        iconKey: String
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "يرجى كتابة اسم التصنيف بالعربية") }
            return
        }

        // فحص تكرار الاسم لنفس النوع
        val existingWithSameName = _uiState.value.allCategories.find {
            it.type == type && it.name.trim().equals(trimmedName, ignoreCase = true) && it.id != id
        }
        if (existingWithSameName != null) {
            _uiState.update { it.copy(errorMessage = "يوجد تصنيف آخر بنفس الاسم لنفس النوع") }
            return
        }

        viewModelScope.launch {
            if (id != null) {
                // تعديل تصنيف قائم
                val existing = _uiState.value.allCategories.find { it.id == id }
                if (existing != null) {
                    // فحص حظر تغيير نوع التصنيف إذا كان مرتبطاً بعمليات
                    val txCount = categoryRepository.getTransactionCountForCategory(id)
                    if (existing.type != type && txCount > 0) {
                        _uiState.update { it.copy(errorMessage = "لا يمكن تغيير نوع تصنيف مرتبط بعمليات مالية سابقة") }
                        return@launch
                    }

                    val updated = existing.copy(
                        name = trimmedName,
                        type = type,
                        iconKey = iconKey
                    )
                    categoryRepository.updateCategory(updated)
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    showAddEditCategoryDialog = false,
                                    categoryToEdit = null,
                                    successMessage = "تم تحديث التصنيف بنجاح"
                                )
                            }
                        }
                        .onFailure { error ->
                            _uiState.update { it.copy(errorMessage = error.message ?: "فشل تحديث التصنيف") }
                        }
                }
            } else {
                // إضافة تصنيف جديد
                val newCategory = Category(
                    id = "cat_custom_${UUID.randomUUID()}",
                    name = trimmedName,
                    type = type,
                    iconKey = iconKey,
                    isDefault = false,
                    isActive = true
                )
                categoryRepository.addCategory(newCategory)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                showAddEditCategoryDialog = false,
                                categoryToEdit = null,
                                successMessage = "تمت إضافة التصنيف بنجاح"
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.message ?: "فشل إضافة التصنيف") }
                    }
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            val count = categoryRepository.getTransactionCountForCategory(category.id)
            if (count > 0) {
                _uiState.update {
                    it.copy(errorMessage = "لا يمكن حذف تصنيف مسجل به $count عمليات سابقة. يمكنك إلغاء تنشيطه (تعطيله) بدلاً من حذفه.")
                }
                return@launch
            }

            categoryRepository.deleteCategory(category.id)
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "تم حذف التصنيف بنجاح") }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "فشل حذف التصنيف") }
                }
        }
    }

    // ==========================================
    // 4. نافذة حول التطبيق وتصفية الرسائل
    // ==========================================

    fun openAboutDialog() {
        _uiState.update { it.copy(isAboutDialogOpen = true) }
    }

    fun closeAboutDialog() {
        _uiState.update { it.copy(isAboutDialogOpen = false) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

/**
 * مصنع إنشاء SettingsViewModel
 */
class SettingsViewModelFactory(
    private val preferencesManager: UserPreferences,
    private val categoryRepository: CategoryRepository,
    private val monthlyBudgetRepository: MonthlyBudgetRepository,
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(
                preferencesManager = preferencesManager,
                categoryRepository = categoryRepository,
                monthlyBudgetRepository = monthlyBudgetRepository,
                transactionRepository = transactionRepository
            ) as T
        }
        throw IllegalArgumentException("فئة نموذج العرض غير معروفة: ${modelClass.name}")
    }
}
