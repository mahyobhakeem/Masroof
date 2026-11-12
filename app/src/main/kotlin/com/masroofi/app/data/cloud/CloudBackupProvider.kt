package com.masroofi.app.data.cloud

import kotlinx.coroutines.flow.Flow

/**
 * واجهة مزود خدمات النسخ الاحتياطي السحابي
 * تفصل منطق السحابة (Firebase Authentication و Cloud Firestore) عن منطق التطبيق الداخلي
 */
interface CloudBackupProvider {
    /**
     * تدفق بيانات الحساب السحابي الحالي (أو null إذا لم يسجل دخوله)
     */
    val currentUser: Flow<CloudUserAccount?>

    /**
     * تدفق حالة النسخ السحابي الحالية
     */
    val backupStatus: Flow<CloudBackupStatus>

    /**
     * تاريخ ووقت آخر نسخة سحابية ناجحة (بالملي ثانية)
     */
    val lastBackupTimestamp: Flow<Long?>

    /**
     * هل النسخ التلقائي في الخلفية مفعّل
     */
    val isAutoBackupEnabled: Flow<Boolean>

    /**
     * تسجيل الدخول عبر حساب Google
     */
    suspend fun signInWithGoogle(account: CloudUserAccount): Result<CloudUserAccount>

    /**
     * تسجيل الخروج من السحابة
     */
    suspend fun signOut(): Result<Unit>

    /**
     * تبديل خيار النسخ السحابي التلقائي
     */
    suspend fun setAutoBackupEnabled(enabled: Boolean)

    /**
     * تحديث حالة النسخ السحابي
     */
    suspend fun updateBackupStatus(status: CloudBackupStatus)

    /**
     * رفع نسخة احتياطية جديدة مع أجزائها
     */
    suspend fun uploadBackup(
        metadata: CloudBackupMetadata,
        chunks: List<CloudBackupChunk>
    ): Result<CloudBackupMetadata>

    /**
     * استرجاع قائمة بجميع النسخ السحابية المتاحة للحساب الحالي
     */
    suspend fun getAvailableBackups(): Result<List<CloudBackupMetadata>>

    /**
     * استرجاع أحدث نسخة سحابية متوفرة للحساب الحالي
     */
    suspend fun getLatestBackup(): Result<CloudBackupMetadata?>

    /**
     * تنزيل أجزاء نسخة سحابية محددة
     */
    suspend fun downloadBackupChunks(backupId: String): Result<List<CloudBackupChunk>>

    /**
     * حذف النسخ القديمة والإبقاء على آخر عدد محدد (افتراضياً: 3 نسخ)
     */
    suspend fun pruneOldBackups(retainCount: Int = 3): Result<Int>

    /**
     * حذف جميع النسخ السحابية التابعة للمستخدم
     */
    suspend fun deleteUserCloudData(): Result<Unit>
}
