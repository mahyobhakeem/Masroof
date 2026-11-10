package com.masroofi.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * كيان جدول الميزانيات الشهرية في قاعدة البيانات المحلية (monthly_budgets)
 * يضمن الفهرس الفريد (Unique Index) على حقل yearMonth استحالة وجود سجلين لنفس الشهر
 */
@Entity(
    tableName = "monthly_budgets",
    indices = [
        Index(value = ["yearMonth"], unique = true)
    ]
)
data class MonthlyBudgetEntity(
    @PrimaryKey
    val id: String,
    val yearMonth: String, // بصيغة YYYY-MM
    val amount: Long // بالريال اليمني
) {
    init {
        require(amount >= 0) { "يجب ألا تكون الميزانية الشهرية سالبة" }
    }
}
