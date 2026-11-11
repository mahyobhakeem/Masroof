package com.masroofi.app.data.dao

import androidx.room.*
import com.masroofi.app.data.entity.CategoryEntity
import com.masroofi.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * واجهة الاستعلام والتعامل مع جدول التصنيفات المالية (CategoryDao)
 */
@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories ORDER BY CASE WHEN id LIKE '%_other' THEN 1 ELSE 0 END ASC, isDefault DESC, name ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY CASE WHEN id LIKE '%_other' THEN 1 ELSE 0 END ASC, isDefault DESC, name ASC")
    fun getActiveCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isActive = 1 AND type = :type ORDER BY CASE WHEN id LIKE '%_other' THEN 1 ELSE 0 END ASC, isDefault DESC, name ASC")
    fun getActiveCategoriesByTypeFlow(type: TransactionType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    fun getCategoryByIdFlow(id: String): Flow<CategoryEntity?>

    @Query("UPDATE categories SET isActive = :isActive WHERE id = :id")
    suspend fun setCategoryActive(id: String, isActive: Boolean): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsForCategory(categoryId: String): Int

    @Delete
    suspend fun deleteCategory(category: CategoryEntity): Int

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String): Int

    @Query("SELECT * FROM categories ORDER BY CASE WHEN id LIKE '%_other' THEN 1 ELSE 0 END ASC, isDefault DESC, name ASC")
    suspend fun getAllCategoryEntities(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceCategoryEntities(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories(): Int
}
