package com.masroofi.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.masroofi.app.domain.model.TransactionType

/**
 * كيان جدول التصنيفات المالية في قاعدة البيانات المحلية (categories)
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: TransactionType,
    val iconKey: String,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)
