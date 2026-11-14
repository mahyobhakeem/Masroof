package com.masroofi.app.data.database

import com.masroofi.app.data.entity.CategoryEntity
import com.masroofi.app.domain.model.TransactionType

/**
 * التصنيفات المالية الافتراضية المعتمدة لتطبيق مصروفي
 * تستخدم معرفات ثابتة لتفادي التكرار عند التهيئة
 */
object DefaultCategories {

    val expenseCategories = listOf(
        CategoryEntity(
            id = "cat_exp_food",
            name = "الطعام والمشروبات",
            type = TransactionType.EXPENSE,
            iconKey = "fastfood",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_exp_transport",
            name = "المواصلات",
            type = TransactionType.EXPENSE,
            iconKey = "directions_car",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_exp_shopping",
            name = "التسوق",
            type = TransactionType.EXPENSE,
            iconKey = "shopping_bag",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_exp_telecom",
            name = "الاتصالات والإنترنت",
            type = TransactionType.EXPENSE,
            iconKey = "wifi",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_exp_bills",
            name = "الفواتير",
            type = TransactionType.EXPENSE,
            iconKey = "receipt",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_exp_housing",
            name = "السكن",
            type = TransactionType.EXPENSE,
            iconKey = "home",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_exp_health",
            name = "الصحة",
            type = TransactionType.EXPENSE,
            iconKey = "local_hospital",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_exp_education",
            name = "التعليم",
            type = TransactionType.EXPENSE,
            iconKey = "school",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_exp_entertainment",
            name = "الترفيه",
            type = TransactionType.EXPENSE,
            iconKey = "sports_esports",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_exp_other",
            name = "مصروفات أخرى",
            type = TransactionType.EXPENSE,
            iconKey = "more_horiz",
            isDefault = true,
            isActive = true
        )
    )

    val incomeCategories = listOf(
        CategoryEntity(
            id = "cat_inc_opening_balance",
            name = "الرصيد الافتتاحي / المحفظة",
            type = TransactionType.INCOME,
            iconKey = "account_balance_wallet",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_inc_salary",
            name = "الراتب",
            type = TransactionType.INCOME,
            iconKey = "payments",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_inc_overtime",
            name = "العمل الإضافي",
            type = TransactionType.INCOME,
            iconKey = "more_time",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_inc_freelance",
            name = "التجارة والعمل الحر",
            type = TransactionType.INCOME,
            iconKey = "trending_up",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_inc_gifts",
            name = "الهدايا",
            type = TransactionType.INCOME,
            iconKey = "card_giftcard",
            isDefault = true,
            isActive = true
        ),
        CategoryEntity(
            id = "cat_inc_other",
            name = "إيرادات أخرى",
            type = TransactionType.INCOME,
            iconKey = "savings",
            isDefault = true,
            isActive = true
        )
    )

    val allDefaultCategories: List<CategoryEntity> = expenseCategories + incomeCategories
}
