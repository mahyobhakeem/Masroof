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

    companion object {
        // ترتيب الأولوية المنطقية للتصنيفات لضمان ظهور التصنيفات الأساسية أولاً وتصنيف "أخرى" في النهاية دائماً
        val categoryPriorityMap = mapOf(
            // تصنيفات المصروفات
            "cat_exp_food" to 1,
            "cat_exp_transport" to 2,
            "cat_exp_shopping" to 3,
            "cat_exp_bills" to 4,
            "cat_exp_telecom" to 5,
            "cat_exp_housing" to 6,
            "cat_exp_health" to 7,
            "cat_exp_education" to 8,
            "cat_exp_entertainment" to 9,
            "cat_exp_other" to 998,
            // تصنيفات الإيرادات
            "cat_inc_salary" to 101,
            "cat_inc_overtime" to 102,
            "cat_inc_freelance" to 103,
            "cat_inc_gifts" to 104,
            "cat_inc_other" to 999
        )

        fun sortCategoriesLogically(categories: List<Category>): List<Category> {
            return categories.sortedWith(
                compareBy(
                    { categoryPriorityMap[it.id] ?: 500 },
                    { it.name }
                )
            )
        }
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategoriesFlow().map { list ->
            sortCategoriesLogically(list.map { it.toDomain() })
        }
    }

    override fun getActiveCategories(type: TransactionType?): Flow<List<Category>> {
        val flow = if (type != null) {
            categoryDao.getActiveCategoriesByTypeFlow(type)
        } else {
            categoryDao.getActiveCategoriesFlow()
        }
        return flow.map { list ->
            sortCategoriesLogically(list.map { it.toDomain() })
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

    override suspend fun getTransactionCountForCategory(id: String): Int {
        return categoryDao.countTransactionsForCategory(id)
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
