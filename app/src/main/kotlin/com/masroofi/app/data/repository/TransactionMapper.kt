package com.masroofi.app.data.repository

import com.masroofi.app.data.entity.TransactionEntity
import com.masroofi.app.data.entity.TransactionWithCategory
import com.masroofi.app.domain.model.TransactionItem

/**
 * دوال تحويل بين كيان جدول العمليات ونموذج نطاق العمل النقي
 */
fun TransactionEntity.toDomain(
    categoryName: String? = null,
    categoryIconKey: String? = null
): TransactionItem {
    return TransactionItem(
        id = id,
        type = type,
        amount = amount,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryIconKey = categoryIconKey,
        transactionDate = transactionDate,
        note = note,
        currencyCode = currencyCode,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun TransactionWithCategory.toDomain(): TransactionItem {
    return TransactionItem(
        id = transaction.id,
        type = transaction.type,
        amount = transaction.amount,
        categoryId = transaction.categoryId,
        categoryName = category?.name,
        categoryIconKey = category?.iconKey,
        transactionDate = transaction.transactionDate,
        note = transaction.note,
        currencyCode = transaction.currencyCode,
        createdAt = transaction.createdAt,
        updatedAt = transaction.updatedAt
    )
}

fun TransactionItem.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        type = type,
        amount = amount,
        categoryId = categoryId,
        transactionDate = transactionDate,
        note = note,
        currencyCode = currencyCode,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
