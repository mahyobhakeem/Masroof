package com.masroofi.app.data.backup

import kotlinx.serialization.Serializable

/**
 * كائن نقل بيانات التصنيف المالي في ملف النسخة الاحتياطية (JSON)
 */
@Serializable
data class CategoryBackupDto(
    val id: String,
    val name: String,
    val type: String, // "EXPENSE" أو "INCOME"
    val iconKey: String,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)

/**
 * كائن نقل بيانات العملية المالية في ملف النسخة الاحتياطية (JSON)
 */
@Serializable
data class TransactionBackupDto(
    val id: String,
    val type: String, // "EXPENSE" أو "INCOME"
    val amount: Long, // بالريال اليمني
    val categoryId: String,
    val transactionDate: String, // بصيغة ISO YYYY-MM-DD
    val note: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/**
 * كائن نقل بيانات الميزانية الشهرية في ملف النسخة الاحتياطية (JSON)
 */
@Serializable
data class MonthlyBudgetBackupDto(
    val id: String,
    val yearMonth: String, // بصيغة YYYY-MM
    val amount: Long // بالريال اليمني
)

/**
 * حمولة النسخة الاحتياطية الشاملة لتطبيق مصروفي بصيغة JSON
 */
@Serializable
data class BackupPayload(
    val backupFormatVersion: Int = 1,
    val createdAt: String,
    val appName: String = "Masroofi",
    val currency: String = "YER",
    val categories: List<CategoryBackupDto> = emptyList(),
    val transactions: List<TransactionBackupDto> = emptyList(),
    val monthlyBudgets: List<MonthlyBudgetBackupDto> = emptyList()
)

/**
 * ملخص محتويات النسخة الاحتياطية لعرضه في نافذة التأكيد قبل الاستعادة
 */
data class BackupSummary(
    val createdAt: String,
    val transactionCount: Int,
    val categoryCount: Int,
    val budgetCount: Int,
    val payload: BackupPayload
)
