package com.masroofi.app.data.repository

import com.masroofi.app.data.entity.MonthlyBudgetEntity
import com.masroofi.app.domain.model.MonthlyBudget

/**
 * دوال تحويل بين كيان جدول الميزانيات ونموذج نطاق العمل النقي
 */
fun MonthlyBudgetEntity.toDomain(): MonthlyBudget {
    return MonthlyBudget(
        id = id,
        yearMonth = yearMonth,
        amount = amount
    )
}

fun MonthlyBudget.toEntity(): MonthlyBudgetEntity {
    return MonthlyBudgetEntity(
        id = id,
        yearMonth = yearMonth,
        amount = amount
    )
}
