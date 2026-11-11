package com.masroofi.app.data.backup

import android.content.Context
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * مدير تخزين وإدارة ملفات النسخ الاحتياطي وتصدير البيانات محلياً
 */
object BackupFileManager {

    /**
     * الحصول على مجلد النسخ الاحتياطية المحلي وإنشاؤه إن لم يكن موجوداً
     * مساره: /sdcard/Android/data/com.masroofi.app/files/backups
     */
    fun getBackupsDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * الحصول على مجلد تصدير الجداول CSV
     * مساره: /sdcard/Android/data/com.masroofi.app/files/exports
     */
    fun getExportsDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "exports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * سرد كافة ملفات النسخ الاحتياطي الموجودة محلياً مرتبة من الأحدث إلى الأقدم
     */
    fun listLocalBackups(context: Context): List<File> {
        val dir = getBackupsDirectory(context)
        return dir.listFiles { file ->
            file.isFile && file.name.endsWith(".json", ignoreCase = true)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * حفظ نص النسخة الاحتياطية مباشرة في مجلد النسخ الاحتياطية المحلي
     */
    fun saveBackupDirectly(context: Context, jsonContent: String, customFileName: String? = null): File {
        val dir = getBackupsDirectory(context)
        val timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val fileName = customFileName ?: "masroofi_backup_$timeStamp.json"
        val targetFile = File(dir, fileName)

        targetFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(jsonContent)
        }
        return targetFile
    }

    /**
     * حفظ نص جدول العمليات CSV مباشرة في مجلد التصدير المحلي
     */
    fun saveCsvDirectly(context: Context, csvContent: String, customFileName: String? = null): File {
        val dir = getExportsDirectory(context)
        val timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val fileName = customFileName ?: "masroofi_transactions_$timeStamp.csv"
        val targetFile = File(dir, fileName)

        targetFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(csvContent)
        }
        return targetFile
    }
}
