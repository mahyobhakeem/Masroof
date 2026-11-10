package com.masroofi.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.masroofi.app.presentation.theme.*

/**
 * أداة مساعدة لربط مفاتيح الأيقونات النصية (iconKey) بأيقونات Material الرسمية والألوان المعتمدة
 */
object CategoryIconHelper {

    fun getIcon(iconKey: String?): ImageVector {
        return when (iconKey) {
            "fastfood" -> Icons.Default.Fastfood
            "directions_car" -> Icons.Default.DirectionsCar
            "shopping_bag" -> Icons.Default.ShoppingBag
            "wifi" -> Icons.Default.Wifi
            "receipt" -> Icons.Default.Receipt
            "home" -> Icons.Default.Home
            "local_hospital" -> Icons.Default.LocalHospital
            "school" -> Icons.Default.School
            "sports_esports" -> Icons.Default.SportsEsports
            "more_horiz" -> Icons.Default.MoreHoriz
            "payments" -> Icons.Default.Payments
            "more_time" -> Icons.Default.MoreTime
            "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
            "card_giftcard" -> Icons.Default.CardGiftcard
            "savings" -> Icons.Default.Savings
            else -> Icons.Default.Category
        }
    }

    fun getColor(iconKey: String?): Color {
        return when (iconKey) {
            "fastfood" -> CategoryFood
            "directions_car" -> CategoryTransport
            "shopping_bag" -> CategoryShopping
            "wifi" -> CategoryBills
            "receipt" -> CategoryBills
            "home" -> CategoryTransport
            "local_hospital" -> CategoryHealth
            "school" -> CategoryTransport
            "sports_esports" -> CategoryEntertainment
            "more_horiz" -> TextSecondary
            "payments" -> IncomeGreen
            "more_time" -> IncomeGreen
            "trending_up" -> PrimaryGreen
            "card_giftcard" -> IncomeGreen
            "savings" -> IncomeGreen
            else -> PrimaryGreen
        }
    }

    /**
     * قائمة مفاتيح الأيقونات المتاحة للاختيار عند إضافة أو تعديل تصنيف مالي
     */
    val selectableIcons: List<String> = listOf(
        "fastfood",
        "directions_car",
        "shopping_bag",
        "wifi",
        "receipt",
        "home",
        "local_hospital",
        "school",
        "sports_esports",
        "payments",
        "more_time",
        "trending_up",
        "card_giftcard",
        "savings",
        "more_horiz"
    )
}
