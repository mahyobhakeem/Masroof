package com.masroofi.app.domain.usecase

import com.masroofi.app.domain.repository.MonthlyBudgetRepository
import java.time.YearMonth

/**
 * حالة الاستخدام المسؤولة عن تعيين أو تعديل ميزانية شهر محدد
 */
class SetMonthlyBudgetUseCase(
    private val monthlyBudgetRepository: MonthlyBudgetRepository
) {
    suspend operator fun invoke(yearMonth: YearMonth, amount: Long): Result<Unit> {
        if (amount <= 0L) {
            return Result.failure(IllegalArgumentException("يجب أن تكون الميزانية أكبر من الصفر دائماً"))
        }
        return monthlyBudgetRepository.setBudgetForMonth(yearMonth.toString(), amount)
    }
}
