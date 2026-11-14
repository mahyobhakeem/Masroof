package com.masroofi.app.presentation.screens.settings

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masroofi.app.data.backup.BackupFileManager
import com.masroofi.app.data.backup.BackupManager
import com.masroofi.app.data.backup.BackupSummary
import com.masroofi.app.data.cloud.CloudBackupManager
import com.masroofi.app.data.cloud.CloudBackupMetadata
import com.masroofi.app.data.cloud.CloudBackupStatus
import com.masroofi.app.data.cloud.CloudUserAccount
import com.masroofi.app.data.export.CsvExporter
import com.masroofi.app.data.preferences.ThemeMode
import com.masroofi.app.data.preferences.UserPreferences
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.CurrencyCatalog
import com.masroofi.app.domain.model.MonthlyBudget
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.CategoryRepository
import com.masroofi.app.domain.repository.MonthlyBudgetRepository
import com.masroofi.app.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.YearMonth
import java.util.UUID

/**
 * حالة واجهة شاشة الإعدادات وإدارة التصنيفات والميزانيات (SettingsUiState)
 */
@Immutable
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedCurrencyCode: String = CurrencyCatalog.DEFAULT_CURRENCY_CODE,
    val isCurrencySelectionOpen: Boolean = false,
    val showCurrencyChangeWarningDialog: Boolean = false,
    val pendingCurrencyCode: String? = null,
    val hasExistingTransactions: Boolean = false,
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
    val isRestoreSelectionOpen: Boolean = false,
    val localBackupFiles: List<File> = emptyList(),
    val isRestoreConfirmationOpen: Boolean = false,
    val pendingRestoreSummary: BackupSummary? = null,
    val currentUser: CloudUserAccount? = null,
    val cloudBackupStatus: CloudBackupStatus = CloudBackupStatus.IDLE,
    val lastCloudBackupTime: Long? = null,
    val isAutoBackupEnabled: Boolean = true,
    val isCloudLoading: Boolean = false,
    val availableCloudBackups: List<CloudBackupMetadata> = emptyList(),
    val isCloudRestoreConfirmationOpen: Boolean = false,
    val pendingCloudRestoreMetadata: CloudBackupMetadata? = null,
    val showGoogleSignInDialog: Boolean = false,
    val showCloudAuthSheet: Boolean = false,
    val showChangePasswordDialog: Boolean = false,
    val showCloudCredentialsDialog: Boolean = false,
    val showCloudBackupFoundPrompt: Boolean = false,
    val detectedCloudBackup: CloudBackupMetadata? = null,
    val isPrivacyPolicyOpen: Boolean = false,
    val isDeleteAccountDialogOpen: Boolean = false,
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
    private val transactionRepository: TransactionRepository,
    private val backupManager: BackupManager,
    private val cloudBackupManager: CloudBackupManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val currentMonthStr = YearMonth.now().toString()

    init {
        // مراقبة حساب السحابة وحالات النسخ
        if (cloudBackupManager != null) {
            viewModelScope.launch {
                cloudBackupManager.provider.currentUser.collect { user ->
                    _uiState.update { it.copy(currentUser = user) }
                    if (user != null) {
                        refreshAvailableCloudBackups()
                    }
                }
            }
            viewModelScope.launch {
                cloudBackupManager.provider.backupStatus.collect { status ->
                    _uiState.update { it.copy(cloudBackupStatus = status) }
                }
            }
            viewModelScope.launch {
                cloudBackupManager.provider.lastBackupTimestamp.collect { ts ->
                    _uiState.update { it.copy(lastCloudBackupTime = ts) }
                }
            }
            viewModelScope.launch {
                cloudBackupManager.provider.isAutoBackupEnabled.collect { enabled ->
                    _uiState.update { it.copy(isAutoBackupEnabled = enabled) }
                }
            }
            // فحص التعديلات المعلقة
            viewModelScope.launch {
                cloudBackupManager.checkPendingChanges()
            }
        }
        // مراقبة وضع المظهر من DataStore
        viewModelScope.launch {
            preferencesManager.themeMode.distinctUntilChanged().collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }

        // مراقبة ميزانية الشهر الحالي
        viewModelScope.launch {
            monthlyBudgetRepository.getBudgetForMonth(currentMonthStr).distinctUntilChanged().collect { currentBudget ->
                _uiState.update { it.copy(currentMonthBudget = currentBudget?.amount) }
            }
        }

        // مراقبة سجل الميزانيات لجميع الأشهر
        viewModelScope.launch {
            monthlyBudgetRepository.getAllBudgets().distinctUntilChanged().collect { budgets ->
                _uiState.update { it.copy(allBudgets = budgets) }
            }
        }

        // مراقبة جميع التصنيفات وتحديث عدد العمليات المرتبطة بكل تصنيف
        viewModelScope.launch {
            categoryRepository.getAllCategories().distinctUntilChanged().collect { categories ->
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

        // مراقبة كود العملة الرئيسية المختارة من DataStore
        viewModelScope.launch {
            preferencesManager.selectedCurrencyCode.distinctUntilChanged().collect { code ->
                _uiState.update { it.copy(selectedCurrencyCode = code) }
            }
        }

        // فحص وجود عمليات مالية سابقة للتحذير عند تغيير العملة
        viewModelScope.launch {
            transactionRepository.getAllTransactions().distinctUntilChanged().collect { txs ->
                _uiState.update { it.copy(hasExistingTransactions = txs.isNotEmpty()) }
            }
        }
    }

    // ==========================================
    // 0. إدارة العملة الرئيسية (Currency Management)
    // ==========================================

    fun openCurrencySelection() {
        _uiState.update { it.copy(isCurrencySelectionOpen = true) }
    }

    fun dismissCurrencySelection() {
        _uiState.update { it.copy(isCurrencySelectionOpen = false) }
    }

    fun selectCurrency(code: String) {
        val current = _uiState.value.selectedCurrencyCode
        if (code.equals(current, ignoreCase = true)) {
            _uiState.update { it.copy(isCurrencySelectionOpen = false) }
            return
        }

        if (_uiState.value.hasExistingTransactions) {
            _uiState.update {
                it.copy(
                    isCurrencySelectionOpen = false,
                    showCurrencyChangeWarningDialog = true,
                    pendingCurrencyCode = code
                )
            }
        } else {
            _uiState.update { it.copy(isCurrencySelectionOpen = false) }
            viewModelScope.launch {
                preferencesManager.setSelectedCurrencyCode(code)
                preferencesManager.setHasSelectedCurrency(true)
            }
        }
    }

    fun confirmCurrencyChange() {
        val newCode = _uiState.value.pendingCurrencyCode ?: return
        _uiState.update {
            it.copy(
                showCurrencyChangeWarningDialog = false,
                pendingCurrencyCode = null
            )
        }
        viewModelScope.launch {
            preferencesManager.setSelectedCurrencyCode(newCode)
            preferencesManager.setHasSelectedCurrency(true)
        }
    }

    fun dismissCurrencyChangeWarning() {
        _uiState.update {
            it.copy(
                showCurrencyChangeWarningDialog = false,
                pendingCurrencyCode = null
            )
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

    // ==========================================
    // 5. النسخ الاحتياطي والاستعادة وتصدير CSV
    // ==========================================

    fun refreshLocalBackups(context: Context) {
        val files = BackupFileManager.listLocalBackups(context)
        _uiState.update { it.copy(localBackupFiles = files) }
    }

    fun openRestoreSelection(context: Context) {
        val files = BackupFileManager.listLocalBackups(context)
        _uiState.update {
            it.copy(
                isRestoreSelectionOpen = true,
                localBackupFiles = files
            )
        }
    }

    fun closeRestoreSelection() {
        _uiState.update { it.copy(isRestoreSelectionOpen = false) }
    }

    /**
     * إنشاء وحفظ نسخة احتياطية مباشرة في المجلد المحلي للتطبيق
     */
    fun createBackupLocally(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = backupManager.createBackupJson()
                val targetFile = BackupFileManager.saveBackupDirectly(context, jsonString)
                val updatedFiles = BackupFileManager.listLocalBackups(context)
                _uiState.update {
                    it.copy(
                        localBackupFiles = updatedFiles,
                        successMessage = "تم إنشاء النسخة الاحتياطية بنجاح:\n${targetFile.name}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "فشل إنشاء النسخة الاحتياطية: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    /**
     * حفظ النسخة الاحتياطية في المسار الخارجي المختار عبر SAF مع الاحتفاظ بنسخة محلية
     */
    fun exportBackupToUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = backupManager.createBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(jsonString) }
                } ?: throw IllegalStateException("تعذر فتح مسار الملف المختار")

                BackupFileManager.saveBackupDirectly(context, jsonString)
                val updatedFiles = BackupFileManager.listLocalBackups(context)

                _uiState.update {
                    it.copy(
                        localBackupFiles = updatedFiles,
                        successMessage = "تم حفظ النسخة الاحتياطية بنجاح في الموقع المحدد"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "فشل حفظ النسخة الاحتياطية: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    /**
     * استعادة نسخة من ملف محلي موجود مسبقاً
     */
    fun restoreFromLocalFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = file.readText(Charsets.UTF_8)
                backupManager.validateBackupJson(jsonString)
                    .onSuccess { summary ->
                        _uiState.update {
                            it.copy(
                                isRestoreSelectionOpen = false,
                                pendingRestoreSummary = summary,
                                isRestoreConfirmationOpen = true
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(errorMessage = error.localizedMessage ?: "ملف النسخة الاحتياطية غير صالح")
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "تعذر قراءة الملف: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    /**
     * قراءة وفحص ملف مسترجع عبر SAF
     */
    fun prepareRestoreFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } ?: throw IllegalStateException("تعذر قراءة الملف المختار")

                backupManager.validateBackupJson(jsonString)
                    .onSuccess { summary ->
                        _uiState.update {
                            it.copy(
                                isRestoreSelectionOpen = false,
                                pendingRestoreSummary = summary,
                                isRestoreConfirmationOpen = true
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(errorMessage = error.localizedMessage ?: "ملف النسخة الاحتياطية غير صالح")
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "تعذر قراءة الملف المختار: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    /**
     * تصدير العمليات إلى ملف CSV محلي وخارجي
     */
    fun exportCsvLocallyAndToUri(context: Context, uri: Uri? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                val csvContent = CsvExporter.exportTransactions(transactions)

                val savedFile = BackupFileManager.saveCsvDirectly(context, csvContent)

                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.bufferedWriter(Charsets.UTF_8).use { it.write(csvContent) }
                    }
                }

                _uiState.update {
                    it.copy(successMessage = "تم تصدير ${transactions.size} عملية إلى ملف CSV بنجاح:\n${savedFile.name}")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "فشل تصدير ملف CSV: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    // دوال الدعم للاختبارات المعزولة (Streams)
    fun exportBackup(outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val jsonString = backupManager.createBackupJson()
                outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(jsonString) }
                _uiState.update { it.copy(successMessage = "تم إنشاء النسخة الاحتياطية بنجاح") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "فشل إنشاء النسخة الاحتياطية: ${e.message}") }
            }
        }
    }

    fun prepareRestore(inputStream: InputStream) {
        viewModelScope.launch {
            try {
                val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                backupManager.validateBackupJson(jsonString)
                    .onSuccess { summary ->
                        _uiState.update {
                            it.copy(
                                pendingRestoreSummary = summary,
                                isRestoreConfirmationOpen = true
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(errorMessage = error.message ?: "ملف النسخة الاحتياطية غير صالح")
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "تعذر قراءة الملف المختار: ${e.message}") }
            }
        }
    }

    fun confirmRestore() {
        val summary = _uiState.value.pendingRestoreSummary ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoreConfirmationOpen = false) }
            backupManager.restoreBackup(summary.payload)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            pendingRestoreSummary = null,
                            successMessage = "تمت استعادة البيانات بنجاح (${summary.transactionCount} عملية، ${summary.categoryCount} تصنيفاً)"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            pendingRestoreSummary = null,
                            errorMessage = "فشلت استعادة البيانات: ${error.message}"
                        )
                    }
                }
        }
    }

    fun cancelRestore() {
        _uiState.update {
            it.copy(
                isRestoreConfirmationOpen = false,
                pendingRestoreSummary = null
            )
        }
    }

    fun exportCsv(outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                val csvContent = CsvExporter.exportTransactions(transactions)
                outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(csvContent) }
                _uiState.update {
                    it.copy(successMessage = "تم تصدير ${transactions.size} عملية إلى ملف CSV بنجاح")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "فشل تصدير ملف CSV: ${e.message}") }
            }
        }
    }

    // ==========================================
    // إدارة الحساب والنسخ الاحتياطي السحابي (Cloud Backup)
    // ==========================================

    fun openCloudAuthSheet() {
        _uiState.update { it.copy(showCloudAuthSheet = true, showGoogleSignInDialog = true) }
    }

    fun dismissCloudAuthSheet() {
        _uiState.update { it.copy(showCloudAuthSheet = false, showGoogleSignInDialog = false) }
    }

    fun openGoogleSignInDialog() {
        openCloudAuthSheet()
    }

    fun dismissGoogleSignInDialog() {
        dismissCloudAuthSheet()
    }

    fun signUpWithEmail(name: String, email: String, pass: String) {
        val manager = cloudBackupManager ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudLoading = true) }
            val res = manager.provider.signUpWithEmail(email, pass, name)
            _uiState.update { it.copy(isCloudLoading = false) }
            if (res.isSuccess) {
                val account = res.getOrThrow()
                _uiState.update {
                    it.copy(
                        showCloudAuthSheet = false,
                        showGoogleSignInDialog = false,
                        currentUser = account,
                        successMessage = "تم إنشاء الحساب بنجاح وربط المحفظة بالسحابة (${account.displayName})"
                    )
                }
                refreshAvailableCloudBackups()
            } else {
                _uiState.update { it.copy(errorMessage = res.exceptionOrNull()?.message ?: "فشل إنشاء الحساب السحابي") }
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        val manager = cloudBackupManager ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudLoading = true) }
            val res = manager.provider.signInWithEmail(email, pass)
            _uiState.update { it.copy(isCloudLoading = false) }
            if (res.isSuccess) {
                val account = res.getOrThrow()
                _uiState.update {
                    it.copy(
                        showCloudAuthSheet = false,
                        showGoogleSignInDialog = false,
                        currentUser = account,
                        successMessage = "تم تسجيل الدخول بنجاح بحساب ${account.email}"
                    )
                }
                // جلب النسخ المتوفرة والتحقق من وجود نسخة للاستعادة التلقائية
                val backupsRes = manager.provider.getAvailableBackups()
                val backups = backupsRes.getOrDefault(emptyList())
                _uiState.update { it.copy(availableCloudBackups = backups) }

                // فحص إذا كانت قاعدة البيانات خالية
                val localTxCount = transactionRepository.getAllTransactions().first().size
                if (backups.isNotEmpty() && localTxCount == 0) {
                    val latestBackup = backups.first()
                    _uiState.update {
                        it.copy(
                            showCloudBackupFoundPrompt = true,
                            detectedCloudBackup = latestBackup
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(errorMessage = res.exceptionOrNull()?.message ?: "فشل تسجيل الدخول بالسحابة") }
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        val manager = cloudBackupManager ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudLoading = true) }
            val res = manager.provider.sendPasswordResetEmail(email)
            _uiState.update { it.copy(isCloudLoading = false) }
            if (res.isSuccess) {
                _uiState.update { it.copy(successMessage = "تم إرسال رابط استعادة كلمة المرور إلى $email بنجاح") }
            } else {
                _uiState.update { it.copy(errorMessage = res.exceptionOrNull()?.message ?: "فشل إرسال رابط الاستعادة") }
            }
        }
    }

    fun openChangePasswordDialog() {
        _uiState.update { it.copy(showChangePasswordDialog = true) }
    }

    fun dismissChangePasswordDialog() {
        _uiState.update { it.copy(showChangePasswordDialog = false) }
    }

    fun changePassword(newPassword: String) {
        val manager = cloudBackupManager ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudLoading = true) }
            val res = manager.provider.changePassword(newPassword)
            _uiState.update { it.copy(isCloudLoading = false) }
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        showChangePasswordDialog = false,
                        successMessage = "تم تغيير كلمة المرور بنجاح"
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = res.exceptionOrNull()?.message ?: "فشل تغيير كلمة المرور") }
            }
        }
    }

    fun openCloudCredentialsDialog() {
        _uiState.update { it.copy(showCloudCredentialsDialog = true) }
    }

    fun dismissCloudCredentialsDialog() {
        _uiState.update { it.copy(showCloudCredentialsDialog = false) }
    }

    fun saveCloudCredentials(apiKey: String, projectId: String) {
        viewModelScope.launch {
            preferencesManager.setCloudCredentials(apiKey, projectId)
            _uiState.update {
                it.copy(
                    showCloudCredentialsDialog = false,
                    successMessage = "تم حفظ إعدادات السحابة بنجاح"
                )
            }
        }
    }

    fun dismissCloudBackupFoundPrompt() {
        _uiState.update {
            it.copy(
                showCloudBackupFoundPrompt = false,
                detectedCloudBackup = null
            )
        }
    }

    fun confirmAutoDetectedRestore() {
        val detected = _uiState.value.detectedCloudBackup ?: return
        _uiState.update {
            it.copy(
                showCloudBackupFoundPrompt = false,
                detectedCloudBackup = null,
                pendingCloudRestoreMetadata = detected
            )
        }
        confirmCloudRestore()
    }

    fun signInWithGoogle(account: CloudUserAccount) {
        val manager = cloudBackupManager ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudLoading = true, showGoogleSignInDialog = false, showCloudAuthSheet = false) }
            val res = manager.provider.signInWithGoogle(account)
            _uiState.update { it.copy(isCloudLoading = false) }
            if (res.isSuccess) {
                _uiState.update { it.copy(successMessage = "تم تسجيل الدخول بنجاح بحساب ${account.email}") }
                refreshAvailableCloudBackups()
            } else {
                _uiState.update { it.copy(errorMessage = "تعذر تسجيل الدخول بالسحابة: ${res.exceptionOrNull()?.message}") }
            }
        }
    }

    fun signOutFromGoogle() {
        val manager = cloudBackupManager ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudLoading = true) }
            val res = manager.provider.signOut()
            _uiState.update { it.copy(isCloudLoading = false, availableCloudBackups = emptyList()) }
            if (res.isSuccess) {
                _uiState.update { it.copy(successMessage = "تم تسجيل الخروج من السحابة بنجاح") }
            }
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        val manager = cloudBackupManager ?: return
        viewModelScope.launch {
            manager.provider.setAutoBackupEnabled(enabled)
        }
    }

    fun backupToCloudNow() {
        val manager = cloudBackupManager ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudLoading = true) }
            val res = manager.createAndUploadCloudBackup()
            _uiState.update { it.copy(isCloudLoading = false) }
            if (res.isSuccess) {
                _uiState.update { it.copy(successMessage = "تم إنشاء النسخة الاحتياطية السحابية وحفظها بنجاح") }
                refreshAvailableCloudBackups()
            } else {
                _uiState.update { it.copy(errorMessage = "فشل النسخ السحابي: ${res.exceptionOrNull()?.message}") }
            }
        }
    }

    fun refreshAvailableCloudBackups() {
        val manager = cloudBackupManager ?: return
        viewModelScope.launch {
            val res = manager.provider.getAvailableBackups()
            if (res.isSuccess) {
                _uiState.update { it.copy(availableCloudBackups = res.getOrDefault(emptyList())) }
            }
        }
    }

    fun requestCloudRestore(backup: CloudBackupMetadata? = null) {
        val target = backup ?: _uiState.value.availableCloudBackups.firstOrNull()
        if (target != null) {
            _uiState.update {
                it.copy(
                    isCloudRestoreConfirmationOpen = true,
                    pendingCloudRestoreMetadata = target
                )
            }
        } else {
            _uiState.update { it.copy(errorMessage = "لا توجد نسخ سحابية متوفرة لهذا الحساب") }
        }
    }

    fun dismissCloudRestoreConfirmation() {
        _uiState.update {
            it.copy(
                isCloudRestoreConfirmationOpen = false,
                pendingCloudRestoreMetadata = null
            )
        }
    }

    fun confirmCloudRestore() {
        val manager = cloudBackupManager ?: return
        val metadata = _uiState.value.pendingCloudRestoreMetadata ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudLoading = true, isCloudRestoreConfirmationOpen = false) }
            val res = manager.restoreFromCloud(metadata)
            _uiState.update { it.copy(isCloudLoading = false, pendingCloudRestoreMetadata = null) }
            if (res.isSuccess) {
                _uiState.update { it.copy(successMessage = "تمت استعادة كافة بياناتك من السحابة بنجاح تام") }
            } else {
                _uiState.update { it.copy(errorMessage = "فشلت استعادة النسخة السحابية: ${res.exceptionOrNull()?.message}") }
            }
        }
    }

    // سياسة الخصوصية
    fun openPrivacyPolicy() {
        _uiState.update { it.copy(isPrivacyPolicyOpen = true) }
    }

    fun closePrivacyPolicy() {
        _uiState.update { it.copy(isPrivacyPolicyOpen = false) }
    }

    // حذف الحساب والبيانات السحابية
    fun openDeleteAccountDialog() {
        _uiState.update { it.copy(isDeleteAccountDialogOpen = true) }
    }

    fun closeDeleteAccountDialog() {
        _uiState.update { it.copy(isDeleteAccountDialogOpen = false) }
    }

    fun deleteAccountAndCloudData(wipeLocalData: Boolean) {
        val manager = cloudBackupManager ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudLoading = true, isDeleteAccountDialogOpen = false) }
            val res = manager.deleteAccountAndCloudData(wipeLocalData)
            _uiState.update { it.copy(isCloudLoading = false) }
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        currentUser = null,
                        availableCloudBackups = emptyList(),
                        successMessage = if (wipeLocalData)
                            "تم حذف الحساب والبيانات السحابية والمحلية بنجاح"
                        else
                            "تم حذف الحساب السحابي وكافة النسخ الاحتياطية بنجاح، مع بقاء بياناتك المحلية في الهاتف"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "تعذر حذف الحساب والبيانات السحابية: ${res.exceptionOrNull()?.message}")
                }
            }
        }
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
    private val transactionRepository: TransactionRepository,
    private val backupManager: BackupManager,
    private val cloudBackupManager: CloudBackupManager? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(
                preferencesManager = preferencesManager,
                categoryRepository = categoryRepository,
                monthlyBudgetRepository = monthlyBudgetRepository,
                transactionRepository = transactionRepository,
                backupManager = backupManager,
                cloudBackupManager = cloudBackupManager
            ) as T
        }
        throw IllegalArgumentException("فئة نموذج العرض غير معروفة: ${modelClass.name}")
    }
}
