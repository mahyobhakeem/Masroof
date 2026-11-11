package com.masroofi.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * نموذج نطاق العمل النقي للميزانية الشهرية
 * يربط كل شهر بميزانيته المستقلة
 */
@Immutable
data class MonthlyBudget(
    val id: String,
    val yearMonth: String, // بصيغة YYYY-MM
    val amount: Long, // خالية من الكسور
    val currencyCode: String = "YER" // كود العملة (ISO 4217)
)
