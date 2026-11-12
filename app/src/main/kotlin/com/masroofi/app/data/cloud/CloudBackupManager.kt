package com.masroofi.app.data.cloud

import com.masroofi.app.data.backup.BackupManager
import com.masroofi.app.data.database.MasroofiDatabase
import com.masroofi.app.data.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * مدير عمليات النسخ الاحتياطي السحابي والاستعادة الذرية
 * ينسق بين قاعدة البيانات المحلية، محرك الضغط والتشفير، ومزود السحابة
 */
class CloudBackupManager(
    private val database: MasroofiDatabase,
    private val backupManager: BackupManager,
    val provider: CloudBackupProvider,
    private val preferencesManager: PreferencesManager? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    /**
     * إنشاء ورفع نسخة احتياطية سحابية كاملة ومضغوطة
     */
    suspend fun createAndUploadCloudBackup(): Result<CloudBackupMetadata> = withContext(Dispatchers.IO) {
        val user = provider.currentUser.first()
            ?: return@withContext Result.failure(IllegalStateException("يجب تسجيل الدخول باستخدام حساب Google أولاً"))

        provider.updateBackupStatus(CloudBackupStatus.BACKING_UP)

        try {
            // 1. توليد نص JSON الشامل للبيانات المحلية
            val backupJsonString = backupManager.createBackupJson()
            val rawJsonObj = JSONObject(backupJsonString)

            val transactionCount = rawJsonObj.optJSONArray("transactions")?.length() ?: 0
            val categoryCount = rawJsonObj.optJSONArray("categories")?.length() ?: 0
            val budgetCount = rawJsonObj.optJSONArray("monthlyBudgets")?.length() ?: 0
            val currencyCode = rawJsonObj.optString("currency", "YER")

            // 2. تحويل النص إلى بايتات وضغطها بنظام GZIP
            val rawBytes = backupJsonString.toByteArray(StandardCharsets.UTF_8)
            val compressedBytes = CloudBackupEngine.compressGzip(rawBytes)

            // 3. احتساب بصمة SHA-256 للبيانات المضغوطة
            val sha256Checksum = CloudBackupEngine.calculateSha256(compressedBytes)

            // 4. تقسيم البيانات المضغوطة إلى أجزاء بحجم أقصى 700KB
            val chunkByteArrays = CloudBackupEngine.splitIntoChunks(compressedBytes)
            val backupId = "backup_${System.currentTimeMillis()}"

            val chunks = chunkByteArrays.mapIndexed { index, bytes ->
                CloudBackupChunk(
                    chunkIndex = index,
                    backupId = backupId,
                    userId = user.userId,
                    dataBase64 = CloudBackupEngine.encodeBase64(bytes),
                    bytesCount = bytes.size,
                    checksum = CloudBackupEngine.calculateSha256(bytes)
                )
            }

            // 5. بناء كائن البيانات الوصفية (Metadata)
            val metadata = CloudBackupMetadata(
                backupId = backupId,
                userId = user.userId,
                appName = "Masroof",
                appVersion = "1.1.0",
                backupSchemaVersion = 2,
                databaseSchemaVersion = 2,
                createdAt = System.currentTimeMillis(),
                sha256Checksum = sha256Checksum,
                totalChunks = chunks.size,
                totalCompressedBytes = compressedBytes.size.toLong(),
                transactionCount = transactionCount,
                categoryCount = categoryCount,
                budgetCount = budgetCount,
                currencyCode = currencyCode,
                deviceName = android.os.Build.MODEL ?: "Android Device",
                status = "completed"
            )

            // 6. رفع البيانات إلى السحابة
            val uploadResult = provider.uploadBackup(metadata, chunks)
            if (uploadResult.isSuccess) {
                provider.updateBackupStatus(CloudBackupStatus.UP_TO_DATE)
                Result.success(metadata)
            } else {
                provider.updateBackupStatus(CloudBackupStatus.FAILED)
                Result.failure(uploadResult.exceptionOrNull() ?: Exception("فشل في رفع النسخة السحابية"))
            }
        } catch (e: Exception) {
            provider.updateBackupStatus(CloudBackupStatus.FAILED)
            Result.failure(e)
        }
    }

    /**
     * استعادة البيانات من النسخة الاحتياطية السحابية بأمان ذري
     * مع التحقق الكامل من بصمة SHA-256 قبل المساس بقاعدة البيانات المحلية
     */
    suspend fun restoreFromCloud(metadata: CloudBackupMetadata): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. تنزيل أجزاء النسخة السحابية
            val chunksResult = provider.downloadBackupChunks(metadata.backupId)
            val chunks = chunksResult.getOrElse {
                return@withContext Result.failure(it)
            }

            if (chunks.size != metadata.totalChunks) {
                return@withContext Result.failure(
                    IllegalStateException("عدد الأجزاء المنزلة (${chunks.size}) لا يطابق العدد المسجل (${metadata.totalChunks})")
                )
            }

            // 2. إعادة تجميع الأجزاء في مصفوفة بايتات واحدة
            val sortedChunks = chunks.sortedBy { it.chunkIndex }
            val chunkByteArrays = sortedChunks.map {
                CloudBackupEngine.decodeBase64(it.dataBase64)
            }
            val reassembledCompressedBytes = CloudBackupEngine.reassembleChunks(chunkByteArrays)

            // 3. التحقق الأمني الذري من بصمة SHA-256
            val isChecksumValid = CloudBackupEngine.verifySha256(
                data = reassembledCompressedBytes,
                expectedChecksum = metadata.sha256Checksum
            )

            if (!isChecksumValid) {
                return@withContext Result.failure(
                    SecurityException("فشل التحقق من بصمة الأمان SHA-256: بيانات النسخة السحابية تالفة أو غير متطابقة. تم إلغاء الاستعادة لحماية بياناتك المحلية.")
                )
            }

            // 4. فك الضغط بنظام GZIP
            val decompressedBytes = CloudBackupEngine.decompressGzip(reassembledCompressedBytes)
            val decompressedJsonString = String(decompressedBytes, StandardCharsets.UTF_8)

            // 5. التحقق الصارم من هيكل JSON
            val validationResult = backupManager.validateBackupJson(decompressedJsonString)
            val summary = validationResult.getOrElse {
                return@withContext Result.failure(it)
            }

            // 6. تنفيذ الاستعادة الذرية الشاملة واستبدال البيانات المحلية بأمان
            val restoreResult = backupManager.restoreBackup(summary.payload)
            if (restoreResult.isSuccess) {
                provider.updateBackupStatus(CloudBackupStatus.UP_TO_DATE)
                Result.success(Unit)
            } else {
                Result.failure(restoreResult.exceptionOrNull() ?: Exception("فشل في استعادة البيانات إلى قاعدة البيانات"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * التحقق من وجود تعديلات محلية معلقة بانتظار المزامنة السحابية
     */
    suspend fun checkPendingChanges() = withContext(Dispatchers.IO) {
        try {
            val user = provider.currentUser.firstOrNull() ?: return@withContext
            val lastBackup = provider.lastBackupTimestamp.firstOrNull() ?: return@withContext

            val transactions = database.transactionDao().getAllTransactionEntities()
            val latestTx = transactions.maxByOrNull { it.updatedAt }
            if (latestTx != null && latestTx.updatedAt > lastBackup) {
                provider.updateBackupStatus(CloudBackupStatus.PENDING)
            }
        } catch (_: Exception) {
        }
    }
}
