package com.masroofi.app.data

import com.masroofi.app.data.entity.CategoryEntity
import com.masroofi.app.data.entity.MonthlyBudgetEntity
import com.masroofi.app.data.entity.TransactionEntity
import com.masroofi.app.data.entity.TransactionWithCategory
import com.masroofi.app.data.repository.toDomain
import com.masroofi.app.data.repository.toEntity
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.MonthlyBudget
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

/**
 * اختبارات التحقق من دوال التحويل بين الكيانات الأولية ونماذج نطاق العمل (Mappers)
 */
class RepositoryMappingTest {

    @Test
    fun categoryMapper_convertsBothWaysCorrectly() {
        val originalEntity = CategoryEntity(
            id = "cat_test_1",
            name = "تصنيف مخصص",
            type = TransactionType.EXPENSE,
            iconKey = "fastfood",
            isDefault = false,
            isActive = true
        )

        val domainModel = originalEntity.toDomain()
        assertEquals(originalEntity.id, domainModel.id)
        assertEquals(originalEntity.name, domainModel.name)
        assertEquals(originalEntity.type, domainModel.type)
        assertEquals(originalEntity.iconKey, domainModel.iconKey)
        assertEquals(originalEntity.isDefault, domainModel.isDefault)
        assertEquals(originalEntity.isActive, domainModel.isActive)

        val backToEntity = domainModel.toEntity()
        assertEquals(originalEntity, backToEntity)
    }

    @Test
    fun transactionMapper_convertsBothWaysCorrectly() {
        val originalEntity = TransactionEntity(
            id = "tx_123",
            type = TransactionType.INCOME,
            amount = 95_000L,
            categoryId = "cat_inc_salary",
            transactionDate = "2026-11-01",
            note = "راتب شهر نوفمبر",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val domainItem = originalEntity.toDomain(categoryName = "الراتب", categoryIconKey = "payments")
        assertEquals(originalEntity.id, domainItem.id)
        assertEquals(originalEntity.type, domainItem.type)
        assertEquals(originalEntity.amount, domainItem.amount)
        assertEquals(originalEntity.categoryId, domainItem.categoryId)
        assertEquals("الراتب", domainItem.categoryName)
        assertEquals("payments", domainItem.categoryIconKey)
        assertEquals(originalEntity.transactionDate, domainItem.transactionDate)
        assertEquals(originalEntity.note, domainItem.note)

        val backToEntity = domainItem.toEntity()
        assertEquals(originalEntity, backToEntity)
    }

    @Test
    fun transactionWithCategory_convertsToDomainCorrectly() {
        val txEntity = TransactionEntity(
            id = "tx_joined",
            type = TransactionType.EXPENSE,
            amount = 3_500L,
            categoryId = "cat_exp_food",
            transactionDate = "2026-11-10",
            note = "وجبة عشاء"
        )
        val catEntity = CategoryEntity(
            id = "cat_exp_food",
            name = "الطعام والمشروبات",
            type = TransactionType.EXPENSE,
            iconKey = "fastfood",
            isDefault = true,
            isActive = true
        )

        val joined = TransactionWithCategory(transaction = txEntity, category = catEntity)
        val domain = joined.toDomain()

        assertEquals("tx_joined", domain.id)
        assertEquals(3_500L, domain.amount)
        assertEquals("cat_exp_food", domain.categoryId)
        assertEquals("الطعام والمشروبات", domain.categoryName)
        assertEquals("fastfood", domain.categoryIconKey)
    }

    @Test
    fun monthlyBudgetMapper_convertsBothWaysCorrectly() {
        val budgetEntity = MonthlyBudgetEntity(
            id = "mb_2026_11",
            yearMonth = "2026-11",
            amount = 150_000L
        )

        val domain = budgetEntity.toDomain()
        assertEquals("mb_2026_11", domain.id)
        assertEquals("2026-11", domain.yearMonth)
        assertEquals(150_000L, domain.amount)

        val backToEntity = domain.toEntity()
        assertEquals(budgetEntity, backToEntity)
    }
}
