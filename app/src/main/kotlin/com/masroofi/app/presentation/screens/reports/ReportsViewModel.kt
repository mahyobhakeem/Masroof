package com.masroofi.app.presentation.screens.reports

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masroofi.app.domain.calculations.CategoryExpenseSummary
import com.masroofi.app.domain.calculations.FinancialCalculator
import com.masroofi.app.domain.calculations.MonthlyReportData
import com.masroofi.app.domain.model.ReportPeriodMode
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.TransactionRepository
import com.masroofi.app.domain.usecase.GetMonthlyReportUseCase
import com.masroofi.app.presentation.components.PdfPeriodMode
import com.masroofi.app.presentation.components.TransactionTypeFilter
import com.masroofi.app.utils.FormatUtils
import com.masroofi.app.utils.PdfReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/**
 * حالة واجهة شاشة التقارير والإحصائيات (ReportsUiState)
 */
@Immutable
data class ReportsUiState(
    val periodMode: ReportPeriodMode = ReportPeriodMode.MONTHLY,
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val customStartDate: LocalDate = LocalDate.now().minusDays(30),
    val customEndDate: LocalDate = LocalDate.now(),
    val periodLabel: String = "",
    val reportData: MonthlyReportData = MonthlyReportData(YearMonth.now()),
    val selectedCategoryForDetails: CategoryExpenseSummary? = null,
    val isLoading: Boolean = false,
    val showDatePicker: Boolean = false,
    val showDateRangePicker: Boolean = false
)

/**
 * نموذج العرض لشاشة التقارير والإحصائيات (ReportsViewModel)
 * يدعم الاستعراض والتحليل الزمني المرن: شهري، أسبوعي، يوم محدد، وفترة مخصصة
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val getMonthlyReportUseCase: GetMonthlyReportUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _periodMode = MutableStateFlow(ReportPeriodMode.MONTHLY)
    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _customStartDate = MutableStateFlow(LocalDate.now().minusDays(30))
    private val _customEndDate = MutableStateFlow(LocalDate.now())
    private val _selectedCategoryForDetails = MutableStateFlow<CategoryExpenseSummary?>(null)
    private val _showDatePicker = MutableStateFlow(false)
    private val _showDateRangePicker = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                _periodMode,
                _selectedYearMonth,
                _selectedDate,
                _customStartDate,
                _customEndDate
            ) { mode, ym, date, start, end ->
                ReportsPeriodSettings(mode, ym, date, start, end)
            }.combine(transactionRepository.getAllTransactions()) { settings, allTx ->
                ReportsConfig(settings.mode, settings.ym, settings.date, settings.start, settings.end, allTx)
            }.flatMapLatest { config ->
                _selectedCategoryForDetails.value = null
                when (config.mode) {
                    ReportPeriodMode.MONTHLY -> {
                        getMonthlyReportUseCase(config.ym).map { report ->
                            val label = FormatUtils.formatYearMonth(config.ym)
                            Pair(label, report)
                        }
                    }
                    ReportPeriodMode.WEEKLY -> {
                        val (weekStart, weekEnd) = FormatUtils.getWeekRangeSaturday(config.date)
                        val periodTx = FinancialCalculator.filterTransactionsByDateRange(config.allTx, weekStart, weekEnd)
                        val report = FinancialCalculator.buildPeriodReport(weekStart, weekEnd, periodTx, config.allTx)
                        val label = "أسبوع: ${FormatUtils.formatArabicDate(weekStart)} - ${FormatUtils.formatArabicDate(weekEnd)}"
                        flowOf(Pair(label, report))
                    }
                    ReportPeriodMode.SPECIFIC_DAY -> {
                        val periodTx = FinancialCalculator.filterTransactionsByDateRange(config.allTx, config.date, config.date)
                        val report = FinancialCalculator.buildPeriodReport(config.date, config.date, periodTx, config.allTx)
                        val label = "${FormatUtils.formatArabicDayOfWeek(config.date)}، ${FormatUtils.formatArabicDate(config.date)}"
                        flowOf(Pair(label, report))
                    }
                    ReportPeriodMode.CUSTOM_RANGE -> {
                        val periodTx = FinancialCalculator.filterTransactionsByDateRange(config.allTx, config.start, config.end)
                        val report = FinancialCalculator.buildPeriodReport(config.start, config.end, periodTx, config.allTx)
                        val label = FormatUtils.formatDateRange(config.start, config.end)
                        flowOf(Pair(label, report))
                    }
                }.combine(_selectedCategoryForDetails) { (label, report), selectedCat ->
                    ReportsUiState(
                        periodMode = config.mode,
                        selectedYearMonth = config.ym,
                        selectedDate = config.date,
                        customStartDate = config.start,
                        customEndDate = config.end,
                        periodLabel = label,
                        reportData = report,
                        selectedCategoryForDetails = selectedCat,
                        isLoading = false,
                        showDatePicker = _showDatePicker.value,
                        showDateRangePicker = _showDateRangePicker.value
                    )
                }
            }.collect { state ->
                _uiState.value = state.copy(
                    showDatePicker = _showDatePicker.value,
                    showDateRangePicker = _showDateRangePicker.value
                )
            }
        }
    }

    private data class ReportsPeriodSettings(
        val mode: ReportPeriodMode,
        val ym: YearMonth,
        val date: LocalDate,
        val start: LocalDate,
        val end: LocalDate
    )

    private data class ReportsConfig(
        val mode: ReportPeriodMode,
        val ym: YearMonth,
        val date: LocalDate,
        val start: LocalDate,
        val end: LocalDate,
        val allTx: List<com.masroofi.app.domain.model.TransactionItem>
    )

    /**
     * تغيير وضع التحليل الزمني
     */
    fun setPeriodMode(mode: ReportPeriodMode) {
        _periodMode.value = mode
    }

    /**
     * الانتقال إلى الفترة السابقة (شهر، أسبوع، أو يوم سابق)
     */
    fun selectPrevious() {
        when (_periodMode.value) {
            ReportPeriodMode.MONTHLY -> _selectedYearMonth.update { it.minusMonths(1) }
            ReportPeriodMode.WEEKLY -> _selectedDate.update { it.minusWeeks(1) }
            ReportPeriodMode.SPECIFIC_DAY -> _selectedDate.update { it.minusDays(1) }
            ReportPeriodMode.CUSTOM_RANGE -> _showDateRangePicker.value = true
        }
    }

    fun selectPreviousMonth() = selectPrevious()

    /**
     * الانتقال إلى الفترة اللاحقة (شهر، أسبوع، أو يوم لاحق)
     */
    fun selectNext() {
        when (_periodMode.value) {
            ReportPeriodMode.MONTHLY -> _selectedYearMonth.update { it.plusMonths(1) }
            ReportPeriodMode.WEEKLY -> _selectedDate.update { it.plusWeeks(1) }
            ReportPeriodMode.SPECIFIC_DAY -> _selectedDate.update { it.plusDays(1) }
            ReportPeriodMode.CUSTOM_RANGE -> _showDateRangePicker.value = true
        }
    }

    fun selectNextMonth() = selectNext()

    /**
     * فتح نافذة التقويم بحسب الوضع الحالي
     */
    fun onDateLabelClick() {
        when (_periodMode.value) {
            ReportPeriodMode.MONTHLY -> {}
            ReportPeriodMode.WEEKLY,
            ReportPeriodMode.SPECIFIC_DAY -> _showDatePicker.value = true
            ReportPeriodMode.CUSTOM_RANGE -> _showDateRangePicker.value = true
        }
    }

    /**
     * تعيين تاريخ محدد تم اختياره من نافذة التقويم
     */
    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        _showDatePicker.value = false
        _uiState.update { it.copy(showDatePicker = false) }
    }

    /**
     * تعيين فترة مخصصة تم اختيارها من نافذة تحديد النطاق الزمني
     */
    fun setCustomRange(start: LocalDate, end: LocalDate) {
        _customStartDate.value = start
        _customEndDate.value = end
        _showDateRangePicker.value = false
        _uiState.update { it.copy(showDateRangePicker = false) }
    }

    fun dismissDatePicker() {
        _showDatePicker.value = false
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun dismissDateRangePicker() {
        _showDateRangePicker.value = false
        _uiState.update { it.copy(showDateRangePicker = false) }
    }

    /**
     * تحديد تصنيف لإظهار تفاصيله وتكبير شريحته في المخطط الدائري، أو إلغاء التحديد بالنقر مجدداً
     */
    fun selectCategory(category: CategoryExpenseSummary?) {
        _selectedCategoryForDetails.update { current ->
            if (current?.categoryId == category?.categoryId) null else category
        }
        _uiState.update {
            it.copy(
                selectedCategoryForDetails = if (it.selectedCategoryForDetails?.categoryId == category?.categoryId) null else category
            )
        }
    }

    /**
     * تعيين شهر محدد مباشرة
     */
    fun setSelectedYearMonth(yearMonth: YearMonth) {
        _selectedYearMonth.value = yearMonth
    }

    private var isExportingPdf = false

    /**
     * تصدير تقرير PDF مالي مخصص من شاشة التقارير
     */
    fun exportPdfReport(
        context: Context,
        periodMode: PdfPeriodMode,
        specificDate: LocalDate?,
        customStartDate: LocalDate?,
        customEndDate: LocalDate?,
        typeFilter: TransactionTypeFilter,
        currencySymbol: String,
        ownerName: String? = null,
        onSuccess: (File) -> Unit
    ) {
        if (isExportingPdf) return
        isExportingPdf = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allTransactions = transactionRepository.getAllTransactions().first()

                val (filteredByDate, periodLabel) = when (periodMode) {
                    PdfPeriodMode.TODAY -> {
                        val todayStr = LocalDate.now().toString()
                        allTransactions.filter { it.transactionDate == todayStr } to "اليوم (${FormatUtils.formatArabicDate(LocalDate.now(), true)})"
                    }
                    PdfPeriodMode.SPECIFIC_DAY -> {
                        val day = specificDate ?: LocalDate.now()
                        val dayStr = day.toString()
                        allTransactions.filter { it.transactionDate == dayStr } to "يوم ${FormatUtils.formatArabicDate(day, true)}"
                    }
                    PdfPeriodMode.THIS_WEEK -> {
                        val (start, end) = FormatUtils.getWeekRangeSaturday(LocalDate.now())
                        allTransactions.filter {
                            val parsed = try { it.toLocalDate() } catch (_: Exception) { null }
                            parsed != null && !parsed.isBefore(start) && !parsed.isAfter(end)
                        } to "هذا الأسبوع (${FormatUtils.formatDateRange(start, end)})"
                    }
                    PdfPeriodMode.THIS_MONTH -> {
                        val ym = YearMonth.now()
                        allTransactions.filter { it.transactionDate.startsWith(ym.toString()) } to "شهر ${FormatUtils.formatMonthName(ym.monthValue)} ${ym.year}"
                    }
                    PdfPeriodMode.PREVIOUS_MONTH -> {
                        val prevYm = YearMonth.now().minusMonths(1)
                        allTransactions.filter { it.transactionDate.startsWith(prevYm.toString()) } to "شهر ${FormatUtils.formatMonthName(prevYm.monthValue)} ${prevYm.year}"
                    }
                    PdfPeriodMode.CUSTOM_RANGE -> {
                        val start = customStartDate ?: LocalDate.now().minusDays(30)
                        val end = customEndDate ?: LocalDate.now()
                        allTransactions.filter {
                            val parsed = try { it.toLocalDate() } catch (_: Exception) { null }
                            parsed != null && !parsed.isBefore(start) && !parsed.isAfter(end)
                        } to "الفترة من ${FormatUtils.formatArabicDate(start)} إلى ${FormatUtils.formatArabicDate(end)}"
                    }
                }

                val finalTransactions = when (typeFilter) {
                    TransactionTypeFilter.ALL -> filteredByDate
                    TransactionTypeFilter.EXPENSE_ONLY -> filteredByDate.filter { it.type == TransactionType.EXPENSE }
                    TransactionTypeFilter.INCOME_ONLY -> filteredByDate.filter { it.type == TransactionType.INCOME }
                }.sortedByDescending { it.transactionDate }

                val pdfFile = PdfReportExporter.generatePdfReport(
                    context = context,
                    transactions = finalTransactions,
                    periodTitle = periodLabel,
                    currencySymbol = currencySymbol,
                    reportTitle = "تقرير العمليات المالية ($periodLabel)",
                    userName = ownerName?.ifBlank { null }
                )

                try {
                    PdfReportExporter.openPdf(context, pdfFile)
                } catch (_: Exception) {}

                withContext(Dispatchers.Main) {
                    onSuccess(pdfFile)
                }
            } catch (_: Exception) {
            } finally {
                isExportingPdf = false
            }
        }
    }
}

/**
 * مصنع إنشاء كائن ReportsViewModel
 */
class ReportsViewModelFactory(
    private val getMonthlyReportUseCase: GetMonthlyReportUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            return ReportsViewModel(getMonthlyReportUseCase, transactionRepository) as T
        }
        throw IllegalArgumentException("فئة نموذج العرض غير معروفة: ${modelClass.name}")
    }
}
