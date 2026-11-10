package com.masroofi.app.data.repository

import com.masroofi.app.data.dao.TransactionDao
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * التنفيذ الفعلي لمستودع العمليات المالية في طبقة البيانات
 */
class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<TransactionItem>> {
        return transactionDao.getAllTransactionsWithCategoryFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTransactionsByMonth(yearMonth: String): Flow<List<TransactionItem>> {
        return transactionDao.getTransactionsWithCategoryByMonthFlow(yearMonth).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTransactionsByDateRange(
        startDate: String,
        endDate: String
    ): Flow<List<TransactionItem>> {
        return transactionDao.getTransactionsWithCategoryByDateRangeFlow(startDate, endDate).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionItem>> {
        return transactionDao.getTransactionsWithCategoryByTypeFlow(type).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionItem>> {
        return transactionDao.getTransactionsWithCategoryByCategoryFlow(categoryId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTransactionById(id: String): Flow<TransactionItem?> {
        return transactionDao.getTransactionWithCategoryById(id).map { it?.toDomain() }
    }

    override suspend fun addTransaction(transaction: TransactionItem): Result<Unit> = runCatching {
        require(transaction.amount > 0) { "يجب أن يكون المبلغ المالي أكبر من الصفر دائماً" }
        transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: TransactionItem): Result<Unit> = runCatching {
        require(transaction.amount > 0) { "يجب أن يكون المبلغ المالي أكبر من الصفر دائماً" }
        transactionDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> = runCatching {
        transactionDao.deleteTransactionById(id)
    }
}
