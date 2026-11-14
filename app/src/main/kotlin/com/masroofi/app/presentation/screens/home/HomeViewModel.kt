package com.masroofi.app.presentation.screens.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masroofi.app.domain.calculations.CategoryExpenseSummary
import com.masroofi.app.domain.calculations.FinancialCalculator
import com.masroofi.app.domain.calculations.FinancialSummary
import com.masroofi.app.domain.model.TimePeriodMode
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.TransactionRepository
import com.masroofi.app.domain.usecase.GetMonthlyFinancialSummaryUseCase
import com.masroofi.app.domain.usecase.SetMonthlyBudgetUseCase
import com.masroofi.app.utils.FormatUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * حالة واجهة الشاشة الرئيسية لتطبيق مصروف
 */
@Immutable
data class HomeUiState(
    val selectedPeriodMode: TimePeriodMode = TimePeriodMode.MONTHLY,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedWeekStart: LocalDate = FormatUtils.getWeekRangeSaturday(LocalDate.now()).first,
    val selectedMonth: YearMonth = YearMonth.now(),
    val customStartDate: LocalDate = LocalDate.now().minusDays(30),
    val customEndDate: LocalDate = LocalDate.now(),
    val summary: FinancialSummary = FinancialSummary(yearMonth = YearMonth.now()),
    val periodTotalExpenses: Long = 0L,
    val periodTotalIncome: Long = 0L,
    val periodNetCashflow: Long = 0L,
    val topCategories: List<CategoryExpenseSummary> = emptyList(),
    val recentTransactions: List<TransactionItem> = emptyList(),
    val isLoading: Boolean = false,
    val isDatePickerOpen: Boolean = false,
    val isCustomRangePickerOpen: Boolean = false,
    val isBudgetDialogOpen: Boolean = false,
    val budgetInput: String = "",
    val budgetError: String? = null,
    val isSavingBudget: Boolean = false,
    val selectedTransactionForDetails: TransactionItem? = null,
    val isDeleteDialogOpen: Boolean = false,
    val transactionToDelete: TransactionItem? = null,
    val userMessage: String? = null
)

/**
 * نموذج العرض للشاشة الرئيسية (HomeViewModel)
 * يدير خيارات الاستعراض الزمني الأربعة (يوم، أسبوع، شهر، فترة مخصصة)
 * والحسابات المالية والتفاعلات اللحظية
 */
class HomeViewModel(
    private val getMonthlyFinancialSummaryUseCase: GetMonthlyFinancialSummaryUseCase,
    private val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentDataJob: Job? = null

    init {
        observeCurrentPeriodData()
    }

    /**
     * تغيير وضع الاستعراض الزمني (يوم / أسبوع / شهر / فترة مخصصة)
     */
    fun setPeriodMode(mode: TimePeriodMode) {
        if (_uiState.value.selectedPeriodMode == mode) return
        _uiState.update { it.copy(selectedPeriodMode = mode, isLoading = true) }
        observeCurrentPeriodData()
    }

    /**
     * مراقبة بيانات الفترة المحددة لحظياً بحسب الوضع المختار
     */
    private fun observeCurrentPeriodData() {
        currentDataJob?.cancel()
        val state = _uiState.value

        currentDataJob = viewModelScope.launch {
            when (state.selectedPeriodMode) {
                TimePeriodMode.MONTHLY -> {
                    getMonthlyFinancialSummaryUseCase(state.selectedMonth).collect { homeData ->
                        _uiState.update { current ->
                            current.copy(
                                summary = homeData.summary,
                                periodTotalExpenses = homeData.summary.totalMonthlyExpenses,
                                periodTotalIncome = homeData.summary.totalMonthlyIncome,
                                periodNetCashflow = homeData.summary.netCashflow,
                                topCategories = homeData.topCategories,
                                recentTransactions = homeData.recentTransactions,
                                isLoading = false
                            )
                        }
                    }
                }
                TimePeriodMode.DAILY -> {
                    val dateStr = state.selectedDate.toString()
                    transactionRepository.getTransactionsByDateRange(dateStr, dateStr).collect { transactions ->
                        val exp = FinancialCalculator.calculateMonthlyExpenses(transactions)
                        val inc = FinancialCalculator.calculateMonthlyIncome(transactions)
                        val net = FinancialCalculator.calculateNetCashflow(inc, exp)
                        val topCats = FinancialCalculator.calculateTopCategories(transactions, exp)
                        _uiState.update { current ->
                            current.copy(
                                periodTotalExpenses = exp,
                                periodTotalIncome = inc,
                                periodNetCashflow = net,
                                topCategories = topCats,
                                recentTransactions = transactions,
                                isLoading = false
                            )
                        }
                    }
                }
                TimePeriodMode.WEEKLY -> {
                    val (start, end) = FormatUtils.getWeekRangeSaturday(state.selectedWeekStart)
                    transactionRepository.getTransactionsByDateRange(start.toString(), end.toString()).collect { transactions ->
                        val exp = FinancialCalculator.calculateMonthlyExpenses(transactions)
                        val inc = FinancialCalculator.calculateMonthlyIncome(transactions)
                        val net = FinancialCalculator.calculateNetCashflow(inc, exp)
                        val topCats = FinancialCalculator.calculateTopCategories(transactions, exp)
                        _uiState.update { current ->
                            current.copy(
                                periodTotalExpenses = exp,
                                periodTotalIncome = inc,
                                periodNetCashflow = net,
                                topCategories = topCats,
                                recentTransactions = transactions,
                                isLoading = false
                            )
                        }
                    }
                }
                TimePeriodMode.CUSTOM_RANGE -> {
                    val startStr = state.customStartDate.toString()
                    val endStr = state.customEndDate.toString()
                    transactionRepository.getTransactionsByDateRange(startStr, endStr).collect { transactions ->
                        val exp = FinancialCalculator.calculateMonthlyExpenses(transactions)
                        val inc = FinancialCalculator.calculateMonthlyIncome(transactions)
                        val net = FinancialCalculator.calculateNetCashflow(inc, exp)
                        val topCats = FinancialCalculator.calculateTopCategories(transactions, exp)
                        _uiState.update { current ->
                            current.copy(
                                periodTotalExpenses = exp,
                                periodTotalIncome = inc,
                                periodNetCashflow = net,
                                topCategories = topCats,
                                recentTransactions = transactions,
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }

    // --- عمليات التحكم باليوم ---

    fun selectPreviousDay() {
        val newDate = _uiState.value.selectedDate.minusDays(1)
        _uiState.update { it.copy(selectedDate = newDate) }
        observeCurrentPeriodData()
    }

    fun selectNextDay() {
        val newDate = _uiState.value.selectedDate.plusDays(1)
        _uiState.update { it.copy(selectedDate = newDate) }
        observeCurrentPeriodData()
    }

    fun setSelectedDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date, isDatePickerOpen = false) }
        observeCurrentPeriodData()
    }

    fun openDatePicker() {
        _uiState.update { it.copy(isDatePickerOpen = true) }
    }

    fun dismissDatePicker() {
        _uiState.update { it.copy(isDatePickerOpen = false) }
    }

    // --- عمليات التحكم بالأسبوع ---

    fun selectPreviousWeek() {
        val newWeek = _uiState.value.selectedWeekStart.minusWeeks(1)
        _uiState.update { it.copy(selectedWeekStart = newWeek) }
        observeCurrentPeriodData()
    }

    fun selectNextWeek() {
        val newWeek = _uiState.value.selectedWeekStart.plusWeeks(1)
        _uiState.update { it.copy(selectedWeekStart = newWeek) }
        observeCurrentPeriodData()
    }

    // --- عمليات التحكم بالشهر ---

    fun selectPreviousMonth() {
        val newMonth = _uiState.value.selectedMonth.minusMonths(1)
        _uiState.update { it.copy(selectedMonth = newMonth) }
        observeCurrentPeriodData()
    }

    fun selectNextMonth() {
        val newMonth = _uiState.value.selectedMonth.plusMonths(1)
        _uiState.update { it.copy(selectedMonth = newMonth) }
        observeCurrentPeriodData()
    }

    fun selectCurrentMonth() {
        val newMonth = YearMonth.now()
        _uiState.update { it.copy(selectedMonth = newMonth) }
        observeCurrentPeriodData()
    }

    // --- عمليات التحكم بالفترة المخصصة ---

    fun setCustomRange(startDate: LocalDate, endDate: LocalDate) {
        _uiState.update {
            it.copy(
                customStartDate = startDate,
                customEndDate = endDate,
                isCustomRangePickerOpen = false
            )
        }
        observeCurrentPeriodData()
    }

    fun openCustomRangePicker() {
        _uiState.update { it.copy(isCustomRangePickerOpen = true) }
    }

    fun dismissCustomRangePicker() {
        _uiState.update { it.copy(isCustomRangePickerOpen = false) }
    }

    // --- إدارة الميزانية الشهرية ---

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

    fun dismissBudgetDialog() {
        _uiState.update {
            it.copy(
                isBudgetDialogOpen = false,
                budgetInput = "",
                budgetError = null
            )
        }
    }

    fun onBudgetInputChanged(input: String) {
        val filtered = input.filter { it.isDigit() }
        _uiState.update {
            it.copy(
                budgetInput = filtered,
                budgetError = if (filtered.isNotEmpty()) null else it.budgetError
            )
        }
    }

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

    // --- إدارة العمليات والتفاصيل والحذف ---

    fun onTransactionClick(transaction: TransactionItem) {
        _uiState.update { it.copy(selectedTransactionForDetails = transaction) }
    }

    fun dismissTransactionDetails() {
        _uiState.update { it.copy(selectedTransactionForDetails = null) }
    }

    fun requestDeleteTransaction(transaction: TransactionItem) {
        _uiState.update {
            it.copy(
                isDeleteDialogOpen = true,
                transactionToDelete = transaction,
                selectedTransactionForDetails = null
            )
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update {
            it.copy(
                isDeleteDialogOpen = false,
                transactionToDelete = null
            )
        }
    }

    fun confirmDeleteTransaction() {
        val tx = _uiState.value.transactionToDelete ?: return
        if (tx.type == TransactionType.INCOME) {
            val netCashflow = _uiState.value.periodNetCashflow
            if (netCashflow - tx.amount < 0L) {
                _uiState.update {
                    it.copy(
                        isDeleteDialogOpen = false,
                        transactionToDelete = null,
                        selectedTransactionForDetails = null,
                        userMessage = "لا يمكن حذف هذا الإيراد لأن هناك مصروفات مسجلة اعتمدت عليه، مما سيجعل الصافي بالسالب"
                    )
                }
                return
            }
        }
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

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
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
