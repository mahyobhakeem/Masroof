package com.masroofi.app.utils

import java.util.Locale

/**
 * أدوات مساعدة لتنسيق الأرقام والعملات والتواريخ وفق معايير التطبيق الصارمة
 */
object FormatUtils {

    /**
     * تنسيق المبالغ المالية بالريال اليمني (YER) كأرقام صحيحة خالية من الكسور
     */
    fun formatCurrency(amount: Long, includeUnit: Boolean = true): String {
        val formatted = String.format(Locale.getDefault(), "%,d", amount)
        return if (includeUnit) "$formatted ريال" else formatted
    }

    /**
     * تنسيق النسبة المئوية
     */
    fun formatPercentage(percentage: Int): String {
        return "$percentage%"
    }
}

/**
 * دالة مساعدة عامة لتنسيق الأرقام مع فواصل الآلاف
 */
fun formatNumber(number: Long): String {
    return String.format(Locale.getDefault(), "%,d", number)
}
