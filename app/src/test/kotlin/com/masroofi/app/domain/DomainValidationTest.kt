package com.masroofi.app.domain

import com.masroofi.app.data.database.DefaultCategories
import com.masroofi.app.data.entity.CategoryEntity
import com.masroofi.app.data.entity.MonthlyBudgetEntity
import com.masroofi.app.data.entity.TransactionEntity
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * اختبارات التحقق من صحة القواعد المالية والنماذج (Domain Validation Tests)
 */
class DomainValidationTest {

    // 1. التحقق من التصنيفات الافتراضية
    @Test
    fun defaultCategories_hasCorrectCountAndNoDuplicates() {
        val expenseCats = DefaultCategories.expenseCategories
        val incomeCats = DefaultCategories.incomeCategories
        val allCats = DefaultCategories.allDefaultCategories

        // 10 تصنيفات للمصروفات و 5 للإيرادات
        assertEquals("يجب أن يحتوي قسم المصروفات على 10 تصنيفات افتراضية بالضبط", 10, expenseCats.size)
        assertEquals("يجب أن يحتوي قسم الإيرادات على 5 تصنيفات افتراضية بالضبط", 5, incomeCats.size)
        assertEquals("إجمالي التصنيفات الافتراضية يجب أن يكون 15 تصنيفاً", 15, allCats.size)

        // التحقق من عدم وجود أي تكرار في المعرفات الثابتة
        val distinctIds = allCats.map { it.id }.distinct()
        assertEquals("يجب أن تكون جميع معرفات التصنيفات الافتراضية فريدة وغير مكررة", allCats.size, distinctIds.size)

        // التحقق من أن جميع التصنيفات الافتراضية مفعلة وبحالة افتراضية
        assertTrue("جميع التصنيفات الافتراضية يجب أن تكون isDefault = true", allCats.all { it.isDefault })
        assertTrue("جميع التصنيفات الافتراضية يجب أن تكون isActive = true", allCats.all { it.isActive })

        // التحقق من نوع كل تصنيف
        assertTrue(expenseCats.all { it.type == TransactionType.EXPENSE })
        assertTrue(incomeCats.all { it.type == TransactionType.INCOME })
    }

    // 2. التحقق من أسماء تصنيفات المصروفات العشرة المعتمدة
    @Test
    fun defaultExpenseCategories_containAllRequiredNames() {
        val names = DefaultCategories.expenseCategories.map { it.name }
        val requiredNames = listOf(
            "الطعام والمشروبات",
            "المواصلات",
            "التسوق",
            "الاتصالات والإنترنت",
            "الفواتير",
            "السكن",
            "الصحة",
            "التعليم",
            "الترفيه",
            "مصروفات أخرى"
        )
        for (required in requiredNames) {
            assertTrue("يجب أن يتضمن تصنيف المصروفات: $required", names.contains(required))
        }
    }

    // 3. التحقق من أسماء تصنيفات الإيرادات الخمسة المعتمدة
    @Test
    fun defaultIncomeCategories_containAllRequiredNames() {
        val names = DefaultCategories.incomeCategories.map { it.name }
        val requiredNames = listOf(
            "الراتب",
            "العمل الإضافي",
            "التجارة والعمل الحر",
            "الهدايا",
            "إيرادات أخرى"
        )
        for (required in requiredNames) {
            assertTrue("يجب أن يتضمن تصنيف الإيرادات: $required", names.contains(required))
        }
    }

    // 4. رفض المبالغ الصفرية للعملية المالية
    @Test(expected = IllegalArgumentException::class)
    fun transactionEntity_rejectsZeroAmount() {
        TransactionEntity(
            id = "test_0",
            type = TransactionType.EXPENSE,
            amount = 0L,
            categoryId = "cat_exp_food",
            transactionDate = "2026-11-10"
        )
    }

    // 5. رفض المبالغ السالبة للعملية المالية
    @Test(expected = IllegalArgumentException::class)
    fun transactionEntity_rejectsNegativeAmount() {
        TransactionEntity(
            id = "test_neg",
            type = TransactionType.EXPENSE,
            amount = -500L,
            categoryId = "cat_exp_food",
            transactionDate = "2026-11-10"
        )
    }

    // 6. قبول المبالغ الموجبة فقط للعملية المالية
    @Test
    fun transactionEntity_acceptsPositiveAmount() {
        val entity = TransactionEntity(
            id = "test_pos",
            type = TransactionType.EXPENSE,
            amount = 1500L,
            categoryId = "cat_exp_food",
            transactionDate = "2026-11-10"
        )
        assertEquals(1500L, entity.amount)
    }

    // 7. رفض المبالغ الصفرية أو السالبة في نموذج نطاق العمل TransactionItem
    @Test(expected = IllegalArgumentException::class)
    fun transactionItem_rejectsNonPositiveAmount() {
        TransactionItem(
            id = "item_neg",
            type = TransactionType.INCOME,
            amount = -100L,
            categoryId = "cat_inc_salary",
            transactionDate = "2026-11-10"
        )
    }

    // 8. رفض الميزانية الشهرية السالبة
    @Test(expected = IllegalArgumentException::class)
    fun monthlyBudgetEntity_rejectsNegativeAmount() {
        MonthlyBudgetEntity(
            id = "b1",
            yearMonth = "2026-11",
            amount = -1L
        )
    }

    // 9. قبول الميزانية الشهرية الصفرية أو الموجبة
    @Test
    fun monthlyBudgetEntity_acceptsZeroOrPositiveAmount() {
        val zeroBudget = MonthlyBudgetEntity(id = "b0", yearMonth = "2026-11", amount = 0L)
        val positiveBudget = MonthlyBudgetEntity(id = "b1", yearMonth = "2026-12", amount = 120_000L)
        assertEquals(0L, zeroBudget.amount)
        assertEquals(120_000L, positiveBudget.amount)
    }

    // 10. التحقق من تحويل التاريخ ISO إلى LocalDate
    @Test
    fun transactionItem_parsesIsoDateCorrectly() {
        val item = TransactionItem(
            id = "item_1",
            type = TransactionType.EXPENSE,
            amount = 5000L,
            categoryId = "cat_exp_bills",
            transactionDate = "2026-11-10"
        )
        val date = item.toLocalDate()
        assertEquals(LocalDate.of(2026, 11, 10), date)
    }
}
