package com.masroofi.app.data.export

import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType

/**
 * مصدّر سجل العمليات المالية إلى صيغة CSV
 * مع دعم كامل للأحرف العربية عبر علامة ترتيب البايت UTF-8 BOM
 */
object CsvExporter {

    /**
     * علامة ترتيب البايت UTF-8 BOM لضمان قراءة البرامج (مثل Microsoft Excel)
     * للغة العربية بشكل سليم تماماً دون تشويه
     */
    const val UTF8_BOM = "\uFEFF"

    /**
     * ترويسة ملف CSV باللغة العربية الفصحى
     */
    const val CSV_HEADER = "التاريخ,النوع,التصنيف,المبلغ (ريال يمني),الملاحظات"

    /**
     * تصدير قائمة العمليات المالية إلى نص CSV مكتمل
     */
    fun exportTransactions(transactions: List<TransactionItem>): String {
        val stringBuilder = StringBuilder()
        // 1. إضافة بادئة UTF-8 BOM
        stringBuilder.append(UTF8_BOM)

        // 2. إضافة سطر الترويسة
        stringBuilder.append(CSV_HEADER).append("\n")

        // 3. ترتيب العمليات من الأحدث إلى الأقدم
        val sortedTransactions = transactions.sortedWith(
            compareByDescending<TransactionItem> { it.transactionDate }
                .thenByDescending { it.createdAt }
        )

        // 4. كتابة أسطر العمليات
        for (tx in sortedTransactions) {
            val dateStr = escapeCsvField(tx.transactionDate)
            val typeStr = escapeCsvField(if (tx.type == TransactionType.EXPENSE) "مصروف" else "إيراد")
            val categoryStr = escapeCsvField(tx.categoryName ?: "غير مصنف")
            val amountStr = tx.amount.toString()
            val noteStr = escapeCsvField(tx.note ?: "")

            stringBuilder.append("$dateStr,$typeStr,$categoryStr,$amountStr,$noteStr\n")
        }

        return stringBuilder.toString()
    }

    /**
     * معالجة الحقل لمنع كسر بنية ملف CSV في حال وجود فواصل أو علامات تنصيص أو أسطر جديدة
     */
    fun escapeCsvField(value: String): String {
        val needsQuotes = value.contains(",") ||
                value.contains("\"") ||
                value.contains("\n") ||
                value.contains("\r")

        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
