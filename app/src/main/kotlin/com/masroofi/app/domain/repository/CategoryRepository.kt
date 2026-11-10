package com.masroofi.app.domain.repository

import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * واجهة مستودع التصنيفات المالية في طبقة منطق العمل (CategoryRepository)
 */
interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getActiveCategories(type: TransactionType? = null): Flow<List<Category>>
    fun getCategoryById(id: String): Flow<Category?>
    suspend fun addCategory(category: Category): Result<Unit>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun setCategoryActive(id: String, isActive: Boolean): Result<Unit>
    suspend fun deleteCategory(id: String): Result<Unit>
    suspend fun seedDefaultCategories(): Result<Unit>
}
