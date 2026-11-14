package com.masroofi.app.data.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

/**
 * عميل مصادقة Firebase السحابي عبر واجهة REST API القياسية المدمجة في أندرويد
 * يتيح التسجيل والدخول وتجديد الرموز المميزة دون الحاجة إلى مكتبات خارجية أو شهادات SHA-1 معقدة
 */
object FirebaseAuthClient {

    private const val CONNECT_TIMEOUT_MS = 15000
    private const val READ_TIMEOUT_MS = 20000

    /**
     * إنشاء حساب جديد بالبريد الإلكتروني وكلمة المرور
     */
    suspend fun signUp(
        apiKey: String,
        email: String,
        password: String,
        displayName: String
    ): Result<CloudUserAccount> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${CloudConfig.AUTH_BASE_URL}/accounts:signUp?key=$apiKey"
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                put("returnSecureToken", true)
            }

            val response = executePost(url, payload.toString())
            val idToken = response.getString("idToken")
            val refreshToken = response.getString("refreshToken")
            val localId = response.getString("localId")
            val expiresInSeconds = response.optLong("expiresIn", 3600L)
            val expiryTimestamp = System.currentTimeMillis() + (expiresInSeconds * 1000L)

            // تحديث الاسم الظاهر للحساب
            val cleanDisplayName = displayName.trim().ifBlank { "مستخدم مصروف" }
            updateProfile(apiKey, idToken, cleanDisplayName)

            CloudUserAccount(
                userId = localId,
                email = email.trim(),
                displayName = cleanDisplayName,
                idToken = idToken,
                refreshToken = refreshToken,
                tokenExpiryTimestamp = expiryTimestamp
            )
        }
    }

    /**
     * تسجيل الدخول بالبريد الإلكتروني وكلمة المرور
     */
    suspend fun signIn(
        apiKey: String,
        email: String,
        password: String
    ): Result<CloudUserAccount> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${CloudConfig.AUTH_BASE_URL}/accounts:signInWithPassword?key=$apiKey"
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                put("returnSecureToken", true)
            }

            val response = executePost(url, payload.toString())
            val idToken = response.getString("idToken")
            val refreshToken = response.getString("refreshToken")
            val localId = response.getString("localId")
            var displayName = response.optString("displayName", "")
            val expiresInSeconds = response.optLong("expiresIn", 3600L)
            val expiryTimestamp = System.currentTimeMillis() + (expiresInSeconds * 1000L)

            if (displayName.isBlank()) {
                displayName = "مستخدم مصروف"
            }

            CloudUserAccount(
                userId = localId,
                email = email.trim(),
                displayName = displayName,
                idToken = idToken,
                refreshToken = refreshToken,
                tokenExpiryTimestamp = expiryTimestamp
            )
        }
    }

    /**
     * تحديث الاسم الظاهر للحساب
     */
    suspend fun updateProfile(
        apiKey: String,
        idToken: String,
        displayName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${CloudConfig.AUTH_BASE_URL}/accounts:update?key=$apiKey"
            val payload = JSONObject().apply {
                put("idToken", idToken)
                put("displayName", displayName)
                put("returnSecureToken", true)
            }
            executePost(url, payload.toString())
            Unit
        }
    }

    /**
     * إرسال رابط استعادة كلمة المرور إلى البريد الإلكتروني
     */
    suspend fun sendPasswordReset(
        apiKey: String,
        email: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${CloudConfig.AUTH_BASE_URL}/accounts:sendOobCode?key=$apiKey"
            val payload = JSONObject().apply {
                put("requestType", "PASSWORD_RESET")
                put("email", email.trim())
            }
            executePost(url, payload.toString())
            Unit
        }
    }

    /**
     * تغيير كلمة المرور للمستخدم المصادق عليه
     */
    suspend fun changePassword(
        apiKey: String,
        idToken: String,
        newPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${CloudConfig.AUTH_BASE_URL}/accounts:update?key=$apiKey"
            val payload = JSONObject().apply {
                put("idToken", idToken)
                put("password", newPassword)
                put("returnSecureToken", true)
            }
            executePost(url, payload.toString())
            Unit
        }
    }

    /**
     * حذف حساب المستخدم من خادم المصادقة السحابي نهائياً
     */
    suspend fun deleteAccount(
        apiKey: String,
        idToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${CloudConfig.AUTH_BASE_URL}/accounts:delete?key=$apiKey"
            val payload = JSONObject().apply {
                put("idToken", idToken)
            }
            executePost(url, payload.toString())
            Unit
        }
    }

    /**
     * تجديد الرمز المميز للأمان (Token Refresh) عند انتهاء صلاحيته
     */
    suspend fun refreshToken(
        apiKey: String,
        refreshToken: String
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val urlStr = "${CloudConfig.TOKEN_REFRESH_BASE_URL}?key=$apiKey"
            val postData = "grant_type=refresh_token&refresh_token=$refreshToken"

            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

            connection.outputStream.use { os ->
                os.write(postData.toByteArray(StandardCharsets.UTF_8))
            }

            val statusCode = connection.responseCode
            if (statusCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val json = JSONObject(responseText)
                val newIdToken = json.getString("id_token")
                val newRefreshToken = json.getString("refresh_token")
                Pair(newIdToken, newRefreshToken)
            } else {
                val errorText = connection.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
                throw IllegalStateException(parseErrorMessage(errorText))
            }
        }
    }

    /**
     * تنفيذ طلب POST عبر HttpsURLConnection مع معالجة الأخطاء وترجمتها إلى العربية
     */
    private fun executePost(urlStr: String, jsonBody: String): JSONObject {
        val url = URL(urlStr)
        val connection = (url.openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }

        connection.outputStream.use { os ->
            os.write(jsonBody.toByteArray(StandardCharsets.UTF_8))
        }

        val statusCode = connection.responseCode
        if (statusCode in 200..299) {
            val responseText = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            return JSONObject(responseText)
        } else {
            val errorText = connection.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            throw IllegalStateException(parseErrorMessage(errorText))
        }
    }

    /**
     * ترجمة رسائل أخطاء خادم السحابة إلى نصوص باللغة العربية الفصحى السليمة
     */
    private fun parseErrorMessage(rawErrorJson: String): String {
        return try {
            val json = JSONObject(rawErrorJson)
            val errorObj = json.optJSONObject("error")
            val message = errorObj?.optString("message", "") ?: rawErrorJson

            when {
                message.contains("EMAIL_EXISTS") ->
                    "هذا البريد الإلكتروني مسجل مسبقاً، يرجى تسجيل الدخول أو استخدام بريد آخر."
                message.contains("EMAIL_NOT_FOUND") ->
                    "لا يوجد حساب مسجل بعنوان البريد الإلكتروني هذا. يرجى إنشاء حساب جديد."
                message.contains("INVALID_PASSWORD") || message.contains("INVALID_LOGIN_CREDENTIALS") ->
                    "كلمة المرور أو البريد الإلكتروني غير صحيح، يرجى التحقق والمحاولة مجدداً."
                message.contains("WEAK_PASSWORD") ->
                    "كلمة المرور ضعيفة جداً، يجب ألا تقل عن 6 أحرف أو أرقام."
                message.contains("USER_DISABLED") ->
                    "تم تعطيل هذا الحساب من إدارة الخدمة."
                message.contains("TOO_MANY_ATTEMPTS_TRY_LATER") ->
                    "تم حظر المحاولات مؤقتاً بسبب تكرار المحاولات الخاطئة، يرجى المحاولة بعد قليل."
                message.contains("INVALID_EMAIL") ->
                    "صيغة البريد الإلكتروني المدخلة غير صالحة."
                message.contains("TOKEN_EXPIRED") || message.contains("USER_NOT_FOUND") ->
                    "انتهت صلاحية جلسة الأمان، يرجى تسجيل الدخول مجدداً."
                message.contains("API_KEY_INVALID") || message.contains("API key not valid") ->
                    "مفتاح السحابة غير صالح، يرجى التحقق من إعدادات مشروع Firebase."
                else -> "خطأ في الاتصال بالخادم السحابي: $message"
            }
        } catch (_: Exception) {
            "تعذر الاتصال بالخادم السحابي، يرجى التحقق من اتصال الإنترنت."
        }
    }
}
