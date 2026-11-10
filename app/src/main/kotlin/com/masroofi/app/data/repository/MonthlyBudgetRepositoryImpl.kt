package com.masroofi.app.data.repository

import com.masroofi.app.data.dao.MonthlyBudgetDao
import com.masroofi.app.data.entity.MonthlyBudgetEntity
import com.masroofi.app.domain.model.MonthlyBudget
import com.masroofi.app.domain.repository.MonthlyBudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * التنفيذ الفعلي لمستودع الميزانيات الشهرية في طبقة البيانات
 */
class MonthlyBudgetRepositoryImpl(
    private val monthlyBudgetDao: MonthlyBudgetDao
) : MonthlyBudgetRepository {

    override fun getBudgetForMonth(yearMonth: String): Flow<MonthlyBudget?> {
        return monthlyBudgetDao.getBudgetForMonthFlow(yearMonth).map { it?.toDomain() }
    }

    override fun getAllBudgets(): Flow<List<MonthlyBudget>> {
        return monthlyBudgetDao.getAllBudgetsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun setBudgetForMonth(yearMonth: String, amount: Long): Result<Unit> = runCatching {
        require(amount >= 0) { "يجب ألا تكون الميزانية سالبة" }
        val existing = monthlyBudgetDao.getBudgetForMonth(yearMonth)
        val budgetEntity = if (existing != null) {
            existing.copy(amount = amount)
        } else {
            MonthlyBudgetEntity(
                id = UUID.randomUUID().toString(),
                yearMonth = yearMonth,
                amount = amount
            )
        }
        monthlyBudgetDao.insertOrUpdateBudget(budgetEntity)
    }

    override suspend fun deleteBudget(id: String): Result<Unit> = runCatching {
        monthlyBudgetDao.deleteBudgetById(id)
    }
}
