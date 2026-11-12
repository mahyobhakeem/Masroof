package com.masroofi.app.data.cloud

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.masroofi.app.MasroofiApplication
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * مهمة WorkManager المجدولة لتنفيذ النسخ الاحتياطي السحابي التلقائي في الخلفية
 * عند توفر الاتصال بالإنترنت
 */
class CloudBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "masroof_cloud_backup_periodic_work"

        /**
         * جدولة النسخ الاحتياطي السحابي الدوري التلقائي (كل 24 ساعة) عند توفر الإنترنت
         */
        fun schedulePeriodicBackup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<CloudBackupWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? MasroofiApplication ?: return Result.failure()
        val cloudManager = app.container.cloudBackupManager

        val isAutoEnabled = cloudManager.provider.isAutoBackupEnabled.first()
        val user = cloudManager.provider.currentUser.first()

        if (!isAutoEnabled || user == null) {
            return Result.success()
        }

        val backupResult = cloudManager.createAndUploadCloudBackup()
        return if (backupResult.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
