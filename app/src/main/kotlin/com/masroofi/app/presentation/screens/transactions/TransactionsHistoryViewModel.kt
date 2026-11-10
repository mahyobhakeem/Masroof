package com.masroofi.app.presentation.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.CategoryRepository
import com.masroofi.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * معايير تصفية قائمة العمليات المالية
 */
data class FilterCriteria(
    val query: String = "",
    val filterIndex: Int = 0, // 0: الكل, 1: مصروفات, 2: إيرادات
    val categoryName: String = "جميع التصنيفات"
)

/**
 * حالة واجهة مستخدم سجل العمليات
 */
data class TransactionsHistoryUiState(
    val transactions: List<TransactionItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val selectedFilterIndex: Int = 0,
    val selectedCategoryName: String = "جميع التصنيفات",
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

    val uiState: StateFlow<TransactionsHistoryUiState> = combine(
        transactionRepository.getAllTransactions(),
        categoryRepository.getAllCategories(),
        _filters,
        _userMessage
    ) { allTx: List<TransactionItem>, allCats: List<Category>, filters: FilterCriteria, msg: String? ->
        val filtered = allTx.filter { tx ->
            val matchesSearch = filters.query.isEmpty() ||
                    (tx.note?.contains(filters.query, ignoreCase = true) == true) ||
                    (tx.categoryName?.contains(filters.query, ignoreCase = true) == true) ||
                    tx.amount.toString().contains(filters.query)

            val matchesType = when (filters.filterIndex) {
                1 -> tx.type == TransactionType.EXPENSE
                2 -> tx.type == TransactionType.INCOME
                else -> true
            }

            val matchesCategory = filters.categoryName == "جميع التصنيفات" || tx.categoryName == filters.categoryName

            matchesSearch && matchesType && matchesCategory
        }

        TransactionsHistoryUiState(
            transactions = filtered,
            categories = allCats,
            searchQuery = filters.query,
            selectedFilterIndex = filters.filterIndex,
            selectedCategoryName = filters.categoryName,
            isLoading = false,
            userMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsHistoryUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _filters.update { it.copy(query = query) }
    }

    fun setFilterIndex(index: Int) {
        _filters.update { it.copy(filterIndex = index) }
    }

    fun setSelectedCategoryName(categoryName: String) {
        _filters.update { it.copy(categoryName = categoryName) }
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
