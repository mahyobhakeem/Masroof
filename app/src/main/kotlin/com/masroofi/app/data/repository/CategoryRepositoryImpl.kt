package com.masroofi.app.data.repository

import com.masroofi.app.data.dao.CategoryDao
import com.masroofi.app.data.database.DefaultCategories
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * التنفيذ الفعلي لمستودع التصنيفات المالية في طبقة البيانات
 */
class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategoriesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getActiveCategories(type: TransactionType?): Flow<List<Category>> {
        return if (type != null) {
            categoryDao.getActiveCategoriesByTypeFlow(type).map { list ->
                list.map { it.toDomain() }
            }
        } else {
            categoryDao.getActiveCategoriesFlow().map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    override fun getCategoryById(id: String): Flow<Category?> {
        return categoryDao.getCategoryByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun addCategory(category: Category): Result<Unit> = runCatching {
        categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category): Result<Unit> = runCatching {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun setCategoryActive(id: String, isActive: Boolean): Result<Unit> = runCatching {
        categoryDao.setCategoryActive(id, isActive)
    }

    override suspend fun deleteCategory(id: String): Result<Unit> = runCatching {
        val count = categoryDao.countTransactionsForCategory(id)
        if (count > 0) {
            throw IllegalStateException("لا يمكن حذف تصنيف مرتبط بعمليات مالية سابقة. يمكنك إلغاء تنشيط التصنيف بدلاً من حذفه.")
        }
        categoryDao.deleteCategoryById(id)
    }

    override suspend fun seedDefaultCategories(): Result<Unit> = runCatching {
        categoryDao.insertCategories(DefaultCategories.allDefaultCategories)
    }
}
