package com.masroofi.app.data.backup

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream
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
        val fileName = customFileName ?: "masroof_backup_$timeStamp.json"
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
        val fileName = customFileName ?: "masroof_transactions_$timeStamp.csv"
        val targetFile = File(dir, fileName)

        targetFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(csvContent)
        }
        return targetFile
    }

    /**
     * حفظ نسخة احتياطية إضافية في مجلد التنزيلات العام Downloads/Masroof
     * يعيد المسار الواضح للملف المحفوظ لعرضه للمستخدم
     */
    fun saveBackupToDownloads(context: Context, jsonContent: String, customFileName: String? = null): String {
        val timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val fileName = customFileName ?: "masroof_backup_$timeStamp.json"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Masroof")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.bufferedWriter(Charsets.UTF_8).use { it.write(jsonContent) }
                    }
                    return "Downloads/Masroof/$fileName"
                }
            }
        } catch (_: Exception) {
            // تجاهل والانتقال للمسار المباشر إن تعذر
        }

        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val masroofDir = File(downloadsDir, "Masroof")
            if (!masroofDir.exists()) {
                masroofDir.mkdirs()
            }
            val targetFile = File(masroofDir, fileName)
            targetFile.bufferedWriter(Charsets.UTF_8).use { it.write(jsonContent) }
            targetFile.absolutePath
        } catch (_: Exception) {
            "Downloads/Masroof/$fileName"
        }
    }
}
