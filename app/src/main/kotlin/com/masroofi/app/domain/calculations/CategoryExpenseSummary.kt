package com.masroofi.app.domain.calculations

import androidx.compose.runtime.Immutable

/**
 * نموذج ملخص نفقات تصنيف مالي محدد خلال الشهر
 */
@Immutable
data class CategoryExpenseSummary(
    val categoryId: String,
    val categoryName: String,
    val iconKey: String,
    val colorHex: String,
    val totalSpent: Long,
    val percentage: Int,
    val transactionCount: Int = 1
)
