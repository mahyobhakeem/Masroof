package com.masroofi.app.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masroofi.app.domain.calculations.CategoryExpenseSummary
import com.masroofi.app.domain.calculations.FinancialSummary
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.repository.TransactionRepository
import com.masroofi.app.domain.usecase.GetMonthlyFinancialSummaryUseCase
import com.masroofi.app.domain.usecase.SetMonthlyBudgetUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * حالة واجهة الشاشة الرئيسية لتطبيق مصروفي
 */
data class HomeUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val summary: FinancialSummary = FinancialSummary(yearMonth = YearMonth.now()),
    val topCategories: List<CategoryExpenseSummary> = emptyList(),
    val recentTransactions: List<TransactionItem> = emptyList(),
    val isLoading: Boolean = false,
    val isBudgetDialogOpen: Boolean = false,
    val budgetInput: String = "",
    val budgetError: String? = null,
    val isSavingBudget: Boolean = false,
    val selectedTransactionForDetails: TransactionItem? = null,
    val isDeleteDialogOpen: Boolean = false,
    val transactionToDelete: TransactionItem? = null
)

/**
 * نموذج العرض للشاشة الرئيسية (HomeViewModel)
 * يدير اختيار الشهر الحالي والمؤشرات المالية السبعة والحوارات التفاعلية
 */
class HomeViewModel(
    private val getMonthlyFinancialSummaryUseCase: GetMonthlyFinancialSummaryUseCase,
    private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentMonthJob: Job? = null

    init {
        observeMonthData(_uiState.value.selectedMonth)
    }

    /**
     * مراقبة تدفق بيانات الشهر المحدد لحظياً عبر UseCase
     */
    private fun observeMonthData(month: YearMonth) {
        currentMonthJob?.cancel()
        currentMonthJob = viewModelScope.launch {
            getMonthlyFinancialSummaryUseCase(month).collect { homeData ->
                _uiState.update { current ->
                    current.copy(
                        selectedMonth = month,
                        summary = homeData.summary,
                        topCategories = homeData.topCategories,
                        recentTransactions = homeData.recentTransactions,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * الانتقال إلى الشهر السابق
     */
    fun selectPreviousMonth() {
        val newMonth = _uiState.value.selectedMonth.minusMonths(1)
        _uiState.update { it.copy(selectedMonth = newMonth) }
        observeMonthData(newMonth)
    }

    /**
     * الانتقال إلى الشهر التالي
     */
    fun selectNextMonth() {
        val newMonth = _uiState.value.selectedMonth.plusMonths(1)
        _uiState.update { it.copy(selectedMonth = newMonth) }
        observeMonthData(newMonth)
    }

    /**
     * العودة إلى الشهر الحالي
     */
    fun selectCurrentMonth() {
        val newMonth = YearMonth.now()
        _uiState.update { it.copy(selectedMonth = newMonth) }
        observeMonthData(newMonth)
    }

    /**
     * فتح نافذة تحديد أو تعديل الميزانية للشهر الحالي
     */
    fun openBudgetDialog() {
        val currentBudget = _uiState.value.summary.monthlyBudget
        _uiState.update {
            it.copy(
                isBudgetDialogOpen = true,
                budgetInput = currentBudget?.toString() ?: "",
                budgetError = null,
                isSavingBudget = false
            )
        }
    }

    /**
     * إغلاق نافذة تحديد الميزانية
     */
    fun dismissBudgetDialog() {
        _uiState.update {
            it.copy(
                isBudgetDialogOpen = false,
                budgetInput = "",
                budgetError = null
            )
        }
    }

    /**
     * تحديث حقل إدخال الميزانية (أرقام فقط)
     */
    fun onBudgetInputChanged(input: String) {
        val filtered = input.filter { it.isDigit() }
        _uiState.update {
            it.copy(
                budgetInput = filtered,
                budgetError = if (filtered.isNotEmpty()) null else it.budgetError
            )
        }
    }

    /**
     * حفظ الميزانية في قاعدة البيانات
     */
    fun saveMonthlyBudget() {
        val currentInput = _uiState.value.budgetInput
        val amount = currentInput.toLongOrNull()
        if (amount == null || amount <= 0L) {
            _uiState.update { it.copy(budgetError = "يرجى إدخال مبلغ صحيح أكبر من الصفر") }
            return
        }

        if (_uiState.value.isSavingBudget) return
        _uiState.update { it.copy(isSavingBudget = true) }

        viewModelScope.launch {
            val result = setMonthlyBudgetUseCase(_uiState.value.selectedMonth, amount)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isBudgetDialogOpen = false,
                        isSavingBudget = false,
                        budgetInput = "",
                        budgetError = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSavingBudget = false,
                        budgetError = result.exceptionOrNull()?.message ?: "فشل في حفظ الميزانية"
                    )
                }
            }
        }
    }

    /**
     * فتح نافذة تفاصيل العملية عند النقر عليها
     */
    fun onTransactionClick(transaction: TransactionItem) {
        _uiState.update { it.copy(selectedTransactionForDetails = transaction) }
    }

    /**
     * إغلاق نافذة تفاصيل العملية
     */
    fun dismissTransactionDetails() {
        _uiState.update { it.copy(selectedTransactionForDetails = null) }
    }

    /**
     * طلب حذف العملية وفتح نافذة التأكيد
     */
    fun requestDeleteTransaction(transaction: TransactionItem) {
        _uiState.update {
            it.copy(
                isDeleteDialogOpen = true,
                transactionToDelete = transaction
            )
        }
    }

    /**
     * إلغاء نافذة تأكيد الحذف
     */
    fun dismissDeleteDialog() {
        _uiState.update {
            it.copy(
                isDeleteDialogOpen = false,
                transactionToDelete = null
            )
        }
    }

    /**
     * تأكيد حذف العملية المالية نهائياً
     */
    fun confirmDeleteTransaction() {
        val tx = _uiState.value.transactionToDelete ?: return
        viewModelScope.launch {
            transactionRepository.deleteTransaction(tx.id)
            _uiState.update {
                it.copy(
                    isDeleteDialogOpen = false,
                    transactionToDelete = null,
                    selectedTransactionForDetails = null
                )
            }
        }
    }
}

/**
 * مصنع إنشاء نموذج العرض للشاشة الرئيسية
 */
class HomeViewModelFactory(
    private val getMonthlyFinancialSummaryUseCase: GetMonthlyFinancialSummaryUseCase,
    private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                getMonthlyFinancialSummaryUseCase = getMonthlyFinancialSummaryUseCase,
                setMonthlyBudgetUseCase = setMonthlyBudgetUseCase,
                transactionRepository = transactionRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
