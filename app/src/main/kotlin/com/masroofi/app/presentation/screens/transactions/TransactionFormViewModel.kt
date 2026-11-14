package com.masroofi.app.presentation.screens.transactions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masroofi.app.domain.calculations.FinancialCalculator
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.CategoryRepository
import com.masroofi.app.domain.repository.TransactionRepository
import com.masroofi.app.utils.FormatUtils
import com.masroofi.app.utils.formatNumber
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/**
 * حالة واجهة مستخدم نموذج إضافة وتعديل العملية المالية
 */
@Immutable
data class TransactionFormUiState(
    val transactionId: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val selectedCategory: Category? = null,
    val availableCategories: List<Category> = emptyList(),
    val transactionDate: LocalDate = LocalDate.now(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val currentAvailableBalance: Long = 0L,
    val amountError: String? = null,
    val dateError: String? = null,
    val categoryError: String? = null,
    val userMessage: String? = null,
    val isSavedSuccess: Boolean = false,
    val isDeletedSuccess: Boolean = false
)

/**
 * نموذج العرض (ViewModel) لإدارة عمليات إضافة وتعديل وحذف العمليات المالية
 * مع التحقق المالي الصارم وحماية الرصيد الصافي المتاح (Balance Validation Guard)
 */
class TransactionFormViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionFormUiState())
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    private var allTransactions: List<TransactionItem> = emptyList()

    init {
        viewModelScope.launch {
            transactionRepository.getAllTransactions().distinctUntilChanged().collect { txs ->
                allTransactions = txs
                val balance = FinancialCalculator.calculateAvailableBalance(txs)
                _uiState.update { current ->
                    current.copy(currentAvailableBalance = balance)
                }
            }
        }
        loadCategoriesForType(TransactionType.EXPENSE)
    }

    /**
     * تحميل التصنيفات النشطة للنوع المحدد (مصروف أو إيراد)
     */
    private fun loadCategoriesForType(type: TransactionType, targetCategoryId: String? = null) {
        viewModelScope.launch {
            categoryRepository.getActiveCategories(type).distinctUntilChanged().collect { categories ->
                val chosenCategory = if (targetCategoryId != null) {
                    categories.find { it.id == targetCategoryId } ?: categories.firstOrNull()
                } else {
                    categories.firstOrNull()
                }
                _uiState.update { currentState ->
                    currentState.copy(
                        availableCategories = categories,
                        selectedCategory = chosenCategory,
                        categoryError = null
                    )
                }
            }
        }
    }

    /**
     * تهيئة النموذج للإضافة الجديدة مع فحص الرصيد المتاح:
     * إذا كان الرصيد صفراً يتم الفتح تلقائياً على تبويب "إيراد" مع تنبيه المستخدم
     */
    fun initForAdd(defaultType: TransactionType = TransactionType.EXPENSE) {
        val balance = FinancialCalculator.calculateAvailableBalance(allTransactions)
        val initialType = if (balance <= 0L) TransactionType.INCOME else defaultType
        val message = if (balance <= 0L) {
            "رصيدك الحالي 0 — أضف إيراداً أو رصيداً أولاً لتتمكن من تسجيل المصروفات"
        } else null

        _uiState.update {
            TransactionFormUiState(
                type = initialType,
                transactionDate = LocalDate.now(),
                currentAvailableBalance = balance,
                userMessage = message
            )
        }
        loadCategoriesForType(initialType)
    }

    /**
     * تهيئة النموذج لتعديل عملية موجودة
     */
    fun initForEdit(transaction: TransactionItem) {
        val parsedDate = runCatching { LocalDate.parse(transaction.transactionDate) }.getOrDefault(LocalDate.now())
        val balance = FinancialCalculator.calculateAvailableBalance(allTransactions)
        _uiState.update {
            it.copy(
                transactionId = transaction.id,
                type = transaction.type,
                amount = transaction.amount.toString(),
                transactionDate = parsedDate,
                note = transaction.note.orEmpty(),
                createdAt = transaction.createdAt,
                isEditing = true,
                currentAvailableBalance = balance,
                amountError = null,
                dateError = null,
                categoryError = null,
                userMessage = null,
                isSavedSuccess = false,
                isDeletedSuccess = false
            )
        }
        loadCategoriesForType(transaction.type, transaction.categoryId)
    }

    /**
     * تغيير نوع العملية (مصروف أو إيراد) مع حماية الرصيد الصفري
     */
    fun setType(type: TransactionType) {
        val currentBalance = _uiState.value.currentAvailableBalance
        val isEditing = _uiState.value.isEditing

        // إذا كان الرصيد صفراً وحاول المستخدم التبديل إلى "مصروف" عند الإضافة، نمنعه ونعرض رسالة خطأ صريحة
        if (type == TransactionType.EXPENSE && !isEditing && currentBalance <= 0L) {
            _uiState.update {
                it.copy(
                    type = TransactionType.INCOME,
                    amountError = "لا يمكن تسجيل مصروف لأن رصيدك المتاح هو 0. يرجى إضافة إيراد أولاً",
                    userMessage = "لا يمكن تسجيل مصروف لأن رصيدك المتاح هو 0. يرجى إضافة إيراد أولاً"
                )
            }
            loadCategoriesForType(TransactionType.INCOME)
            return
        }

        if (_uiState.value.type != type) {
            _uiState.update { it.copy(type = type, selectedCategory = null, amountError = null) }
            loadCategoriesForType(type)
        }
    }

    /**
     * إدخال وتعديل المبلغ المالي (يقبل أرقام فقط)
     */
    fun setAmount(input: String) {
        val filtered = input.filter { it.isDigit() }
        _uiState.update { it.copy(amount = filtered, amountError = null) }
    }

    /**
     * اختيار التصنيف المالي
     */
    fun setCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category, categoryError = null) }
    }

    /**
     * اختيار تاريخ العملية والتحقق من عدم كونه في المستقبل
     */
    fun setDate(date: LocalDate) {
        if (date.isAfter(LocalDate.now())) {
            _uiState.update {
                it.copy(
                    transactionDate = date,
                    dateError = "لا يمكن تسجيل عملية بتاريخ مستقبلي"
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    transactionDate = date,
                    dateError = null
                )
            }
        }
    }

    /**
     * إدخال الملاحظة
     */
    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    /**
     * حفظ العملية المالية (إضافة جديدة أو تعديل) مع تطبيق قواعد التحقق الصارمة وحماية الرصيد
     */
    fun saveTransaction() {
        val currentState = _uiState.value
        if (currentState.isSaving) return

        // 1. التحقق من المبلغ
        val parsedAmount = currentState.amount.toLongOrNull()
        if (parsedAmount == null || parsedAmount <= 0) {
            _uiState.update { it.copy(amountError = "يرجى إدخال مبلغ صحيح أكبر من الصفر") }
            return
        }

        // 2. التحقق من التصنيف
        val category = currentState.selectedCategory
        if (category == null) {
            _uiState.update { it.copy(categoryError = "يرجى اختيار تصنيف للعملية") }
            return
        }

        // 3. التحقق من التاريخ
        if (currentState.transactionDate.isAfter(LocalDate.now())) {
            _uiState.update { it.copy(dateError = "لا يمكن تسجيل عملية بتاريخ مستقبلي") }
            return
        }

        // 4. الحماية الصارمة للمنطق المالي والرصيد المتاح (Balance Validation Guard)
        if (currentState.type == TransactionType.EXPENSE) {
            if (!currentState.isEditing) {
                // حالة الإضافة الجديدة لمصروف:
                if (currentState.currentAvailableBalance <= 0L) {
                    _uiState.update {
                        it.copy(
                            amountError = "لا يمكن تسجيل مصروف لأن رصيدك المتاح هو 0. يرجى إضافة إيراد أولاً",
                            userMessage = "لا يمكن تسجيل مصروف لأن رصيدك المتاح هو 0. يرجى إضافة إيراد أولاً"
                        )
                    }
                    return
                }
                if (parsedAmount > currentState.currentAvailableBalance) {
                    _uiState.update {
                        it.copy(
                            amountError = "المبلغ المدخل يتجاوز رصيدك الصافي المتاح حالياً (${formatNumber(currentState.currentAvailableBalance)})"
                        )
                    }
                    return
                }
            } else {
                // حالة التعديل لمصروف قائم:
                val origTx = allTransactions.find { it.id == currentState.transactionId }
                val maxAllowed = if (origTx != null && origTx.type == TransactionType.EXPENSE) {
                    currentState.currentAvailableBalance + origTx.amount
                } else if (origTx != null && origTx.type == TransactionType.INCOME) {
                    currentState.currentAvailableBalance - origTx.amount
                } else {
                    currentState.currentAvailableBalance
                }

                if (maxAllowed <= 0L) {
                    _uiState.update {
                        it.copy(amountError = "لا يمكن تسجيل مصروف لأن رصيدك المتاح بعد التعديل هو 0 أو أقل")
                    }
                    return
                }
                if (parsedAmount > maxAllowed) {
                    _uiState.update {
                        it.copy(
                            amountError = "المبلغ المدخل يتجاوز رصيدك الصافي المتاح حالياً (${formatNumber(maxAllowed)})"
                        )
                    }
                    return
                }
            }
        } else {
            // في حالة تعديل إيراد موجود: التحقق من عدم جعل الصافي سالباً
            if (currentState.isEditing) {
                val origTx = allTransactions.find { it.id == currentState.transactionId }
                if (origTx != null && origTx.type == TransactionType.INCOME) {
                    val reduction = origTx.amount - parsedAmount
                    if (reduction > 0 && (currentState.currentAvailableBalance - reduction) < 0L) {
                        _uiState.update {
                            it.copy(
                                amountError = "لا يمكن تقليل هذا الإيراد لأن هناك مصروفات مسجلة اعتمدت عليه، مما سيجعل الصافي بالسالب"
                            )
                        }
                        return
                    }
                }
            }
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val nowTimestamp = System.currentTimeMillis()
            val transactionItem = if (currentState.isEditing && currentState.transactionId != null) {
                // تعديل: الحفاظ على id الأصلي و createdAt وتحديث updatedAt فقط
                TransactionItem(
                    id = currentState.transactionId,
                    type = currentState.type,
                    amount = parsedAmount,
                    categoryId = category.id,
                    categoryName = category.name,
                    categoryIconKey = category.iconKey,
                    transactionDate = currentState.transactionDate.toString(),
                    note = currentState.note.trim().ifEmpty { null },
                    createdAt = currentState.createdAt,
                    updatedAt = nowTimestamp
                )
            } else {
                // إضافة جديدة: إنشاء معرف UUID فريد
                TransactionItem(
                    id = UUID.randomUUID().toString(),
                    type = currentState.type,
                    amount = parsedAmount,
                    categoryId = category.id,
                    categoryName = category.name,
                    categoryIconKey = category.iconKey,
                    transactionDate = currentState.transactionDate.toString(),
                    note = currentState.note.trim().ifEmpty { null },
                    createdAt = nowTimestamp,
                    updatedAt = nowTimestamp
                )
            }

            val result = if (currentState.isEditing) {
                transactionRepository.updateTransaction(transactionItem)
            } else {
                transactionRepository.addTransaction(transactionItem)
            }

            if (result.isSuccess) {
                val successMessage = if (currentState.isEditing) {
                    "تم تعديل العملية بنجاح"
                } else {
                    "تم حفظ العملية بنجاح"
                }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isSavedSuccess = true,
                        userMessage = successMessage
                    )
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "حدث خطأ أثناء حفظ العملية"
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        userMessage = errorMsg
                    )
                }
            }
        }
    }

    /**
     * حذف العملية المالية مع منع حذف الإيرادات التي تعتمد عليها مصروفات
     */
    fun deleteTransaction(transactionId: String) {
        val tx = allTransactions.find { it.id == transactionId }
        if (tx != null && tx.type == TransactionType.INCOME) {
            val resultingBalance = _uiState.value.currentAvailableBalance - tx.amount
            if (resultingBalance < 0L) {
                _uiState.update {
                    it.copy(
                        userMessage = "لا يمكن حذف هذا الإيراد لأن هناك مصروفات مسجلة اعتمدت عليه، مما سيجعل الصافي بالسالب"
                    )
                }
                return
            }
        }

        viewModelScope.launch {
            val result = transactionRepository.deleteTransaction(transactionId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isDeletedSuccess = true,
                        userMessage = "تم حذف العملية بنجاح"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        userMessage = "تعذر حذف العملية، يرجى المحاولة لاحقاً"
                    )
                }
            }
        }
    }

    /**
     * مسح الرسائل بعد عرضها
     */
    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}

/**
 * مصنع تزويد نموذج العرض بالاعتماديات (ViewModelProvider.Factory)
 */
class TransactionFormViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionFormViewModel::class.java)) {
            return TransactionFormViewModel(transactionRepository, categoryRepository) as T
        }
        throw IllegalArgumentException("فئة نموذج العرض غير معروفة: ${modelClass.name}")
    }
}
