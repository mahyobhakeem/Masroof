package com.masroofi.app.data.export

import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

/**
 * اختبارات وحدة شاملة لمصدّر ملفات CSV (CsvExporterTest)
 * يتحقق من علامة ترتيب البايت UTF-8 BOM وصحة الترويسة والهروب الآمن للحقول
 */
class CsvExporterTest {

    @Test
    fun `test CSV starts with UTF-8 BOM`() {
        val result = CsvExporter.exportTransactions(emptyList())
        assertTrue("يجب أن يبدأ ملف CSV بعلامة UTF-8 BOM لضمان قراءة العربية في Excel", result.startsWith(CsvExporter.UTF8_BOM))
        assertTrue("رمز BOM يجب أن يكون \\uFEFF", result.startsWith("\uFEFF"))
    }

    @Test
    fun `test CSV header matches requirement exactly`() {
        val result = CsvExporter.exportTransactions(emptyList())
        val lines = result.removePrefix(CsvExporter.UTF8_BOM).trim().split("\n")
        assertEquals(1, lines.size)
        assertEquals("التاريخ,النوع,التصنيف,المبلغ (ريال يمني),الملاحظات", lines[0])
    }

    @Test
    fun `test export transactions formats rows correctly`() {
        val transactions = listOf(
            TransactionItem(
                id = "tx_1",
                type = TransactionType.EXPENSE,
                amount = 15000L,
                categoryId = "cat_food",
                categoryName = "طعام ومشروبات",
                transactionDate = "2026-11-10",
                note = "غداء"
            ),
            TransactionItem(
                id = "tx_2",
                type = TransactionType.INCOME,
                amount = 350000L,
                categoryId = "cat_salary",
                categoryName = "الراتب الشهري",
                transactionDate = "2026-11-11",
                note = null
            )
        )

        val csv = CsvExporter.exportTransactions(transactions)
        val lines = csv.removePrefix(CsvExporter.UTF8_BOM).trim().split("\n")

        assertEquals(3, lines.size)
        assertEquals("التاريخ,النوع,التصنيف,المبلغ (ريال يمني),الملاحظات", lines[0])

        // الأحدث تاريخاً يظهر أولاً (2026-11-11 قبل 2026-11-10)
        assertEquals("2026-11-11,إيراد,الراتب الشهري,350000,", lines[1])
        assertEquals("2026-11-10,مصروف,طعام ومشروبات,15000,غداء", lines[2])
    }

    @Test
    fun `test CSV escaping for notes with commas`() {
        val transactions = listOf(
            TransactionItem(
                id = "tx_comma",
                type = TransactionType.EXPENSE,
                amount = 5000L,
                categoryId = "cat_food",
                categoryName = "طعام ومشروبات",
                transactionDate = "2026-11-11",
                note = "شراء خضار, فواكه, ولحم"
            )
        )

        val csv = CsvExporter.exportTransactions(transactions)
        val lines = csv.removePrefix(CsvExporter.UTF8_BOM).trim().split("\n")

        assertEquals(2, lines.size)
        // يجب تغليف الملاحظة بعلامات تنصيص بسبب وجود الفواصل
        assertEquals("2026-11-11,مصروف,طعام ومشروبات,5000,\"شراء خضار, فواكه, ولحم\"", lines[1])
    }

    @Test
    fun `test CSV escaping for notes with quotation marks`() {
        val transactions = listOf(
            TransactionItem(
                id = "tx_quotes",
                type = TransactionType.EXPENSE,
                amount = 12000L,
                categoryId = "cat_electronics",
                categoryName = "إلكترونيات",
                transactionDate = "2026-11-11",
                note = "شراء سماعة \"بلوتوث\" أصلية"
            )
        )

        val csv = CsvExporter.exportTransactions(transactions)
        val lines = csv.removePrefix(CsvExporter.UTF8_BOM).trim().split("\n")

        assertEquals(2, lines.size)
        // علامات التنصيص المزدوجة يجب مضاعفتها داخل الحقل المغلف
        assertEquals("2026-11-11,مصروف,إلكترونيات,12000,\"شراء سماعة \"\"بلوتوث\"\" أصلية\"", lines[1])
    }

    @Test
    fun `test CSV escaping for notes with newlines`() {
        val transactions = listOf(
            TransactionItem(
                id = "tx_newline",
                type = TransactionType.EXPENSE,
                amount = 8000L,
                categoryId = "cat_misc",
                categoryName = "متنوعات",
                transactionDate = "2026-11-11",
                note = "السطر الأول\nالسطر الثاني"
            )
        )

        val csv = CsvExporter.exportTransactions(transactions)
        assertTrue(csv.contains("\"السطر الأول\nالسطر الثاني\""))
    }
}
