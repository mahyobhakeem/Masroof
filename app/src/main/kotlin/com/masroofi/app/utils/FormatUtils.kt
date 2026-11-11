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
     * اسم الشهر العربي بمفرده
     */
    fun formatMonthName(monthValue: Int): String {
        return when (monthValue) {
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
            else -> ""
        }
    }

    /**
     * تنسيق الشهر والسنة باللغة العربية الفصحى
     */
    fun formatYearMonth(yearMonth: java.time.YearMonth): String {
        val monthName = formatMonthName(yearMonth.monthValue)
        return "$monthName ${yearMonth.year}"
    }

    /**
     * اسم يوم الأسبوع باللغة العربية
     */
    fun formatArabicDayOfWeek(dayOfWeek: java.time.DayOfWeek): String {
        return when (dayOfWeek) {
            java.time.DayOfWeek.SATURDAY -> "السبت"
            java.time.DayOfWeek.SUNDAY -> "الأحد"
            java.time.DayOfWeek.MONDAY -> "الإثنين"
            java.time.DayOfWeek.TUESDAY -> "الثلاثاء"
            java.time.DayOfWeek.WEDNESDAY -> "الأربعاء"
            java.time.DayOfWeek.THURSDAY -> "الخميس"
            java.time.DayOfWeek.FRIDAY -> "الجمعة"
        }
    }

    fun formatArabicDayOfWeek(date: java.time.LocalDate): String = formatArabicDayOfWeek(date.dayOfWeek)

    /**
     * تنسيق التاريخ باللغة العربية الفصحى
     */
    fun formatArabicDate(date: java.time.LocalDate, includeDayOfWeek: Boolean = false): String {
        val monthName = formatMonthName(date.monthValue)
        val dayNumber = date.dayOfMonth
        val year = date.year
        return if (includeDayOfWeek) {
            val dayName = formatArabicDayOfWeek(date.dayOfWeek)
            "$dayName، $dayNumber $monthName $year"
        } else {
            "$dayNumber $monthName $year"
        }
    }

    /**
     * تنسيق نطاق زمني بين تاريخين باللغة العربية
     */
    fun formatDateRange(startDate: java.time.LocalDate, endDate: java.time.LocalDate): String {
        if (startDate == endDate) {
            return formatArabicDate(startDate)
        }
        val startMonth = formatMonthName(startDate.monthValue)
        val endMonth = formatMonthName(endDate.monthValue)
        return if (startDate.year == endDate.year && startDate.monthValue == endDate.monthValue) {
            "${startDate.dayOfMonth} - ${endDate.dayOfMonth} $startMonth ${startDate.year}"
        } else if (startDate.year == endDate.year) {
            "${startDate.dayOfMonth} $startMonth - ${endDate.dayOfMonth} $endMonth ${startDate.year}"
        } else {
            "${startDate.dayOfMonth} $startMonth ${startDate.year} - ${endDate.dayOfMonth} $endMonth ${endDate.year}"
        }
    }

    /**
     * حساب بداية ونهاية الأسبوع بحيث يبدأ من السبت وينتهي بالجمعة
     */
    fun getWeekRangeSaturday(date: java.time.LocalDate): Pair<java.time.LocalDate, java.time.LocalDate> {
        val dow = date.dayOfWeek.value // 1 (الإثنين) .. 6 (السبت), 7 (الأحد)
        val daysFromSaturday = when (dow) {
            6 -> 0
            7 -> 1
            else -> dow + 1
        }
        val startOfWeek = date.minusDays(daysFromSaturday.toLong())
        val endOfWeek = startOfWeek.plusDays(6)
        return Pair(startOfWeek, endOfWeek)
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
