package com.masroofi.app.domain.repository

import com.masroofi.app.domain.model.MonthlyBudget
import kotlinx.coroutines.flow.Flow

/**
 * واجهة مستودع الميزانيات الشهرية في طبقة منطق العمل (MonthlyBudgetRepository)
 */
interface MonthlyBudgetRepository {
    fun getBudgetForMonth(yearMonth: String): Flow<MonthlyBudget?>
    fun getAllBudgets(): Flow<List<MonthlyBudget>>
    suspend fun setBudgetForMonth(yearMonth: String, amount: Long): Result<Unit>
    suspend fun deleteBudget(id: String): Result<Unit>
}
