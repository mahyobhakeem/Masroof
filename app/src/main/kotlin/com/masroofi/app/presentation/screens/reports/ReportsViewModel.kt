package com.masroofi.app.presentation.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masroofi.app.domain.calculations.CategoryExpenseSummary
import com.masroofi.app.domain.calculations.MonthlyReportData
import com.masroofi.app.domain.usecase.GetMonthlyReportUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * حالة واجهة شاشة التقارير والإحصائيات (ReportsUiState)
 */
data class ReportsUiState(
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val reportData: MonthlyReportData = MonthlyReportData(YearMonth.now()),
    val selectedCategoryForDetails: CategoryExpenseSummary? = null,
    val isLoading: Boolean = false
)

/**
 * نموذج العرض لشاشة التقارير والإحصائيات (ReportsViewModel)
 * يتولى إدارة الشهر المختار والتفاعل مع المخططات البيانية
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val getMonthlyReportUseCase: GetMonthlyReportUseCase
) : ViewModel() {

    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    private val _selectedCategoryForDetails = MutableStateFlow<CategoryExpenseSummary?>(null)
    private val _uiState = MutableStateFlow(ReportsUiState())

    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedYearMonth.flatMapLatest { ym ->
                _selectedCategoryForDetails.value = null
                getMonthlyReportUseCase(ym).combine(_selectedCategoryForDetails) { report, selectedCat ->
                    ReportsUiState(
                        selectedYearMonth = ym,
                        reportData = report,
                        selectedCategoryForDetails = selectedCat,
                        isLoading = false
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * الانتقال إلى الشهر السابق
     */
    fun selectPreviousMonth() {
        _selectedYearMonth.update { it.minusMonths(1) }
    }

    /**
     * الانتقال إلى الشهر اللاحق
     */
    fun selectNextMonth() {
        _selectedYearMonth.update { it.plusMonths(1) }
    }

    /**
     * تحديد تصنيف لإظهار تفاصيله وتكبير شريحته في المخطط الدائري، أو إلغاء التحديد بالنقر مجدداً
     */
    fun selectCategory(category: CategoryExpenseSummary?) {
        _selectedCategoryForDetails.update { current ->
            if (current?.categoryId == category?.categoryId) null else category
        }
    }

    /**
     * تعيين شهر محدد مباشرة
     */
    fun setSelectedYearMonth(yearMonth: YearMonth) {
        _selectedYearMonth.value = yearMonth
    }
}

/**
 * مصنع إنشاء كائن ReportsViewModel
 */
class ReportsViewModelFactory(
    private val getMonthlyReportUseCase: GetMonthlyReportUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            return ReportsViewModel(getMonthlyReportUseCase) as T
        }
        throw IllegalArgumentException("فئة نموذج العرض غير معروفة: ${modelClass.name}")
    }
}
