package com.masroofi.app.domain.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * نموذج نطاق العمل للعملية المالية الحقيقية
 */
@Immutable
data class TransactionItem(
    val id: String,
    val type: TransactionType,
    val amount: Long, // خالي من الكسور أكبر من صفر دائماً
    val categoryId: String,
    val categoryName: String? = null,
    val categoryIconKey: String? = null,
    val transactionDate: String, // بصيغة ISO YYYY-MM-DD
    val note: String? = null,
    val currencyCode: String = "YER", // كود العملة (ISO 4217)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(amount > 0) { "يجب أن يكون المبلغ المالي أكبر من الصفر دائماً" }
    }

    /**
     * تحويل تاريخ العملية إلى كائن LocalDate
     */
    fun toLocalDate(): LocalDate {
        return LocalDate.parse(transactionDate)
    }
}
