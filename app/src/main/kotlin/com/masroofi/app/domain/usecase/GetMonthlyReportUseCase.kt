package com.masroofi.app.domain.usecase

import com.masroofi.app.domain.calculations.FinancialCalculator
import com.masroofi.app.domain.calculations.MonthlyReportData
import com.masroofi.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

/**
 * حالة استخدام جلب التقرير المالي والإحصائي لشهر محدد (GetMonthlyReportUseCase)
 * تقوم بحساب المؤشرات السبعة والرسوم البيانية الثلاثة من قاعدة البيانات
 */
class GetMonthlyReportUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(
        yearMonth: YearMonth,
        currentDate: LocalDate = LocalDate.now()
    ): Flow<MonthlyReportData> {
        val monthPrefix = yearMonth.toString()
        return transactionRepository.getAllTransactions().map { allTransactions ->
            val monthTransactions = allTransactions.filter { it.transactionDate.startsWith(monthPrefix) }
            FinancialCalculator.buildMonthlyReport(
                selectedMonth = yearMonth,
                monthTransactions = monthTransactions,
                allTransactions = allTransactions,
                currentDate = currentDate
            )
        }
    }
}
