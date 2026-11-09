package com.masroofi.app

import android.app.Application
import android.content.Context
import com.masroofi.app.data.preferences.PreferencesManager

/**
 * واجهة حاوية الاعتماديات المركزية لتطبيق مصروفي (AppContainer)
 * تضمن معمارية حقن اعتماديات يدوية نظيفة وخفيفة الوزن دون الحاجة لمكتبات معقدة
 */
interface AppContainer {
    val preferencesManager: PreferencesManager
}

/**
 * التنفيذ الافتراضي لحاوية الاعتماديات
 */
class DefaultAppContainer(private val context: Context) : AppContainer {
    override val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }
}

/**
 * الفئة المركزية للتطبيق المسجلة في ملف البيان AndroidManifest.xml
 */
class MasroofiApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
    }
}
