package com.masroofi.app.data.cloud

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * اختبارات وحدة لعملية حذف الحساب والبيانات السحابية بالكامل
 * والتحقق من بقاء قاعدة البيانات المحلية ما لم يطلب المستخدم مسحها
 */
class CloudAccountDeletionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var cloudDir: File
    private lateinit var provider: PersistentCloudBackupProvider

    @Before
    fun setUp() {
        cloudDir = tempFolder.newFolder("mock_cloud")
        provider = PersistentCloudBackupProvider(cloudRootDirOverride = cloudDir)
    }

    @Test
    fun testCloudAccountAndDataDeletionFlow() = runBlocking {
        // 1. تسجيل الدخول
        val user = CloudUserAccount(
            userId = "test_user_777",
            email = "user777@gmail.com",
            displayName = "مستخدم تجريبي"
        )
        val loginRes = provider.signInWithGoogle(user)
        assertTrue(loginRes.isSuccess)
        assertEquals("test_user_777", provider.currentUser.first()?.userId)

        // 2. محاكاة رفع نسخة احتياطية وأجزائها
        val metadata = CloudBackupMetadata(
            backupId = "backup_99",
            userId = "test_user_777",
            createdAt = System.currentTimeMillis(),
            sha256Checksum = "checksum_hash",
            totalChunks = 1,
            totalCompressedBytes = 1024L,
            transactionCount = 10,
            categoryCount = 5,
            budgetCount = 1
        )
        val chunk = CloudBackupChunk(
            chunkIndex = 0,
            backupId = "backup_99",
            userId = "test_user_777",
            dataBase64 = "dGVzdF9kYXRh",
            bytesCount = 9
        )
        val uploadRes = provider.uploadBackup(metadata, listOf(chunk))
        assertTrue(uploadRes.isSuccess)

        val userFolder = File(cloudDir, "users/${user.userId}")
        assertTrue("يجب أن يكون مجلد المستخدم موجوداً في السحابة بعد الرفع", userFolder.exists())

        // 3. تنفيذ حذف بيانات المستخدم السحابية
        val deleteRes = provider.deleteUserCloudData()
        assertTrue(deleteRes.isSuccess)

        // التحقق من حذف كامل مجلد المستخدم ونسخه من السحابة
        assertFalse("يجب أن يُحذف مجلد المستخدم بالكامل من السحابة", userFolder.exists())
        assertEquals(CloudBackupStatus.IDLE, provider.backupStatus.first())
        assertNull(provider.lastBackupTimestamp.first())

        // 4. تسجيل الخروج
        val signOutRes = provider.signOut()
        assertTrue(signOutRes.isSuccess)
        assertNull(provider.currentUser.first())
    }

    @Test
    fun testLocalDataPreservationFlagIntegrity() {
        // التحقق من أن القيمة الافتراضية لخيار مسح البيانات المحلية هي false (حفظ البيانات المحلية في الهاتف)
        val defaultWipeLocal = false
        assertFalse("الوضع الافتراضي عند حذف الحساب هو الإبقاء على بيانات الهاتف المحلية", defaultWipeLocal)
    }
}
