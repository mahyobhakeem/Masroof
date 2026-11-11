package com.masroofi.app.domain.model

/**
 * أوضاع الاستعراض والتحليل الزمني الأربعة لتطبيق مصروف في الشاشة الرئيسية
 */
enum class TimePeriodMode(val title: String) {
    DAILY("اليوم"),
    WEEKLY("الأسبوع"),
    MONTHLY("الشهر"),
    CUSTOM_RANGE("فترة مخصصة")
}

/**
 * أوضاع التحليل الزمني المرن في شاشة التقارير والإحصائيات
 */
enum class ReportPeriodMode(val title: String) {
    MONTHLY("شهري"),
    WEEKLY("أسبوعي"),
    SPECIFIC_DAY("يوم محدد"),
    CUSTOM_RANGE("فترة مخصصة")
}

