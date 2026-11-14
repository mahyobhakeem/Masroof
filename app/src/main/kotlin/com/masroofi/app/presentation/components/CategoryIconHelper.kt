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
            "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
            else -> Icons.Default.Category
        }
    }

    /**
     * لون أيقونات التصنيفات الموحد باللون الأخضر الزمردي لمنع التشتت البصري
     * يضمن استخدام PrimaryGreen (#16A67A) في الوضع الفاتح و (#35B98A) في الوضع الداكن
     */
    fun getColor(iconKey: String? = null): Color {
        return EmeraldPrimary
    }

    fun getColor(isDarkTheme: Boolean): Color {
        return if (isDarkTheme) EmeraldDark else EmeraldPrimary
    }

    /**
     * لون الخلفية الدائرية الناعمة الموحدة لأيقونات التصنيفات
     * (#E6F4EF في الوضع الفاتح، و #16A67A بشفافية 18% في الوضع الداكن)
     */
    fun getBackgroundColor(isDarkTheme: Boolean = false): Color {
        return if (isDarkTheme) EmeraldDarkBg else EmeraldLightBg
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
        "account_balance_wallet",
        "more_time",
        "trending_up",
        "card_giftcard",
        "savings",
        "more_horiz"
    )
}
