package com.masroofi.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDarkMode(isDark: Boolean)
}

/**
 * مدير التفضيلات والإعدادات المحلية عبر DataStore
 * يقتصر دوره حصرياً على حفظ تفضيلات المظهر (الوضع الفاتح / الداكن / التلقائي)
 * بينما تحفظ الميزانيات والتصنيفات والعمليات في قاعدة بيانات Room المحلية
 */
open class PreferencesManager(private val context: Context) : UserPreferences {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
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
