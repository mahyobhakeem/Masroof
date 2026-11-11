package com.masroofi.app.data.dao

import androidx.room.*
import com.masroofi.app.data.entity.TransactionEntity
import com.masroofi.app.data.entity.TransactionWithCategory
import com.masroofi.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * واجهة الاستعلام والتعامل مع جدول العمليات المالية (TransactionDao)
 */
@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity): Int

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String): Int

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionByIdFlow(id: String): Flow<TransactionEntity?>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionWithCategoryById(id: String): Flow<TransactionWithCategory?>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC, createdAt DESC")
    fun getAllTransactionsWithCategoryFlow(): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE transactionDate LIKE :yearMonth || '%' ORDER BY transactionDate DESC, createdAt DESC")
    fun getTransactionsWithCategoryByMonthFlow(yearMonth: String): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC, createdAt DESC")
    fun getTransactionsWithCategoryByDateRangeFlow(startDate: String, endDate: String): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY transactionDate DESC, createdAt DESC")
    fun getTransactionsWithCategoryByTypeFlow(type: TransactionType): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY transactionDate DESC, createdAt DESC")
    fun getTransactionsWithCategoryByCategoryFlow(categoryId: String): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions ORDER BY transactionDate ASC, createdAt ASC")
    suspend fun getAllTransactionEntities(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransactionEntities(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions(): Int
}
