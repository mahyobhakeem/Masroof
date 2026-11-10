package com.masroofi.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "masroofi_settings")

/**
 * مدير التفضيلات والإعدادات المحلية عبر DataStore
 * يقتصر دوره حصرياً على حفظ تفضيلات المظهر (الوضع الفاتح / الداكن)
 * بينما تحفظ الميزانيات والعمليات في قاعدة بيانات Room المحلية
 */
class PreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }

    /**
     * تدفق حالة مظهر التطبيق (الوضع الفاتح / الداكن)
     */
    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] ?: false
        }

    /**
     * حفظ خيار مظهر التطبيق محلياً
     */
    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDark
        }
    }
}
