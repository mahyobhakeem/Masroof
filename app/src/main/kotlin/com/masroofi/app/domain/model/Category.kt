package com.masroofi.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * نموذج نطاق العمل النقي للتصنيف المالي
 */
@Immutable
data class Category(
    val id: String,
    val name: String,
    val type: TransactionType,
    val iconKey: String,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)
