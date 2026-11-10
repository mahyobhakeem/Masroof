package com.masroofi.app.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * فئة ربط العملية بالتصنيف المقابل لها عبر علاقة Room المدمجة
 */
data class TransactionWithCategory(
    @Embedded
    val transaction: TransactionEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)
