package com.masroofi.app.domain.calculations

/**
 * نموذج ملخص نفقات تصنيف مالي محدد خلال الشهر
 */
data class CategoryExpenseSummary(
    val categoryId: String,
    val categoryName: String,
    val iconKey: String,
    val colorHex: String,
    val totalSpent: Long,
    val percentage: Int,
    val transactionCount: Int = 1
)
