package com.masroofi.app.data.cloud

import com.masroofi.app.domain.model.CurrencyCatalog

/**
 * البيانات الوصفية للنسخة الاحتياطية السحابية المحفوظة في Firestore
 * المسار: /users/{userId}/backups/{backupId}
 */
data class CloudBackupMetadata(
    val backupId: String,
    val userId: String,
    val appName: String = "Masroof",
    val appVersion: String = "1.1.0",
    val backupSchemaVersion: Int = 2,
    val databaseSchemaVersion: Int = 2,
    val createdAt: Long = System.currentTimeMillis(),
    val sha256Checksum: String,
    val totalChunks: Int,
    val totalCompressedBytes: Long,
    val transactionCount: Int,
    val categoryCount: Int,
    val budgetCount: Int,
    val currencyCode: String = CurrencyCatalog.DEFAULT_CURRENCY_CODE,
    val deviceName: String = "Android Device",
    val status: String = "completed"
)

/**
 * جزء من البيانات المضغوطة للنسخة السحابية (Chunk)
 * المسار: /users/{userId}/backups/{backupId}/chunks/{chunkIndex}
 */
data class CloudBackupChunk(
    val chunkIndex: Int,
    val backupId: String,
    val userId: String,
    val dataBase64: String,
    val bytesCount: Int,
    val checksum: String = ""
)

/**
 * حالات مزامنة النسخ السحابي المعروضة للمستخدم
 */
enum class CloudBackupStatus {
    IDLE,        // في وضع الاستعداد
    UP_TO_DATE,  // محدّثة (البيانات متطابقة مع آخر نسخة سحابية)
    PENDING,     // توجد بيانات معلقة بانتظار النسخ
    BACKING_UP,  // جاري النسخ السحابي...
    FAILED       // فشل الاتصال بالسحابة — ستتم إعادة المحاولة تلقائياً
}

/**
 * بيانات حساب المستخدم المرتبط بالنسخ الاحتياطي السحابي
 */
data class CloudUserAccount(
    val userId: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null
)
