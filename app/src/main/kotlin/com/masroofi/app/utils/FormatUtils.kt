package com.masroofi.app.utils

import java.util.Locale

/**
 * أدوات مساعدة لتنسيق الأرقام والعملات والتواريخ وفق معايير التطبيق الصارمة
 */
object FormatUtils {

    /**
     * تنسيق المبالغ المالية بالريال اليمني (YER) كأرقام صحيحة خالية من الكسور وبالأرقام القياسية الموحدة (0-9)
     */
    fun formatCurrency(amount: Long, includeUnit: Boolean = true): String {
        val formatted = String.format(Locale.US, "%,d", amount)
        return if (includeUnit) "$formatted ريال" else formatted
    }

    /**
     * تنسيق النسبة المئوية
     */
    fun formatPercentage(percentage: Int): String {
        return "$percentage%"
    }

    /**
     * تنسيق الشهر والسنة باللغة العربية الفصحى
     */
    fun formatYearMonth(yearMonth: java.time.YearMonth): String {
        val monthName = when (yearMonth.monthValue) {
            1 -> "يناير"
            2 -> "فبراير"
            3 -> "مارس"
            4 -> "أبريل"
            5 -> "مايو"
            6 -> "يونيو"
            7 -> "يوليو"
            8 -> "أغسطس"
            9 -> "سبتمبر"
            10 -> "أكتوبر"
            11 -> "نوفمبر"
            12 -> "ديسمبر"
            else -> yearMonth.month.name
        }
        return "$monthName ${yearMonth.year}"
    }
}

/**
 * دالة مساعدة عامة لتنسيق الأرقام القياسية (0-9) مع فواصل الآلاف
 */
fun formatNumber(number: Long): String {
    return String.format(Locale.US, "%,d", number)
}

/**
 * دالة مساعدة عامة لتنسيق الشهر والسنة
 */
fun formatYearMonth(yearMonth: java.time.YearMonth): String {
    return FormatUtils.formatYearMonth(yearMonth)
}
