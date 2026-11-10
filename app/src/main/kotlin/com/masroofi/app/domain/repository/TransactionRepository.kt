package com.masroofi.app.domain.repository

import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * واجهة مستودع العمليات المالية في طبقة منطق العمل (TransactionRepository)
 */
interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionItem>>
    fun getTransactionsByMonth(yearMonth: String): Flow<List<TransactionItem>>
    fun getTransactionsByDateRange(startDate: String, endDate: String): Flow<List<TransactionItem>>
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionItem>>
    fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionItem>>
    fun getTransactionById(id: String): Flow<TransactionItem?>
    suspend fun addTransaction(transaction: TransactionItem): Result<Unit>
    suspend fun updateTransaction(transaction: TransactionItem): Result<Unit>
    suspend fun deleteTransaction(id: String): Result<Unit>
}
