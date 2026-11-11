package com.masroofi.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * بيانات العملة المالية وفق معيار ISO 4217
 *
 * @param code رمز العملة الدولي المكون من 3 أحرف (ISO 4217) مثل YER أو SAR
 * @param nameArabic الاسم الكامل للعملة باللغة العربية الفصحى
 * @param symbolArabic الرمز المختصر أو علامة العملة المستخدمة في العرض اليومي
 * @param countryArabic اسم الدولة أو النطاق الجغرافي باللغة العربية
 * @param flagEmoji رمز تعبيري لعلم الدولة لتسهيل التمييز البصري
 */
@Immutable
data class CurrencyInfo(
    val code: String,
    val nameArabic: String,
    val symbolArabic: String,
    val countryArabic: String,
    val flagEmoji: String = ""
) {
    /**
     * النص المعروض بشكل مدمج وأنيق في القوائم والبطاقات
     * مثال: الريال اليمني (YER - ر.ي)
     */
    val displayTitle: String
        get() = "$nameArabic ($code - $symbolArabic)"
}

/**
 * الدليل المركزي لعملات الدول العربية والعملات الدولية الشائعة (ISO 4217)
 * يضم 24 عملة معتمدة تغطي كامل العالم العربي والدولار واليورو
 */
object CurrencyCatalog {

    const val DEFAULT_CURRENCY_CODE = "YER"

    val allCurrencies: List<CurrencyInfo> = listOf(
        // 1. اليمن
        CurrencyInfo(
            code = "YER",
            nameArabic = "الريال اليمني",
            symbolArabic = "ر.ي",
            countryArabic = "اليمن",
            flagEmoji = "🇾🇪"
        ),
        // 2. السعودية
        CurrencyInfo(
            code = "SAR",
            nameArabic = "الريال السعودي",
            symbolArabic = "ر.س",
            countryArabic = "المملكة العربية السعودية",
            flagEmoji = "🇸🇦"
        ),
        // 3. الإمارات
        CurrencyInfo(
            code = "AED",
            nameArabic = "الدرهم الإماراتي",
            symbolArabic = "د.إ",
            countryArabic = "الإمارات العربية المتحدة",
            flagEmoji = "🇦🇪"
        ),
        // 4. الكويت
        CurrencyInfo(
            code = "KWD",
            nameArabic = "الدينار الكويتي",
            symbolArabic = "د.ك",
            countryArabic = "الكويت",
            flagEmoji = "🇰🇼"
        ),
        // 5. البحرين
        CurrencyInfo(
            code = "BHD",
            nameArabic = "الدينار البحريني",
            symbolArabic = "د.ب",
            countryArabic = "البحرين",
            flagEmoji = "🇧🇭"
        ),
        // 6. قطر
        CurrencyInfo(
            code = "QAR",
            nameArabic = "الريال القطري",
            symbolArabic = "ر.ق",
            countryArabic = "قطر",
            flagEmoji = "🇶🇦"
        ),
        // 7. عُمان
        CurrencyInfo(
            code = "OMR",
            nameArabic = "الريال العُماني",
            symbolArabic = "ر.ع",
            countryArabic = "سلطنة عُمان",
            flagEmoji = "🇴🇲"
        ),
        // 8. مصر
        CurrencyInfo(
            code = "EGP",
            nameArabic = "الجنيه المصري",
            symbolArabic = "ج.م",
            countryArabic = "مصر",
            flagEmoji = "🇪🇬"
        ),
        // 9. الأردن / فلسطين
        CurrencyInfo(
            code = "JOD",
            nameArabic = "الدينار الأردني",
            symbolArabic = "د.أ",
            countryArabic = "الأردن / فلسطين",
            flagEmoji = "🇯🇴"
        ),
        // 10. العراق
        CurrencyInfo(
            code = "IQD",
            nameArabic = "الدينار العراقي",
            symbolArabic = "د.ع",
            countryArabic = "العراق",
            flagEmoji = "🇮🇶"
        ),
        // 11. سوريا
        CurrencyInfo(
            code = "SYP",
            nameArabic = "الليرة السورية",
            symbolArabic = "ل.س",
            countryArabic = "سوريا",
            flagEmoji = "🇸🇾"
        ),
        // 12. لبنان
        CurrencyInfo(
            code = "LBP",
            nameArabic = "الليرة اللبنانية",
            symbolArabic = "ل.ل",
            countryArabic = "لبنان",
            flagEmoji = "🇱🇧"
        ),
        // 13. فلسطين (الشيكل المتداول بالإضافة إلى الدينار الأردني والدولار)
        CurrencyInfo(
            code = "ILS",
            nameArabic = "الشيكل",
            symbolArabic = "₪",
            countryArabic = "فلسطين",
            flagEmoji = "🇵🇸"
        ),
        // 14. المغرب
        CurrencyInfo(
            code = "MAD",
            nameArabic = "الدرهم المغربي",
            symbolArabic = "د.م",
            countryArabic = "المغرب",
            flagEmoji = "🇲🇦"
        ),
        // 15. الجزائر
        CurrencyInfo(
            code = "DZD",
            nameArabic = "الدينار الجزائري",
            symbolArabic = "د.ج",
            countryArabic = "الجزائر",
            flagEmoji = "🇩🇿"
        ),
        // 16. تونس
        CurrencyInfo(
            code = "TND",
            nameArabic = "الدينار التونسي",
            symbolArabic = "د.ت",
            countryArabic = "تونس",
            flagEmoji = "🇹🇳"
        ),
        // 17. ليبيا
        CurrencyInfo(
            code = "LYD",
            nameArabic = "الدينار الليبي",
            symbolArabic = "د.ل",
            countryArabic = "ليبيا",
            flagEmoji = "🇱🇾"
        ),
        // 18. السودان
        CurrencyInfo(
            code = "SDG",
            nameArabic = "الجنيه السوداني",
            symbolArabic = "ج.س",
            countryArabic = "السودان",
            flagEmoji = "🇸🇩"
        ),
        // 19. موريتانيا
        CurrencyInfo(
            code = "MRU",
            nameArabic = "الأوقية الموريتانية",
            symbolArabic = "أ.م",
            countryArabic = "موريتانيا",
            flagEmoji = "🇲🇷"
        ),
        // 20. الصومال
        CurrencyInfo(
            code = "SOS",
            nameArabic = "الشلن الصومالي",
            symbolArabic = "ش.ص",
            countryArabic = "الصومال",
            flagEmoji = "🇸🇴"
        ),
        // 21. جيبوتي
        CurrencyInfo(
            code = "DJF",
            nameArabic = "الفرنك الجيبوتي",
            symbolArabic = "ف.ج",
            countryArabic = "جيبوتي",
            flagEmoji = "🇩🇯"
        ),
        // 22. جزر القمر
        CurrencyInfo(
            code = "KMF",
            nameArabic = "الفرنك القمري",
            symbolArabic = "ف.ق",
            countryArabic = "جزر القمر",
            flagEmoji = "🇰🇲"
        ),
        // 23. الدولار الأمريكي
        CurrencyInfo(
            code = "USD",
            nameArabic = "الدولار الأمريكي",
            symbolArabic = "$",
            countryArabic = "عملة دولية",
            flagEmoji = "🇺🇸"
        ),
        // 24. اليورو
        CurrencyInfo(
            code = "EUR",
            nameArabic = "اليورو",
            symbolArabic = "€",
            countryArabic = "عملة دولية",
            flagEmoji = "🇪🇺"
        )
    )

    /**
     * جلب العملة الافتراضية للتطبيق (الريال اليمني)
     */
    val defaultCurrency: CurrencyInfo
        get() = allCurrencies.first { it.code == DEFAULT_CURRENCY_CODE }

    /**
     * جلب معلومات عملة عبر كود ISO 4217 مع العودة للعملة الافتراضية إذا كان الرمز غير معروف
     */
    fun getCurrencyByCode(code: String?): CurrencyInfo {
        if (code.isNullOrBlank()) return defaultCurrency
        return allCurrencies.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
            ?: defaultCurrency
    }

    /**
     * البحث في دليل العملات بالاسم العربي، رمز العملة، أو اسم الدولة
     */
    fun searchCurrencies(query: String): List<CurrencyInfo> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return allCurrencies
        return allCurrencies.filter { currency ->
            currency.code.contains(trimmed, ignoreCase = true) ||
            currency.nameArabic.contains(trimmed, ignoreCase = true) ||
            currency.symbolArabic.contains(trimmed, ignoreCase = true) ||
            currency.countryArabic.contains(trimmed, ignoreCase = true)
        }
    }
}
