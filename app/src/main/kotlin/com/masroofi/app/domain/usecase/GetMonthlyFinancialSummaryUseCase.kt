package com.masroofi.app.domain.usecase

import com.masroofi.app.domain.calculations.CategoryExpenseSummary
import com.masroofi.app.domain.calculations.FinancialCalculator
import com.masroofi.app.domain.calculations.FinancialSummary
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.repository.MonthlyBudgetRepository
import com.masroofi.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth

/**
 * بيانات الشاشة الرئيسية المجمعة للشهر المختار
 */
data class HomeData(
    val summary: FinancialSummary,
    val topCategories: List<CategoryExpenseSummary>,
    val recentTransactions: List<TransactionItem>
)

/**
 * حالة الاستخدام المسؤولة عن تجميع وحساب بيانات الشاشة الرئيسية لشهر محدد
 */
class GetMonthlyFinancialSummaryUseCase(
    private val transactionRepository: TransactionRepository,
    private val monthlyBudgetRepository: MonthlyBudgetRepository
) {
    operator fun invoke(
        yearMonth: YearMonth,
        targetDate: LocalDate = LocalDate.now()
    ): Flow<HomeData> {
        val yearMonthStr = yearMonth.toString()
        val transactionsFlow = transactionRepository.getTransactionsByMonth(yearMonthStr)
        val budgetFlow = monthlyBudgetRepository.getBudgetForMonth(yearMonthStr)

        return combine(transactionsFlow, budgetFlow) { transactions, budget ->
            val summary = FinancialCalculator.buildFinancialSummary(
                yearMonth = yearMonth,
                transactions = transactions,
                budgetAmount = budget?.amount,
                targetDate = targetDate
            )
            val topCategories = FinancialCalculator.calculateTopCategories(
                transactions = transactions,
                totalMonthlyExpenses = summary.totalMonthlyExpenses
            )
            val recent = transactions.take(5)

            HomeData(
                summary = summary,
                topCategories = topCategories,
                recentTransactions = recent
            )
        }
    }
}
