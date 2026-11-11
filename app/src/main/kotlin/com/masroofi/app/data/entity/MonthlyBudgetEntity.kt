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
    val amount: Long, // خالية من الكسور
    val currencyCode: String = "YER" // كود العملة وفق معيار ISO 4217
) {
    init {
        require(amount >= 0) { "يجب ألا تكون الميزانية الشهرية سالبة" }
    }
}
