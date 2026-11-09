package com.masroofi.app.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.TrendingUp

/**
 * نموذج نطاق العمل للعملية المالية (Transaction Domain Model)
 */
data class Transaction(
    val id: String,
    val title: String,
    val amount: Long, // بالريال اليمني دون كسور عشرية
    val category: String,
    val categoryIcon: ImageVector,
    val categoryColor: Color,
    val date: String,
    val isExpense: Boolean,
    val notes: String = ""
)

/**
 * نموذج نطاق العمل للتصنيف المالي (Category Domain Model)
 */
data class CategoryItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val totalSpent: Long = 0,
    val percentage: Int = 0
)

/**
 * بيانات تجريبية متناسقة وموحدة للتطبيق
 */
object MockData {
    const val MONTHLY_BUDGET: Long = 100_000
    const val TOTAL_EXPENSES: Long = 35_000
    const val REMAINING_BUDGET: Long = 65_000
    const val CONSUMPTION_PERCENTAGE: Int = 35
    const val TOTAL_INCOME: Long = 80_000
    const val NET_FLOW: Long = 45_000
    const val TODAY_EXPENSES: Long = 2_500

    val topCategories = listOf(
        CategoryItem("1", "طعام ومشروبات", Icons.Default.Fastfood, Color(0xFFF79009), 15_000, 43),
        CategoryItem("2", "فواتير وخدمات", Icons.Default.Receipt, Color(0xFF2E90FA), 10_000, 29),
        CategoryItem("3", "مواصلات ووقود", Icons.Default.DirectionsCar, Color(0xFF7A5AF8), 6_000, 17),
        CategoryItem("4", "تسوق ومستلزمات", Icons.Default.ShoppingBag, Color(0xFFEE46BC), 4_000, 11)
    )

    val allCategories = listOf(
        CategoryItem("1", "طعام ومشروبات", Icons.Default.Fastfood, Color(0xFFF79009)),
        CategoryItem("2", "فواتير وخدمات", Icons.Default.Receipt, Color(0xFF2E90FA)),
        CategoryItem("3", "مواصلات ووقود", Icons.Default.DirectionsCar, Color(0xFF7A5AF8)),
        CategoryItem("4", "تسوق ومستلزمات", Icons.Default.ShoppingBag, Color(0xFFEE46BC)),
        CategoryItem("5", "صحة وعلاج", Icons.Default.LocalHospital, Color(0xFF12B76A)),
        CategoryItem("6", "راتب شهري", Icons.Default.Payments, Color(0xFF35B98A)),
        CategoryItem("7", "أرباح واستثمار", Icons.AutoMirrored.Filled.TrendingUp, Color(0xFF16A67A))
    )

    val transactionsList = listOf(
        Transaction(
            id = "t1",
            title = "غداء مطعم الشيباني",
            amount = 2_500,
            category = "طعام ومشروبات",
            categoryIcon = Icons.Default.Fastfood,
            categoryColor = Color(0xFFF79009),
            date = "اليوم، 9 نوفمبر",
            isExpense = true,
            notes = "وجبة غداء عمل"
        ),
        Transaction(
            id = "t2",
            title = "فاتورة الإنترنت المنزلي",
            amount = 6_000,
            category = "فواتير وخدمات",
            categoryIcon = Icons.Default.Receipt,
            categoryColor = Color(0xFF2E90FA),
            date = "أمس، 8 نوفمبر",
            isExpense = true,
            notes = "اشتراك باقة يمن نت"
        ),
        Transaction(
            id = "t3",
            title = "دفعة مشروع حر",
            amount = 80_000,
            category = "أرباح واستثمار",
            categoryIcon = Icons.AutoMirrored.Filled.TrendingUp,
            categoryColor = Color(0xFF35B98A),
            date = "7 نوفمبر 2026",
            isExpense = false,
            notes = "تسليم المرحلة الأولى"
        ),
        Transaction(
            id = "t4",
            title = "تعبئة وقود للسيارة",
            amount = 6_000,
            category = "مواصلات ووقود",
            categoryIcon = Icons.Default.DirectionsCar,
            categoryColor = Color(0xFF7A5AF8),
            date = "5 نوفمبر 2026",
            isExpense = true,
            notes = "20 لتر بترول"
        ),
        Transaction(
            id = "t5",
            title = "مشتريات بقالة المنزل",
            amount = 12_500,
            category = "طعام ومشروبات",
            categoryIcon = Icons.Default.Fastfood,
            categoryColor = Color(0xFFF79009),
            date = "3 نوفمبر 2026",
            isExpense = true,
            notes = "أغذية وتموينات أسبوعية"
        ),
        Transaction(
            id = "t6",
            title = "فاتورة الكهرباء والماء",
            amount = 4_000,
            category = "فواتير وخدمات",
            categoryIcon = Icons.Default.Receipt,
            categoryColor = Color(0xFF2E90FA),
            date = "2 نوفمبر 2026",
            isExpense = true,
            notes = "سداد عبر تطبيق بنكي"
        ),
        Transaction(
            id = "t7",
            title = "أدوات ومستلزمات مكتبية",
            amount = 4_000,
            category = "تسوق ومستلزمات",
            categoryIcon = Icons.Default.ShoppingBag,
            categoryColor = Color(0xFFEE46BC),
            date = "1 نوفمبر 2026",
            isExpense = true,
            notes = "أوراق وأقلام ومذكرات"
        )
    )
}
