package com.masroofi.app.data.cloud

import android.content.Context
import com.masroofi.app.data.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * مزود النسخ السحابي الدائم والمتوافق لبيئة التطوير والمحاكي والإنتاج
 * يحفظ البيانات السحابية بشكل معزول يحاكي مستندات Firestore:
 * /users/{userId}/backups/{backupId}/metadata.json
 * /users/{userId}/backups/{backupId}/chunks/{chunkIndex}.json
 */
class PersistentCloudBackupProvider(
    private val context: Context? = null,
    private val preferencesManager: PreferencesManager? = null,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    cloudRootDirOverride: File? = null
) : CloudBackupProvider {

    private val cloudRootDir: File = cloudRootDirOverride
        ?: File(context?.filesDir ?: File(System.getProperty("java.io.tmpdir"), "masroof_cloud"), "cloud_storage").apply { if (!exists()) mkdirs() }

    private val authConfigFile: File
        get() = File(cloudRootDir, "auth_session.json")

    private val _currentUser = MutableStateFlow<CloudUserAccount?>(null)
    override val currentUser: Flow<CloudUserAccount?> = _currentUser.asStateFlow()

    private val _backupStatus = MutableStateFlow(CloudBackupStatus.IDLE)
    override val backupStatus: Flow<CloudBackupStatus> = _backupStatus.asStateFlow()

    private val _lastBackupTimestamp = MutableStateFlow<Long?>(null)
    override val lastBackupTimestamp: Flow<Long?> = _lastBackupTimestamp.asStateFlow()

    private val _isAutoBackupEnabled = MutableStateFlow(true)
    override val isAutoBackupEnabled: Flow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        try {
            if (authConfigFile.exists()) {
                val json = JSONObject(authConfigFile.readText())
                val userId = json.optString("userId")
                val email = json.optString("email")
                val displayName = json.optString("displayName", "مستخدم مصروف")
                val autoBackup = json.optBoolean("autoBackup", true)
                val lastBackup = json.optLong("lastBackupTimestamp", 0L)

                if (userId.isNotBlank() && email.isNotBlank()) {
                    _currentUser.value = CloudUserAccount(
                        userId = userId,
                        email = email,
                        displayName = displayName
                    )
                    _isAutoBackupEnabled.value = autoBackup
                    if (lastBackup > 0L) {
                        _lastBackupTimestamp.value = lastBackup
                        _backupStatus.value = CloudBackupStatus.UP_TO_DATE
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun saveSession() {
        try {
            val user = _currentUser.value
            if (user != null) {
                val json = JSONObject().apply {
                    put("userId", user.userId)
                    put("email", user.email)
                    put("displayName", user.displayName)
                    put("autoBackup", _isAutoBackupEnabled.value)
                    put("lastBackupTimestamp", _lastBackupTimestamp.value ?: 0L)
                }
                authConfigFile.writeText(json.toString())
            } else {
                if (authConfigFile.exists()) {
                    authConfigFile.delete()
                }
            }
        } catch (_: Exception) {
        }
    }

    override suspend fun signInWithGoogle(account: CloudUserAccount): Result<CloudUserAccount> = withContext(Dispatchers.IO) {
        runCatching {
            _currentUser.value = account
            // التحقق من وجود نسخ سابقة للمستخدم
            val backups = getAvailableBackups().getOrDefault(emptyList())
            if (backups.isNotEmpty()) {
                _lastBackupTimestamp.value = backups.first().createdAt
                _backupStatus.value = CloudBackupStatus.UP_TO_DATE
            } else {
                _backupStatus.value = CloudBackupStatus.IDLE
            }
            saveSession()
            account
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<CloudUserAccount> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim().lowercase()
            val cleanName = displayName.trim().ifBlank { "مستخدم مصروف" }
            val userId = "user_${cleanEmail.replace("@", "_").replace(".", "_")}"
            val profileFile = File(cloudRootDir, "users/$userId/profile.json").apply { parentFile?.mkdirs() }
            val profileJson = JSONObject().apply {
                put("userId", userId)
                put("email", cleanEmail)
                put("displayName", cleanName)
            }
            profileFile.writeText(profileJson.toString())

            val account = CloudUserAccount(
                userId = userId,
                email = cleanEmail,
                displayName = cleanName
            )
            _currentUser.value = account
            _lastBackupTimestamp.value = null
            _backupStatus.value = CloudBackupStatus.IDLE
            saveSession()
            account
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<CloudUserAccount> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim().lowercase()
            val userId = "user_${cleanEmail.replace("@", "_").replace(".", "_")}"
            val profileFile = File(cloudRootDir, "users/$userId/profile.json")
            val existingName = if (profileFile.exists()) {
                try {
                    JSONObject(profileFile.readText()).optString("displayName", "مستخدم مصروف")
                } catch (_: Exception) { "مستخدم مصروف" }
            } else if (authConfigFile.exists()) {
                try {
                    val json = JSONObject(authConfigFile.readText())
                    if (json.optString("email") == cleanEmail) {
                        json.optString("displayName", "مستخدم مصروف")
                    } else "مستخدم مصروف"
                } catch (_: Exception) { "مستخدم مصروف" }
            } else "مستخدم مصروف"

            val account = CloudUserAccount(
                userId = userId,
                email = cleanEmail,
                displayName = existingName
            )
            _currentUser.value = account
            val backups = getAvailableBackups().getOrDefault(emptyList())
            if (backups.isNotEmpty()) {
                _lastBackupTimestamp.value = backups.first().createdAt
                _backupStatus.value = CloudBackupStatus.UP_TO_DATE
            } else {
                _backupStatus.value = CloudBackupStatus.IDLE
            }
            saveSession()
            account
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // محاكاة إرسال البريد بنجاح في الوضع المحلي
            Unit
        }
    }

    override suspend fun changePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Unit
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            deleteUserCloudData()
            signOut()
            Unit
        }
    }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            _currentUser.value = null
            _backupStatus.value = CloudBackupStatus.IDLE
            _lastBackupTimestamp.value = null
            saveSession()
        }
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        _isAutoBackupEnabled.value = enabled
        saveSession()
    }

    override suspend fun updateBackupStatus(status: CloudBackupStatus) {
        _backupStatus.value = status
    }

    fun syncUser(account: CloudUserAccount?) {
        _currentUser.value = account
    }

    override suspend fun uploadBackup(
        metadata: CloudBackupMetadata,
        chunks: List<CloudBackupChunk>
    ): Result<CloudBackupMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val user = _currentUser.value ?: CloudUserAccount(
                userId = metadata.userId,
                email = "user@masroof.app",
                displayName = "مستخدم مصروف"
            )

            val backupDir = File(cloudRootDir, "users/${user.userId}/backups/${metadata.backupId}").apply { mkdirs() }
            val chunksDir = File(backupDir, "chunks").apply { mkdirs() }

            // 1. كتابة ملف البيانات الوصفية (Metadata)
            val metadataJson = JSONObject().apply {
                put("backupId", metadata.backupId)
                put("userId", metadata.userId)
                put("appName", metadata.appName)
                put("appVersion", metadata.appVersion)
                put("backupSchemaVersion", metadata.backupSchemaVersion)
                put("databaseSchemaVersion", metadata.databaseSchemaVersion)
                put("createdAt", metadata.createdAt)
                put("sha256Checksum", metadata.sha256Checksum)
                put("totalChunks", metadata.totalChunks)
                put("totalCompressedBytes", metadata.totalCompressedBytes)
                put("transactionCount", metadata.transactionCount)
                put("categoryCount", metadata.categoryCount)
                put("budgetCount", metadata.budgetCount)
                put("currencyCode", metadata.currencyCode)
                put("deviceName", metadata.deviceName)
                put("status", metadata.status)
            }
            File(backupDir, "metadata.json").writeText(metadataJson.toString(2))

            // 2. كتابة ملفات الأجزاء (Chunks)
            for (chunk in chunks) {
                val chunkJson = JSONObject().apply {
                    put("chunkIndex", chunk.chunkIndex)
                    put("backupId", chunk.backupId)
                    put("userId", chunk.userId)
                    put("dataBase64", chunk.dataBase64)
                    put("bytesCount", chunk.bytesCount)
                    put("checksum", chunk.checksum)
                }
                File(chunksDir, "${chunk.chunkIndex}.json").writeText(chunkJson.toString())
            }

            // 3. تحديث الحالات الزمنية
            _lastBackupTimestamp.value = metadata.createdAt
            _backupStatus.value = CloudBackupStatus.UP_TO_DATE
            saveSession()

            // 4. الاحتفاظ بآخر 3 نسخ وحذف الأقدم
            pruneOldBackups(retainCount = 3)

            metadata
        }
    }

    override suspend fun getAvailableBackups(): Result<List<CloudBackupMetadata>> = withContext(Dispatchers.IO) {
        runCatching {
            val user = _currentUser.value ?: return@runCatching emptyList()
            val userBackupsDir = File(cloudRootDir, "users/${user.userId}/backups")
            if (!userBackupsDir.exists() || !userBackupsDir.isDirectory) return@runCatching emptyList()

            val list = mutableListOf<CloudBackupMetadata>()
            userBackupsDir.listFiles()?.filter { it.isDirectory }?.forEach { bDir ->
                val metaFile = File(bDir, "metadata.json")
                if (metaFile.exists()) {
                    try {
                        val json = JSONObject(metaFile.readText())
                        list.add(
                            CloudBackupMetadata(
                                backupId = json.getString("backupId"),
                                userId = json.getString("userId"),
                                appName = json.optString("appName", "Masroof"),
                                appVersion = json.optString("appVersion", "1.1.0"),
                                backupSchemaVersion = json.optInt("backupSchemaVersion", 2),
                                databaseSchemaVersion = json.optInt("databaseSchemaVersion", 2),
                                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                                sha256Checksum = json.getString("sha256Checksum"),
                                totalChunks = json.getInt("totalChunks"),
                                totalCompressedBytes = json.optLong("totalCompressedBytes", 0L),
                                transactionCount = json.optInt("transactionCount", 0),
                                categoryCount = json.optInt("categoryCount", 0),
                                budgetCount = json.optInt("budgetCount", 0),
                                currencyCode = json.optString("currencyCode", "YER"),
                                deviceName = json.optString("deviceName", "Android Device"),
                                status = json.optString("status", "completed")
                            )
                        )
                    } catch (_: Exception) {
                    }
                }
            }
            list.sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getLatestBackup(): Result<CloudBackupMetadata?> = withContext(Dispatchers.IO) {
        runCatching {
            getAvailableBackups().getOrNull()?.firstOrNull()
        }
    }

    override suspend fun downloadBackupChunks(backupId: String): Result<List<CloudBackupChunk>> = withContext(Dispatchers.IO) {
        runCatching {
            val user = _currentUser.value ?: throw IllegalStateException("يجب تسجيل الدخول أولاً")
            val chunksDir = File(cloudRootDir, "users/${user.userId}/backups/$backupId/chunks")
            if (!chunksDir.exists() || !chunksDir.isDirectory) {
                throw IllegalStateException("مجلد أجزاء النسخة الاحتياطية غير موجود في السحابة")
            }

            val list = mutableListOf<CloudBackupChunk>()
            chunksDir.listFiles()?.filter { it.extension == "json" }?.forEach { cFile ->
                try {
                    val json = JSONObject(cFile.readText())
                    list.add(
                        CloudBackupChunk(
                            chunkIndex = json.getInt("chunkIndex"),
                            backupId = json.getString("backupId"),
                            userId = json.getString("userId"),
                            dataBase64 = json.getString("dataBase64"),
                            bytesCount = json.optInt("bytesCount", 0),
                            checksum = json.optString("checksum", "")
                        )
                    )
                } catch (_: Exception) {
                }
            }
            list.sortedBy { it.chunkIndex }
        }
    }

    override suspend fun pruneOldBackups(retainCount: Int): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val backups = getAvailableBackups().getOrDefault(emptyList())
            if (backups.size <= retainCount) return@runCatching 0

            val user = _currentUser.value ?: return@runCatching 0
            val excess = backups.drop(retainCount)
            var deletedCount = 0

            for (oldBackup in excess) {
                val dir = File(cloudRootDir, "users/${user.userId}/backups/${oldBackup.backupId}")
                if (dir.exists()) {
                    dir.deleteRecursively()
                    deletedCount++
                }
            }
            deletedCount
        }
    }

    override suspend fun deleteUserCloudData(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = _currentUser.value ?: return@runCatching
            val userDir = File(cloudRootDir, "users/${user.userId}")
            if (userDir.exists()) {
                userDir.deleteRecursively()
            }
            _lastBackupTimestamp.value = null
            _backupStatus.value = CloudBackupStatus.IDLE
            saveSession()
        }
    }
}
