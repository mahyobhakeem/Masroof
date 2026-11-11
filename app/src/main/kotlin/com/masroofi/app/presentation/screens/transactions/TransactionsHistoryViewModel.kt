package com.masroofi.app.presentation.screens.transactions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masroofi.app.data.repository.CategoryRepositoryImpl
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.CategoryRepository
import com.masroofi.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * الفترات الزمنية لتصفية سجل العمليات
 */
enum class DateRangeFilter(val title: String) {
    ALL("الكل"),
    THIS_MONTH("هذا الشهر"),
    THIS_WEEK("هذا الأسبوع"),
    LAST_MONTH("الشهر السابق")
}

/**
 * معايير تصفية قائمة العمليات المالية
 */
data class FilterCriteria(
    val query: String = "",
    val filterIndex: Int = 0, // 0: الكل, 1: مصروفات, 2: إيرادات
    val categoryId: String? = null, // null يعني "جميع التصنيفات"
    val datePeriod: DateRangeFilter = DateRangeFilter.ALL
)

/**
 * حالة واجهة مستخدم سجل العمليات
 */
@Immutable
data class TransactionsHistoryUiState(
    val transactions: List<TransactionItem> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val availableFilterCategories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val selectedFilterIndex: Int = 0,
    val selectedCategoryId: String? = null,
    val selectedDatePeriod: DateRangeFilter = DateRangeFilter.ALL,
    val totalExpenses: Long = 0L,
    val totalIncome: Long = 0L,
    val netTotal: Long = 0L,
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

/**
 * نموذج العرض لشاشة سجل العمليات (TransactionsHistoryViewModel)
 */
class TransactionsHistoryViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _filters = MutableStateFlow(FilterCriteria())
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(TransactionsHistoryUiState(isLoading = true))
    val uiState: StateFlow<TransactionsHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactions().distinctUntilChanged(),
                categoryRepository.getAllCategories().distinctUntilChanged(),
                _filters,
                _userMessage
            ) { allTx: List<TransactionItem>, allCats: List<Category>, filters: FilterCriteria, msg: String? ->
                // فرز التصنيفات منطقياً
                val sortedCats = CategoryRepositoryImpl.sortCategoriesLogically(allCats)

                // تحديد التصنيفات المتاحة في شريط الفلاتر بحسب نوع العملية المختار
                val availableCats = when (filters.filterIndex) {
                    1 -> sortedCats.filter { it.type == TransactionType.EXPENSE }
                    2 -> sortedCats.filter { it.type == TransactionType.INCOME }
                    else -> sortedCats
                }

                // حساب نطاق التاريخ بحسب الفلتر
                val now = LocalDate.now()
                val currentYearMonth = YearMonth.now()
                val lastYearMonth = currentYearMonth.minusMonths(1)
                val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))

                // تصفية العمليات
                val filtered = allTx.filter { tx ->
                    // 1. التصفية النصية والرقمية الشاملة
                    val matchesSearch = filters.query.isBlank() ||
                            (tx.note?.contains(filters.query, ignoreCase = true) == true) ||
                            (tx.categoryName?.contains(filters.query, ignoreCase = true) == true) ||
                            tx.amount.toString().contains(filters.query.trim())

                    // 2. التصفية بحسب نوع المعاملة
                    val matchesType = when (filters.filterIndex) {
                        1 -> tx.type == TransactionType.EXPENSE
                        2 -> tx.type == TransactionType.INCOME
                        else -> true
                    }

                    // 3. التصفية بحسب معرف التصنيف الفريد (وليس الاسم أبداً)
                    val matchesCategory = filters.categoryId == null || tx.categoryId == filters.categoryId

                    // 4. التصفية بحسب الفترة الزمنية
                    val matchesDate = when (filters.datePeriod) {
                        DateRangeFilter.ALL -> true
                        DateRangeFilter.THIS_MONTH -> tx.transactionDate.startsWith(currentYearMonth.toString())
                        DateRangeFilter.LAST_MONTH -> tx.transactionDate.startsWith(lastYearMonth.toString())
                        DateRangeFilter.THIS_WEEK -> {
                            try {
                                val txDate = LocalDate.parse(tx.transactionDate)
                                !txDate.isBefore(startOfWeek) && !txDate.isAfter(now)
                            } catch (_: Exception) {
                                true
                            }
                        }
                    }

                    matchesSearch && matchesType && matchesCategory && matchesDate
                }

                // حساب إجماليات النتائج المصفاة
                val totalExp = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val totalInc = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val net = totalInc - totalExp

                TransactionsHistoryUiState(
                    transactions = filtered,
                    allCategories = sortedCats,
                    availableFilterCategories = availableCats,
                    searchQuery = filters.query,
                    selectedFilterIndex = filters.filterIndex,
                    selectedCategoryId = filters.categoryId,
                    selectedDatePeriod = filters.datePeriod,
                    totalExpenses = totalExp,
                    totalIncome = totalInc,
                    netTotal = net,
                    isLoading = false,
                    userMessage = msg
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setSearchQuery(query: String) {
        _filters.update { it.copy(query = query) }
    }

    fun setFilterIndex(index: Int) {
        _filters.update { current ->
            // إذا تغير النوع، نتأكد أن التصنيف المختار ينتمي للنوع الجديد وإلا نعيد تعيينه
            val newType = when (index) {
                1 -> TransactionType.EXPENSE
                2 -> TransactionType.INCOME
                else -> null
            }
            val currentCatId = current.categoryId
            val validCategory = if (currentCatId != null && newType != null) {
                val cat = uiState.value.allCategories.find { it.id == currentCatId }
                if (cat?.type == newType) currentCatId else null
            } else currentCatId

            current.copy(filterIndex = index, categoryId = validCategory)
        }
    }

    fun setSelectedCategoryId(categoryId: String?) {
        _filters.update { it.copy(categoryId = categoryId) }
    }

    fun setDatePeriod(period: DateRangeFilter) {
        _filters.update { it.copy(datePeriod = period) }
    }

    fun resetFilters() {
        _filters.value = FilterCriteria()
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            val result = transactionRepository.deleteTransaction(id)
            if (result.isSuccess) {
                _userMessage.value = "تم حذف العملية بنجاح"
            } else {
                _userMessage.value = "تعذر حذف العملية"
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}

/**
 * مصنع إنشاء نموذج العرض لسجل العمليات
 */
class TransactionsHistoryViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionsHistoryViewModel::class.java)) {
            return TransactionsHistoryViewModel(transactionRepository, categoryRepository) as T
        }
        throw IllegalArgumentException("فئة نموذج العرض غير معروفة: ${modelClass.name}")
    }
}
