package com.masroofi.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.masroofi.app.domain.model.TransactionType

/**
 * كيان جدول العمليات المالية في قاعدة البيانات المحلية (transactions)
 * يحتوي على قيود المفتاح الخارجي لمنع حذف التصنيفات المرتبطة بعمليات سابقة (RESTRICT)
 * وفهارس على الحقول الأكثر استعلاماً لتسريع الأداء
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transactionDate"]),
        Index(value = ["categoryId"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val type: TransactionType,
    val amount: Long, // بالريال أو العملة المختارة كعدد صحيح أكبر من صفر دائماً
    val categoryId: String,
    val transactionDate: String, // بصيغة ISO YYYY-MM-DD
    val note: String? = null,
    val currencyCode: String = "YER", // كود العملة وفق معيار ISO 4217
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(amount > 0) { "يجب أن يكون المبلغ المالي للعملية أكبر من الصفر دائماً" }
    }
}
