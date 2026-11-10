package com.masroofi.app.data.dao

import androidx.room.*
import com.masroofi.app.data.entity.MonthlyBudgetEntity
import kotlinx.coroutines.flow.Flow

/**
 * واجهة الاستعلام والتعامل مع جدول الميزانيات الشهرية (MonthlyBudgetDao)
 */
@Dao
interface MonthlyBudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: MonthlyBudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBudget(budget: MonthlyBudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: MonthlyBudgetEntity): Int

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth LIMIT 1")
    suspend fun getBudgetForMonth(yearMonth: String): MonthlyBudgetEntity?

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth LIMIT 1")
    fun getBudgetForMonthFlow(yearMonth: String): Flow<MonthlyBudgetEntity?>

    @Query("SELECT * FROM monthly_budgets ORDER BY yearMonth DESC")
    fun getAllBudgetsFlow(): Flow<List<MonthlyBudgetEntity>>

    @Delete
    suspend fun deleteBudget(budget: MonthlyBudgetEntity): Int

    @Query("DELETE FROM monthly_budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: String): Int
}
