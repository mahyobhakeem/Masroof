package com.masroofi.app.data.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

/**
 * عميل سحابي لقاعدة بيانات Cloud Firestore عبر واجهة REST API القياسية المدمجة في أندرويد
 * يقوم برفع واسترجاع وحذف النسخ الاحتياطية السحابية وأجزائها المشفرة والمضغوطة
 * تحت المسار المعزول أمنياً: users/{userId}/backups/{backupId}
 */
object FirebaseFirestoreClient {

    private const val CONNECT_TIMEOUT_MS = 20000
    private const val READ_TIMEOUT_MS = 30000

    /**
     * حفظ مستند البيانات الوصفية للنسخة الاحتياطية (Metadata)
     */
    suspend fun saveBackupMetadata(
        projectId: String,
        idToken: String,
        metadata: CloudBackupMetadata
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = CloudConfig.getFirestoreDocumentsUrl(projectId)
            val docUrl = "$baseUrl/users/${metadata.userId}/backups/${metadata.backupId}"

            val fields = JSONObject().apply {
                put("backupId", stringField(metadata.backupId))
                put("userId", stringField(metadata.userId))
                put("appName", stringField(metadata.appName))
                put("appVersion", stringField(metadata.appVersion))
                put("backupSchemaVersion", integerField(metadata.backupSchemaVersion.toLong()))
                put("databaseSchemaVersion", integerField(metadata.databaseSchemaVersion.toLong()))
                put("createdAt", integerField(metadata.createdAt))
                put("sha256Checksum", stringField(metadata.sha256Checksum))
                put("totalChunks", integerField(metadata.totalChunks.toLong()))
                put("totalCompressedBytes", integerField(metadata.totalCompressedBytes))
                put("transactionCount", integerField(metadata.transactionCount.toLong()))
                put("categoryCount", integerField(metadata.categoryCount.toLong()))
                put("budgetCount", integerField(metadata.budgetCount.toLong()))
                put("currencyCode", stringField(metadata.currencyCode))
                put("deviceName", stringField(metadata.deviceName))
                put("status", stringField(metadata.status))
            }

            val docBody = JSONObject().apply {
                put("fields", fields)
            }

            executeHttp(docUrl, "PATCH", idToken, docBody.toString())
            Unit
        }
    }

    /**
     * رفع جزء مضغوط من النسخة السحابية (Chunk)
     */
    suspend fun saveBackupChunk(
        projectId: String,
        idToken: String,
        chunk: CloudBackupChunk
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = CloudConfig.getFirestoreDocumentsUrl(projectId)
            val docUrl = "$baseUrl/users/${chunk.userId}/backups/${chunk.backupId}/chunks/${chunk.chunkIndex}"

            val fields = JSONObject().apply {
                put("chunkIndex", integerField(chunk.chunkIndex.toLong()))
                put("backupId", stringField(chunk.backupId))
                put("userId", stringField(chunk.userId))
                put("dataBase64", stringField(chunk.dataBase64))
                put("bytesCount", integerField(chunk.bytesCount.toLong()))
                put("checksum", stringField(chunk.checksum))
            }

            val docBody = JSONObject().apply {
                put("fields", fields)
            }

            executeHttp(docUrl, "PATCH", idToken, docBody.toString())
            Unit
        }
    }

    /**
     * استرجاع قائمة بجميع النسخ الاحتياطية المتوفرة لحساب المستخدم من السحابة
     */
    suspend fun listBackups(
        projectId: String,
        userId: String,
        idToken: String
    ): Result<List<CloudBackupMetadata>> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = CloudConfig.getFirestoreDocumentsUrl(projectId)
            val listUrl = "$baseUrl/users/$userId/backups?pageSize=100"

            val responseJson = executeHttp(listUrl, "GET", idToken)
            val documents = responseJson.optJSONArray("documents") ?: JSONArray()
            val resultList = mutableListOf<CloudBackupMetadata>()

            for (i in 0 until documents.length()) {
                val doc = documents.getJSONObject(i)
                val fields = doc.optJSONObject("fields") ?: continue

                val meta = CloudBackupMetadata(
                    backupId = extractString(fields, "backupId"),
                    userId = extractString(fields, "userId", userId),
                    appName = extractString(fields, "appName", "Masroof"),
                    appVersion = extractString(fields, "appVersion", "1.1.0"),
                    backupSchemaVersion = extractLong(fields, "backupSchemaVersion", 2L).toInt(),
                    databaseSchemaVersion = extractLong(fields, "databaseSchemaVersion", 2L).toInt(),
                    createdAt = extractLong(fields, "createdAt", System.currentTimeMillis()),
                    sha256Checksum = extractString(fields, "sha256Checksum"),
                    totalChunks = extractLong(fields, "totalChunks", 1L).toInt(),
                    totalCompressedBytes = extractLong(fields, "totalCompressedBytes", 0L),
                    transactionCount = extractLong(fields, "transactionCount", 0L).toInt(),
                    categoryCount = extractLong(fields, "categoryCount", 0L).toInt(),
                    budgetCount = extractLong(fields, "budgetCount", 0L).toInt(),
                    currencyCode = extractString(fields, "currencyCode", "YER"),
                    deviceName = extractString(fields, "deviceName", "Android Device"),
                    status = extractString(fields, "status", "completed")
                )
                if (meta.backupId.isNotBlank()) {
                    resultList.add(meta)
                }
            }

            resultList.sortedByDescending { it.createdAt }
        }
    }

    /**
     * تنزيل أجزاء نسخة احتياطية محددة من السحابة
     */
    suspend fun downloadChunks(
        projectId: String,
        userId: String,
        backupId: String,
        idToken: String
    ): Result<List<CloudBackupChunk>> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = CloudConfig.getFirestoreDocumentsUrl(projectId)
            val chunksUrl = "$baseUrl/users/$userId/backups/$backupId/chunks?pageSize=300"

            val responseJson = executeHttp(chunksUrl, "GET", idToken)
            val documents = responseJson.optJSONArray("documents") ?: JSONArray()
            val chunkList = mutableListOf<CloudBackupChunk>()

            for (i in 0 until documents.length()) {
                val doc = documents.getJSONObject(i)
                val fields = doc.optJSONObject("fields") ?: continue

                val chunk = CloudBackupChunk(
                    chunkIndex = extractLong(fields, "chunkIndex", 0L).toInt(),
                    backupId = extractString(fields, "backupId", backupId),
                    userId = extractString(fields, "userId", userId),
                    dataBase64 = extractString(fields, "dataBase64"),
                    bytesCount = extractLong(fields, "bytesCount", 0L).toInt(),
                    checksum = extractString(fields, "checksum")
                )
                chunkList.add(chunk)
            }

            chunkList.sortedBy { it.chunkIndex }
        }
    }

    /**
     * حذف نسخة احتياطية وكافة أجزائها من السحابة
     */
    suspend fun deleteBackup(
        projectId: String,
        userId: String,
        backupId: String,
        idToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = CloudConfig.getFirestoreDocumentsUrl(projectId)

            // أولاً: قراءة وحذف الأجزاء التابعة للنسخة
            val chunksResult = downloadChunks(projectId, userId, backupId, idToken)
            chunksResult.getOrNull()?.forEach { chunk ->
                val chunkDocUrl = "$baseUrl/users/$userId/backups/$backupId/chunks/${chunk.chunkIndex}"
                try {
                    executeHttp(chunkDocUrl, "DELETE", idToken)
                } catch (_: Exception) {}
            }

            // ثانياً: حذف مستند النسخة الرئيسي
            val backupDocUrl = "$baseUrl/users/$userId/backups/$backupId"
            executeHttp(backupDocUrl, "DELETE", idToken)
            Unit
        }
    }

    /**
     * حذف كافة النسخ الاحتياطية التابعة للمستخدم من السحابة نهائياً
     */
    suspend fun deleteAllUserBackups(
        projectId: String,
        userId: String,
        idToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val backups = listBackups(projectId, userId, idToken).getOrDefault(emptyList())
            for (backup in backups) {
                deleteBackup(projectId, userId, backup.backupId, idToken)
            }
            Unit
        }
    }

    private fun stringField(value: String): JSONObject {
        return JSONObject().apply { put("stringValue", value) }
    }

    private fun integerField(value: Long): JSONObject {
        return JSONObject().apply { put("integerValue", value.toString()) }
    }

    private fun extractString(fields: JSONObject, key: String, default: String = ""): String {
        val obj = fields.optJSONObject(key) ?: return default
        return obj.optString("stringValue", default)
    }

    private fun extractLong(fields: JSONObject, key: String, default: Long = 0L): Long {
        val obj = fields.optJSONObject(key) ?: return default
        return obj.optString("integerValue", default.toString()).toLongOrNull() ?: default
    }

    private fun executeHttp(
        urlStr: String,
        method: String,
        idToken: String,
        jsonBody: String? = null
    ): JSONObject {
        val url = URL(urlStr)
        val connection = (url.openConnection() as HttpsURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $idToken")
            setRequestProperty("Accept", "application/json")
            if (jsonBody != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
        }

        if (jsonBody != null) {
            connection.outputStream.use { os ->
                os.write(jsonBody.toByteArray(StandardCharsets.UTF_8))
            }
        }

        val statusCode = connection.responseCode
        if (statusCode in 200..299) {
            val responseText = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            return if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
        } else {
            val errorText = connection.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            throw IllegalStateException("فشل الاتصال بخادم Firestore ($statusCode): $errorText")
        }
    }
}
