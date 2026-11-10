package com.masroofi.app.domain.model

/**
 * نموذج نطاق العمل النقي للميزانية الشهرية
 * يربط كل شهر بميزانيته المستقلة
 */
data class MonthlyBudget(
    val id: String,
    val yearMonth: String, // بصيغة YYYY-MM
    val amount: Long // بالريال اليمني خالية من الكسور
)
