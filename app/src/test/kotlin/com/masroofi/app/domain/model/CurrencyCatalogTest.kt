package com.masroofi.app.domain.model

import com.masroofi.app.utils.FormatUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * اختبارات وحدة شاملة لدليل العملات ومعالجة تنسيق العملات (CurrencyCatalogTest)
 * يتحقق من تواجد 24 عملة وتوافقها مع معيار ISO 4217 ودقة البحث والتنسيق المالي
 */
class CurrencyCatalogTest {

    @Test
    fun `test catalog contains exactly 24 currencies`() {
        val currencies = CurrencyCatalog.allCurrencies
        assertEquals("دليل العملات يجب أن يحتوي بدقة على 24 عملة (22 دولة عربية + الدولار واليورو)", 24, currencies.size)
    }

    @Test
    fun `test all currencies have complete non-empty metadata`() {
        for (currency in CurrencyCatalog.allCurrencies) {
            assertTrue("كود العملة لا يجب أن يكون فارغاً", currency.code.isNotBlank())
            assertEquals("كود العملة يجب أن يكون 3 أحرف وفق معيار ISO 4217", 3, currency.code.length)
            assertTrue("الاسم العربي لا يجب أن يكون فارغاً", currency.nameArabic.isNotBlank())
            assertTrue("الرمز المختصر بالعربية لا يجب أن يكون فارغاً", currency.symbolArabic.isNotBlank())
            assertTrue("اسم الدولة بالعربية لا يجب أن يكون فارغاً", currency.countryArabic.isNotBlank())
            assertTrue("رمز العلم لا يجب أن يكون فارغاً", currency.flagEmoji.isNotBlank())
        }
    }

    @Test
    fun `test all required Arab currencies and international currencies are present`() {
        val expectedCodes = listOf(
            "YER", "SAR", "AED", "KWD", "BHD", "QAR", "OMR", "EGP",
            "JOD", "IQD", "SYP", "LBP", "ILS", "MAD", "DZD", "TND",
            "LYD", "SDG", "MRU", "SOS", "DJF", "KMF", "USD", "EUR"
        )

        for (code in expectedCodes) {
            val currency = CurrencyCatalog.getCurrencyByCode(code)
            assertEquals("يجب العثور على العملة $code", code, currency.code)
        }
    }

    @Test
    fun `test default currency is Yemeni Rial YER`() {
        val defaultCurrency = CurrencyCatalog.defaultCurrency
        assertEquals("العملة الافتراضية يجب أن تكون الريال اليمني YER", "YER", defaultCurrency.code)
        assertEquals("الريال اليمني", defaultCurrency.nameArabic)
        assertEquals("ر.ي", defaultCurrency.symbolArabic)
        assertEquals("اليمن", defaultCurrency.countryArabic)
    }

    @Test
    fun `test unknown currency code falls back to default YER`() {
        val currency = CurrencyCatalog.getCurrencyByCode("XYZ_UNKNOWN")
        assertEquals("أي كود عملة غير معروف يجب أن يُرجع العملة الافتراضية YER", "YER", currency.code)
    }

    @Test
    fun `test search currencies by Arabic name and country and code`() {
        // البحث باسم الدولة
        val egyptResults = CurrencyCatalog.searchCurrencies("مصر")
        assertTrue(egyptResults.any { it.code == "EGP" })

        // البحث باسم العملة
        val saudiResults = CurrencyCatalog.searchCurrencies("سعودي")
        assertTrue(saudiResults.any { it.code == "SAR" })

        // البحث بالكود الإنجليزي (حساسية غير مفرطة لحالة الأحرف)
        val kwdResults = CurrencyCatalog.searchCurrencies("kwd")
        assertTrue(kwdResults.any { it.code == "KWD" })

        // البحث بالدينار (يجب أن يعيد كل العملات التي اسمها يحتوي على دينار)
        val dinarResults = CurrencyCatalog.searchCurrencies("دينار")
        assertTrue(dinarResults.any { it.code == "KWD" })
        assertTrue(dinarResults.any { it.code == "BHD" })
        assertTrue(dinarResults.any { it.code == "JOD" })
        assertTrue(dinarResults.any { it.code == "IQD" })
        assertTrue(dinarResults.any { it.code == "DZD" })
        assertTrue(dinarResults.any { it.code == "TND" })
        assertTrue(dinarResults.any { it.code == "LYD" })

        // البحث بفلسطين (يجب أن يظهر الشيكل ILS أو الأردن JOD)
        val palestineResults = CurrencyCatalog.searchCurrencies("فلسطين")
        assertTrue(palestineResults.any { it.code == "ILS" || it.code == "JOD" })
    }

    @Test
    fun `test formatCurrency with various currencies and thousands separators`() {
        // الريال اليمني
        val formattedYer = FormatUtils.formatCurrency(35000L, "YER")
        assertEquals("35,000 ر.ي", formattedYer)

        // الريال السعودي
        val formattedSar = FormatUtils.formatCurrency(1500L, "SAR")
        assertEquals("1,500 ر.س", formattedSar)

        // الدينار الكويتي
        val formattedKwd = FormatUtils.formatCurrency(250L, "KWD")
        assertEquals("250 د.ك", formattedKwd)

        // الجنيه المصري
        val formattedEgp = FormatUtils.formatCurrency(12000L, "EGP")
        assertEquals("12,000 ج.م", formattedEgp)

        // الدولار الأمريكي
        val formattedUsd = FormatUtils.formatCurrency(100L, "USD")
        assertEquals("100 $", formattedUsd)

        // اليورو
        val formattedEur = FormatUtils.formatCurrency(50L, "EUR")
        assertEquals("50 €", formattedEur)
    }
}
