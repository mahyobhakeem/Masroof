package com.masroofi.app.data.cloud

import android.content.Context
import com.masroofi.app.data.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * المزود السحابي الرئيسي لتطبيق مصروف
 * يدعم وضعين متكاملين:
 * 1. وضع السحابة الحقيقية عبر الإنترنت (Firebase Auth + Cloud Firestore REST API) عند توفر مفاتيح الاعتماد
 * 2. وضع المحاكاة المحلية المعزولة (Local Fallback) في بيئة التطوير أو عند غياب الاتصال بالإنترنت ومفاتيح السحابة
 */
class FirebaseCloudBackupProvider(
    private val context: Context? = null,
    private val preferencesManager: PreferencesManager? = null,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val cloudRootDirOverride: File? = null
) : CloudBackupProvider {

    private val localFallbackProvider: PersistentCloudBackupProvider = PersistentCloudBackupProvider(
        context = context,
        preferencesManager = preferencesManager,
        externalScope = externalScope,
        cloudRootDirOverride = cloudRootDirOverride
    )

    private val cloudRootDir: File = cloudRootDirOverride
        ?: File(context?.filesDir ?: File(System.getProperty("java.io.tmpdir"), "masroof_cloud"), "cloud_session").apply { if (!exists()) mkdirs() }

    private val sessionFile: File
        get() = File(cloudRootDir, "firebase_auth_session.json")

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

    private suspend fun getActiveApiKey(): String {
        val prefKey = preferencesManager?.cloudApiKey?.firstOrNull()?.trim()
        return if (!prefKey.isNullOrBlank()) prefKey else CloudConfig.DEFAULT_FIREBASE_API_KEY.trim()
    }

    private suspend fun getActiveProjectId(): String {
        val prefProject = preferencesManager?.cloudProjectId?.firstOrNull()?.trim()
        return if (!prefProject.isNullOrBlank()) prefProject else CloudConfig.DEFAULT_FIREBASE_PROJECT_ID.trim()
    }

    private suspend fun isRealCloudConfigured(): Boolean {
        if (cloudRootDirOverride != null) return false
        val key = getActiveApiKey()
        val project = getActiveProjectId()
        return CloudConfig.isConfigured(key, project)
    }

    private fun loadSession() {
        try {
            if (sessionFile.exists()) {
                val json = JSONObject(sessionFile.readText())
                val userId = json.optString("userId")
                val email = json.optString("email")
                val displayName = json.optString("displayName", "مستخدم مصروف")
                val idToken = json.optString("idToken", "")
                val refreshToken = json.optString("refreshToken", "")
                val tokenExpiry = json.optLong("tokenExpiryTimestamp", 0L)
                val autoBackup = json.optBoolean("autoBackup", true)
                val lastBackup = json.optLong("lastBackupTimestamp", 0L)

                if (userId.isNotBlank() && email.isNotBlank()) {
                    val userAcc = CloudUserAccount(
                        userId = userId,
                        email = email,
                        displayName = displayName,
                        idToken = idToken.ifBlank { null },
                        refreshToken = refreshToken.ifBlank { null },
                        tokenExpiryTimestamp = if (tokenExpiry > 0L) tokenExpiry else null
                    )
                    _currentUser.value = userAcc
                    localFallbackProvider.syncUser(userAcc)
                    _isAutoBackupEnabled.value = autoBackup
                    if (lastBackup > 0L) {
                        _lastBackupTimestamp.value = lastBackup
                        _backupStatus.value = CloudBackupStatus.UP_TO_DATE
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveSession() {
        try {
            val user = _currentUser.value
            if (user != null) {
                val json = JSONObject().apply {
                    put("userId", user.userId)
                    put("email", user.email)
                    put("displayName", user.displayName)
                    put("idToken", user.idToken ?: "")
                    put("refreshToken", user.refreshToken ?: "")
                    put("tokenExpiryTimestamp", user.tokenExpiryTimestamp ?: 0L)
                    put("autoBackup", _isAutoBackupEnabled.value)
                    put("lastBackupTimestamp", _lastBackupTimestamp.value ?: 0L)
                }
                sessionFile.writeText(json.toString())
            } else {
                if (sessionFile.exists()) {
                    sessionFile.delete()
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * التحقق من صلاحية الرمز وتجديده تلقائياً إذا شارف على الانتهاء
     */
    private suspend fun ensureValidIdToken(): String {
        val user = _currentUser.value ?: throw IllegalStateException("يجب تسجيل الدخول أولاً")
        val currentToken = user.idToken ?: ""
        val refreshToken = user.refreshToken ?: ""
        val expiry = user.tokenExpiryTimestamp ?: 0L
        val now = System.currentTimeMillis()

        // إذا كان الرمز سارياً لأكثر من 5 دقائق قادمة
        if (currentToken.isNotBlank() && (expiry - now) > 300_000L) {
            return currentToken
        }

        // تجديد الرمز إذا كان لدينا رمز تحديث
        if (refreshToken.isNotBlank()) {
            val apiKey = getActiveApiKey()
            val refreshResult = FirebaseAuthClient.refreshToken(apiKey, refreshToken)
            if (refreshResult.isSuccess) {
                val (newIdToken, newRefreshToken) = refreshResult.getOrThrow()
                val newExpiry = System.currentTimeMillis() + (3600L * 1000L)
                val updatedUser = user.copy(
                    idToken = newIdToken,
                    refreshToken = newRefreshToken,
                    tokenExpiryTimestamp = newExpiry
                )
                _currentUser.value = updatedUser
                saveSession()
                return newIdToken
            }
        }

        return currentToken
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<CloudUserAccount> = withContext(Dispatchers.IO) {
        val isConfigured = isRealCloudConfigured()
        if (isConfigured) {
            val apiKey = getActiveApiKey()
            val result = FirebaseAuthClient.signUp(apiKey, email, password, displayName)
            result.onSuccess { account ->
                _currentUser.value = account
                localFallbackProvider.syncUser(account)
                _lastBackupTimestamp.value = null
                _backupStatus.value = CloudBackupStatus.IDLE
                saveSession()
            }
            result
        } else {
            val result = localFallbackProvider.signUpWithEmail(email, password, displayName)
            result.onSuccess { account ->
                _currentUser.value = account
                localFallbackProvider.syncUser(account)
                _lastBackupTimestamp.value = null
                _backupStatus.value = CloudBackupStatus.IDLE
                saveSession()
            }
            result
        }
    }

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<CloudUserAccount> = withContext(Dispatchers.IO) {
        val isConfigured = isRealCloudConfigured()
        if (isConfigured) {
            val apiKey = getActiveApiKey()
            val result = FirebaseAuthClient.signIn(apiKey, email, password)
            result.onSuccess { account ->
                _currentUser.value = account
                localFallbackProvider.syncUser(account)
                saveSession()
                // فحص توفر نسخ سحابية للحساب
                val backups = getAvailableBackups().getOrDefault(emptyList())
                if (backups.isNotEmpty()) {
                    _lastBackupTimestamp.value = backups.first().createdAt
                    _backupStatus.value = CloudBackupStatus.UP_TO_DATE
                } else {
                    _backupStatus.value = CloudBackupStatus.IDLE
                }
                saveSession()
            }
            result
        } else {
            val result = localFallbackProvider.signInWithEmail(email, password)
            result.onSuccess { account ->
                _currentUser.value = account
                localFallbackProvider.syncUser(account)
                val backups = localFallbackProvider.getAvailableBackups().getOrDefault(emptyList())
                if (backups.isNotEmpty()) {
                    _lastBackupTimestamp.value = backups.first().createdAt
                    _backupStatus.value = CloudBackupStatus.UP_TO_DATE
                } else {
                    _backupStatus.value = CloudBackupStatus.IDLE
                }
                saveSession()
            }
            result
        }
    }

    override suspend fun signInWithGoogle(account: CloudUserAccount): Result<CloudUserAccount> = withContext(Dispatchers.IO) {
        _currentUser.value = account
        localFallbackProvider.syncUser(account)
        val backups = getAvailableBackups().getOrDefault(emptyList())
        if (backups.isNotEmpty()) {
            _lastBackupTimestamp.value = backups.first().createdAt
            _backupStatus.value = CloudBackupStatus.UP_TO_DATE
        } else {
            _backupStatus.value = CloudBackupStatus.IDLE
        }
        saveSession()
        Result.success(account)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (isRealCloudConfigured()) {
            val apiKey = getActiveApiKey()
            FirebaseAuthClient.sendPasswordReset(apiKey, email)
        } else {
            localFallbackProvider.sendPasswordResetEmail(email)
        }
    }

    override suspend fun changePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (isRealCloudConfigured()) {
            val apiKey = getActiveApiKey()
            val token = ensureValidIdToken()
            FirebaseAuthClient.changePassword(apiKey, token, newPassword)
        } else {
            localFallbackProvider.changePassword(newPassword)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isRealCloudConfigured()) {
            deleteUserCloudData()
            val apiKey = getActiveApiKey()
            val token = ensureValidIdToken()
            val result = FirebaseAuthClient.deleteAccount(apiKey, token)
            signOut()
            result
        } else {
            val result = localFallbackProvider.deleteAccount()
            signOut()
            result
        }
    }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        _currentUser.value = null
        _backupStatus.value = CloudBackupStatus.IDLE
        _lastBackupTimestamp.value = null
        saveSession()
        localFallbackProvider.signOut()
        Result.success(Unit)
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        _isAutoBackupEnabled.value = enabled
        saveSession()
        localFallbackProvider.setAutoBackupEnabled(enabled)
    }

    override suspend fun updateBackupStatus(status: CloudBackupStatus) {
        _backupStatus.value = status
        localFallbackProvider.updateBackupStatus(status)
    }

    override suspend fun uploadBackup(
        metadata: CloudBackupMetadata,
        chunks: List<CloudBackupChunk>
    ): Result<CloudBackupMetadata> = withContext(Dispatchers.IO) {
        if (isRealCloudConfigured()) {
            runCatching {
                val projectId = getActiveProjectId()
                val token = ensureValidIdToken()

                // رفع البيانات الوصفية إلى Firestore
                FirebaseFirestoreClient.saveBackupMetadata(projectId, token, metadata).getOrThrow()

                // رفع الأجزاء إلى المجموعة الفرعية chunks
                for (chunk in chunks) {
                    FirebaseFirestoreClient.saveBackupChunk(projectId, token, chunk).getOrThrow()
                }

                _lastBackupTimestamp.value = metadata.createdAt
                _backupStatus.value = CloudBackupStatus.UP_TO_DATE
                saveSession()

                pruneOldBackups(retainCount = 3)
                metadata
            }
        } else {
            val result = localFallbackProvider.uploadBackup(metadata, chunks)
            result.onSuccess {
                _lastBackupTimestamp.value = it.createdAt
                _backupStatus.value = CloudBackupStatus.UP_TO_DATE
                saveSession()
            }
            result
        }
    }

    override suspend fun getAvailableBackups(): Result<List<CloudBackupMetadata>> = withContext(Dispatchers.IO) {
        if (isRealCloudConfigured()) {
            runCatching {
                val user = _currentUser.value ?: return@runCatching emptyList()
                val projectId = getActiveProjectId()
                val token = ensureValidIdToken()
                FirebaseFirestoreClient.listBackups(projectId, user.userId, token).getOrThrow()
            }
        } else {
            localFallbackProvider.getAvailableBackups()
        }
    }

    override suspend fun getLatestBackup(): Result<CloudBackupMetadata?> = withContext(Dispatchers.IO) {
        runCatching {
            getAvailableBackups().getOrNull()?.firstOrNull()
        }
    }

    override suspend fun downloadBackupChunks(backupId: String): Result<List<CloudBackupChunk>> = withContext(Dispatchers.IO) {
        if (isRealCloudConfigured()) {
            runCatching {
                val user = _currentUser.value ?: throw IllegalStateException("يجب تسجيل الدخول أولاً")
                val projectId = getActiveProjectId()
                val token = ensureValidIdToken()
                FirebaseFirestoreClient.downloadChunks(projectId, user.userId, backupId, token).getOrThrow()
            }
        } else {
            localFallbackProvider.downloadBackupChunks(backupId)
        }
    }

    override suspend fun pruneOldBackups(retainCount: Int): Result<Int> = withContext(Dispatchers.IO) {
        if (isRealCloudConfigured()) {
            runCatching {
                val backups = getAvailableBackups().getOrDefault(emptyList())
                if (backups.size <= retainCount) return@runCatching 0

                val user = _currentUser.value ?: return@runCatching 0
                val projectId = getActiveProjectId()
                val token = ensureValidIdToken()
                val excess = backups.drop(retainCount)
                var count = 0

                for (oldBackup in excess) {
                    FirebaseFirestoreClient.deleteBackup(projectId, user.userId, oldBackup.backupId, token)
                    count++
                }
                count
            }
        } else {
            localFallbackProvider.pruneOldBackups(retainCount)
        }
    }

    override suspend fun deleteUserCloudData(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isRealCloudConfigured()) {
            runCatching {
                val user = _currentUser.value ?: return@runCatching
                val projectId = getActiveProjectId()
                val token = ensureValidIdToken()
                FirebaseFirestoreClient.deleteAllUserBackups(projectId, user.userId, token).getOrThrow()
                _lastBackupTimestamp.value = null
                _backupStatus.value = CloudBackupStatus.IDLE
                saveSession()
            }
        } else {
            localFallbackProvider.deleteUserCloudData()
            _lastBackupTimestamp.value = null
            _backupStatus.value = CloudBackupStatus.IDLE
            saveSession()
            Result.success(Unit)
        }
    }
}
