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
import com.masroofi.app.utils.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * الفترات الزمنية لتصفية سجل العمليات
 */
enum class DateRangeFilter(val title: String) {
    ALL("الكل"),
    TODAY("اليوم"),
    SPECIFIC_DAY("تحديد يوم معين"),
    THIS_WEEK("هذا الأسبوع"),
    THIS_MONTH("هذا الشهر"),
    LAST_MONTH("الشهر السابق"),
    CUSTOM_RANGE("فترة مخصصة (من - إلى)")
}

/**
 * معايير تصفية قائمة العمليات المالية
 */
data class FilterCriteria(
    val query: String = "",
    val filterIndex: Int = 0, // 0: الكل, 1: مصروفات, 2: إيرادات
    val categoryId: String? = null, // null يعني "جميع التصنيفات"
    val datePeriod: DateRangeFilter = DateRangeFilter.ALL,
    val specificDay: LocalDate = LocalDate.now(),
    val customStartDate: LocalDate = LocalDate.now().minusDays(30),
    val customEndDate: LocalDate = LocalDate.now()
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
    val specificDay: LocalDate = LocalDate.now(),
    val customStartDate: LocalDate = LocalDate.now().minusDays(30),
    val customEndDate: LocalDate = LocalDate.now(),
    val isDatePickerOpen: Boolean = false,
    val isDateRangePickerOpen: Boolean = false,
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
    private val _isDatePickerOpen = MutableStateFlow(false)
    private val _isDateRangePickerOpen = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(TransactionsHistoryUiState(isLoading = true))
    val uiState: StateFlow<TransactionsHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val dialogAndMsgFlow = combine(
                _isDatePickerOpen,
                _isDateRangePickerOpen,
                _userMessage
            ) { datePicker, rangePicker, msg ->
                Triple(datePicker, rangePicker, msg)
            }

            combine(
                transactionRepository.getAllTransactions().distinctUntilChanged(),
                categoryRepository.getAllCategories().distinctUntilChanged(),
                _filters,
                dialogAndMsgFlow
            ) { allTx: List<TransactionItem>, allCats: List<Category>, filters: FilterCriteria, dialogInfo: Triple<Boolean, Boolean, String?> ->
                val (datePickerOpen, rangePickerOpen, msg) = dialogInfo
                val sortedCats = CategoryRepositoryImpl.sortCategoriesLogically(allCats)

                val availableCats = when (filters.filterIndex) {
                    1 -> sortedCats.filter { it.type == TransactionType.EXPENSE }
                    2 -> sortedCats.filter { it.type == TransactionType.INCOME }
                    else -> sortedCats
                }

                val now = LocalDate.now()
                val currentYearMonth = YearMonth.now()
                val lastYearMonth = currentYearMonth.minusMonths(1)
                val (weekStart, weekEnd) = FormatUtils.getWeekRangeSaturday(now)

                val filtered = allTx.filter { tx ->
                    // 1. التصفية النصية والرقمية
                    val matchesSearch = filters.query.isBlank() ||
                            (tx.note?.contains(filters.query, ignoreCase = true) == true) ||
                            (tx.categoryName?.contains(filters.query, ignoreCase = true) == true) ||
                            tx.amount.toString().contains(filters.query.trim())

                    // 2. التصفية بنوع المعاملة
                    val matchesType = when (filters.filterIndex) {
                        1 -> tx.type == TransactionType.EXPENSE
                        2 -> tx.type == TransactionType.INCOME
                        else -> true
                    }

                    // 3. التصفية بمعرف التصنيف
                    val matchesCategory = filters.categoryId == null || tx.categoryId == filters.categoryId

                    // 4. التصفية الزمنية الصارمة
                    val matchesDate = when (filters.datePeriod) {
                        DateRangeFilter.ALL -> true
                        DateRangeFilter.TODAY -> tx.transactionDate == now.toString()
                        DateRangeFilter.SPECIFIC_DAY -> tx.transactionDate == filters.specificDay.toString()
                        DateRangeFilter.THIS_MONTH -> tx.transactionDate.startsWith(currentYearMonth.toString())
                        DateRangeFilter.LAST_MONTH -> tx.transactionDate.startsWith(lastYearMonth.toString())
                        DateRangeFilter.THIS_WEEK -> {
                            try {
                                val txDate = LocalDate.parse(tx.transactionDate)
                                !txDate.isBefore(weekStart) && !txDate.isAfter(weekEnd)
                            } catch (_: Exception) {
                                true
                            }
                        }
                        DateRangeFilter.CUSTOM_RANGE -> {
                            try {
                                val txDate = LocalDate.parse(tx.transactionDate)
                                !txDate.isBefore(filters.customStartDate) && !txDate.isAfter(filters.customEndDate)
                            } catch (_: Exception) {
                                true
                            }
                        }
                    }

                    matchesSearch && matchesType && matchesCategory && matchesDate
                }

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
                    specificDay = filters.specificDay,
                    customStartDate = filters.customStartDate,
                    customEndDate = filters.customEndDate,
                    isDatePickerOpen = datePickerOpen,
                    isDateRangePickerOpen = rangePickerOpen,
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
        when (period) {
            DateRangeFilter.SPECIFIC_DAY -> {
                _filters.update { it.copy(datePeriod = period) }
                openDatePicker()
            }
            DateRangeFilter.CUSTOM_RANGE -> {
                _filters.update { it.copy(datePeriod = period) }
                openDateRangePicker()
            }
            else -> {
                _filters.update { it.copy(datePeriod = period) }
            }
        }
    }

    fun setSpecificDay(day: LocalDate) {
        _filters.update { it.copy(specificDay = day, datePeriod = DateRangeFilter.SPECIFIC_DAY) }
        dismissDatePicker()
    }

    fun setCustomRange(startDate: LocalDate, endDate: LocalDate) {
        _filters.update {
            it.copy(
                customStartDate = startDate,
                customEndDate = endDate,
                datePeriod = DateRangeFilter.CUSTOM_RANGE
            )
        }
        dismissDateRangePicker()
    }

    fun openDatePicker() {
        _isDatePickerOpen.value = true
    }

    fun dismissDatePicker() {
        _isDatePickerOpen.value = false
    }

    fun openDateRangePicker() {
        _isDateRangePickerOpen.value = true
    }

    fun dismissDateRangePicker() {
        _isDateRangePickerOpen.value = false
    }

    fun resetFilters() {
        _filters.value = FilterCriteria()
    }

    fun deleteTransaction(id: String) {
        val tx = _uiState.value.transactions.find { it.id == id }
        if (tx != null && tx.type == TransactionType.INCOME) {
            val currentNet = _uiState.value.netTotal
            if (currentNet - tx.amount < 0L) {
                _userMessage.value = "لا يمكن حذف هذا الإيراد لأن هناك مصروفات مسجلة اعتمدت عليه، مما سيجعل الصافي بالسالب"
                return
            }
        }
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
