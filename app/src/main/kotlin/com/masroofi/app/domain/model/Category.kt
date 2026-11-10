package com.masroofi.app.domain.model

/**
 * نموذج نطاق العمل النقي للتصنيف المالي
 */
data class Category(
    val id: String,
    val name: String,
    val type: TransactionType,
    val iconKey: String,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)
