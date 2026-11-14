package com.masroofi.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.masroofi.app.domain.model.CurrencyCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "masroofi_settings")

/**
 * خيارات مظهر التطبيق الثلاثة
 */
enum class ThemeMode {
    LIGHT,  // فاتح
    DARK,   // داكن
    SYSTEM  // تلقائي حسب إعدادات الجهاز
}

/**
 * واجهة إدارة التفضيلات والإعدادات المحلية لتطبيق مصروفي
 */
interface UserPreferences {
    val themeMode: Flow<ThemeMode>
    val isDarkMode: Flow<Boolean>
    val selectedCurrencyCode: Flow<String>
    val hasSelectedCurrency: Flow<Boolean>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDarkMode(isDark: Boolean)
    suspend fun setSelectedCurrencyCode(code: String)
    suspend fun setHasSelectedCurrency(hasSelected: Boolean)
    suspend fun setCloudCredentials(apiKey: String, projectId: String)
}

/**
 * مدير التفضيلات والإعدادات المحلية عبر DataStore
 * يحفظ تفضيلات المظهر والعملة الرئيسية وحالة التهيئة الأولية
 */
open class PreferencesManager(private val context: Context) : UserPreferences {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val SELECTED_CURRENCY_CODE = stringPreferencesKey("selected_currency_code")
        val HAS_SELECTED_CURRENCY = booleanPreferencesKey("has_selected_currency")
        val CLOUD_API_KEY = stringPreferencesKey("cloud_api_key")
        val CLOUD_PROJECT_ID = stringPreferencesKey("cloud_project_id")
    }

    /**
     * تدفق مفتاح واجهة برمجة تطبيقات السحابة (Firebase Web API Key)
     */
    val cloudApiKey: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.CLOUD_API_KEY] }

    /**
     * تدفق معرّف المشروع السحابي (Firebase Project ID)
     */
    val cloudProjectId: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.CLOUD_PROJECT_ID] }

    override suspend fun setCloudCredentials(apiKey: String, projectId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLOUD_API_KEY] = apiKey.trim()
            preferences[PreferencesKeys.CLOUD_PROJECT_ID] = projectId.trim()
        }
    }

    /**
     * تدفق كود العملة الرئيسية المختارة (افتراضياً: YER - الريال اليمني)
     */
    override val selectedCurrencyCode: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_CURRENCY_CODE] ?: CurrencyCatalog.DEFAULT_CURRENCY_CODE
        }

    /**
     * تدفق حالة ما إذا كان المستخدم قد اختار عملته سابقاً (للتمييز بين المستخدم الجديد والحالي)
     */
    override val hasSelectedCurrency: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.HAS_SELECTED_CURRENCY] ?: false
        }

    /**
     * حفظ كود العملة الرئيسية المختارة
     */
    override suspend fun setSelectedCurrencyCode(code: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_CURRENCY_CODE] = code.trim().uppercase()
        }
    }

    /**
     * تعيين حالة اختيار العملة الأولية
     */
    override suspend fun setHasSelectedCurrency(hasSelected: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SELECTED_CURRENCY] = hasSelected
        }
    }

    /**
     * تدفق حالة مظهر التطبيق (فاتح / داكن / تلقائي)
     */
    override val themeMode: Flow<ThemeMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val modeStr = preferences[PreferencesKeys.THEME_MODE]
            if (modeStr != null) {
                try {
                    ThemeMode.valueOf(modeStr)
                } catch (_: Exception) {
                    ThemeMode.SYSTEM
                }
            } else {
                val isDark = preferences[PreferencesKeys.IS_DARK_MODE]
                if (isDark != null) {
                    if (isDark) ThemeMode.DARK else ThemeMode.LIGHT
                } else {
                    ThemeMode.SYSTEM
                }
            }
        }

    /**
     * تدفق حالة الوضع الداكن للتوافق السريع
     */
    override val isDarkMode: Flow<Boolean> = themeMode.map { mode ->
        mode == ThemeMode.DARK
    }

    /**
     * حفظ خيار مظهر التطبيق (فاتح، داكن، تلقائي) محلياً
     */
    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
            preferences[PreferencesKeys.IS_DARK_MODE] = (mode == ThemeMode.DARK)
        }
    }

    /**
     * حفظ خيار التبديل السريع (فاتح / داكن)
     */
    override suspend fun setDarkMode(isDark: Boolean) {
        setThemeMode(if (isDark) ThemeMode.DARK else ThemeMode.LIGHT)
    }
}
