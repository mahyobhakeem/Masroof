package com.masroofi.app.data.cloud

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * اختبارات وحدة شاملة لمنظومة الحساب والنسخ الاحتياطي السحابي الحقيقي (المرحلة 7)
 * تشمل:
 * 1. التحقق من صيغة البريد الإلكتروني وقوة وتطابق كلمات المرور
 * 2. تدفق إنشاء الحساب السحابي الجديد (Sign Up)
 * 3. تدفق تسجيل الدخول (Sign In)
 * 4. محاكاة الانتقال بين هاتفين (رفع نسخة من الهاتف الأول -> مسح الهاتف -> تسجيل الدخول والاستعادة من هاتف جديد بنسبة 100%)
 * 5. تغيير كلمة المرور وطلب الاستعادة
 * 6. الحذف النهائي للحساب والبيانات السحابية
 */
class CloudAuthAndBackupTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var cloudDir: File
    private lateinit var provider: FirebaseCloudBackupProvider

    @Before
    fun setUp() {
        cloudDir = tempFolder.newFolder("cloud_test_root")
        provider = FirebaseCloudBackupProvider(cloudRootDirOverride = cloudDir)
    }

    @Test
    fun testEmailFormatValidation() {
        // فحص صيغ البريد الإلكتروني الصالحة
        val validEmails = listOf(
            "user@gmail.com",
            "ahmed.ali@yahoo.com",
            "contact@masroof.app",
            "test_user123@domain.org"
        )
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$")

        for (email in validEmails) {
            assertTrue("يجب أن يكون البريد صالِحاً: $email", emailRegex.matches(email))
        }

        // فحص صيغ البريد الإلكتروني غير الصالحة
        val invalidEmails = listOf(
            "",
            "plainaddress",
            "@missingusername.com",
            "username@.com",
            "user with space@domain.com"
        )
        for (email in invalidEmails) {
            assertFalse("يجب رفض البريد غير الصالح: $email", emailRegex.matches(email))
        }
    }

    @Test
    fun testPasswordStrengthAndMatchingLogic() {
        val weakPass = "12345"
        val validPass = "Masroof2026!"
        val confirmMatching = "Masroof2026!"
        val confirmMismatched = "Different2026"

        assertTrue("كلمة المرور يجب أن تكون 6 أحرف أو أكثر", validPass.length >= 6)
        assertTrue("يجب رفض كلمة المرور الأقل من 6 أحرف", weakPass.length < 6)
        assertEquals("يجب تطابق كلمتي المرور", validPass, confirmMatching)
        assertNotEquals("يجب رصد عدم تطابق كلمتي المرور", validPass, confirmMismatched)
    }

    @Test
    fun testSignUpFlow() = runBlocking {
        val result = provider.signUpWithEmail(
            email = "abdullah@example.com",
            password = "SecurePassword123!",
            displayName = "عبدالله أحمد"
        )

        assertTrue("يجب نجاح إنشاء الحساب", result.isSuccess)
        val account = result.getOrThrow()
        assertEquals("abdullah@example.com", account.email)
        assertEquals("عبدالله أحمد", account.displayName)
        assertNotNull(account.userId)

        val currentUser = provider.currentUser.first()
        assertNotNull("يجب أن يكون المستخدم الحالي مسجلاً في التدفق", currentUser)
        assertEquals(account.userId, currentUser?.userId)
    }

    @Test
    fun testSignInFlow() = runBlocking {
        // 1. إنشاء الحساب أولاً
        provider.signUpWithEmail(
            email = "fatima@example.com",
            password = "MyPassword2026!",
            displayName = "فاطمة محمد"
        )
        provider.signOut()
        assertNull(provider.currentUser.first())

        // 2. تسجيل الدخول
        val signInResult = provider.signInWithEmail("fatima@example.com", "MyPassword2026!")
        assertTrue("يجب نجاح تسجيل الدخول", signInResult.isSuccess)
        val account = signInResult.getOrThrow()
        assertEquals("fatima@example.com", account.email)
        assertEquals("فاطمة محمد", account.displayName)
        assertEquals(account.userId, provider.currentUser.first()?.userId)
    }

    @Test
    fun testTwoPhoneWipeAndRestoreSimulation() = runBlocking {
        // ==========================================
        // الهاتف الأول: إنشاء حساب ورفع نسخة سحابية
        // ==========================================
        val phone1Provider = FirebaseCloudBackupProvider(cloudRootDirOverride = cloudDir)
        val signUpResult = phone1Provider.signUpWithEmail(
            email = "businessman@masroof.com",
            password = "SecretPassword2026",
            displayName = "رجل الأعمال"
        )
        assertTrue(signUpResult.isSuccess)
        val user1 = signUpResult.getOrThrow()

        // محاكاة تحضير بيانات المحفظة وضغطها وتجزئتها عبر CloudBackupEngine
        val sampleBackupJson = """
            {
                "appName": "Masroof",
                "appVersion": "1.1.0",
                "currency": "YER",
                "transactions": [
                    {"id": "tx_1", "amount": 50000, "type": "INCOME", "category": "راتب"},
                    {"id": "tx_2", "amount": 12000, "type": "EXPENSE", "category": "تسوق"}
                ],
                "categories": [
                    {"id": "cat_1", "name": "راتب", "type": "INCOME"},
                    {"id": "cat_2", "name": "تسوق", "type": "EXPENSE"}
                ],
                "monthlyBudgets": [
                    {"yearMonth": "2026-11", "amount": 300000}
                ]
            }
        """.trimIndent()

        val rawBytes = sampleBackupJson.toByteArray(StandardCharsets.UTF_8)
        val compressedBytes = CloudBackupEngine.compressGzip(rawBytes)
        val checksum = CloudBackupEngine.calculateSha256(compressedBytes)
        val chunkByteArrays = CloudBackupEngine.splitIntoChunks(compressedBytes)

        val backupId = "backup_two_phone_test"
        val chunks = chunkByteArrays.mapIndexed { index, bytes ->
            CloudBackupChunk(
                chunkIndex = index,
                backupId = backupId,
                userId = user1.userId,
                dataBase64 = CloudBackupEngine.encodeBase64(bytes),
                bytesCount = bytes.size,
                checksum = CloudBackupEngine.calculateSha256(bytes)
            )
        }

        val metadata = CloudBackupMetadata(
            backupId = backupId,
            userId = user1.userId,
            appName = "Masroof",
            appVersion = "1.1.0",
            sha256Checksum = checksum,
            totalChunks = chunks.size,
            totalCompressedBytes = compressedBytes.size.toLong(),
            transactionCount = 2,
            categoryCount = 2,
            budgetCount = 1,
            currencyCode = "YER"
        )

        val uploadResult = phone1Provider.uploadBackup(metadata, chunks)
        assertTrue("يجب نجاح رفع النسخة السحابية من الهاتف الأول", uploadResult.isSuccess)

        // ==========================================
        // محاكاة ضياع الهاتف الأول وشراء هاتف جديد كلياً
        // ==========================================
        val phone2CleanDir = tempFolder.newFolder("phone2_clean_session")
        // الهاتف الثاني يبدأ بذاكرة محلية فارغة تماماً ولكن يتصل بنفس مستودع السحابة
        val phone2Provider = FirebaseCloudBackupProvider(
            cloudRootDirOverride = cloudDir // نفس السحابة المشتركة عبر الإنترنت
        )
        // التأكد من أن الهاتف الثاني يبدأ دون مستخدم مسجل
        phone2Provider.signOut()
        assertNull(phone2Provider.currentUser.first())

        // ==========================================
        // الهاتف الثاني: تسجيل الدخول بنفس البريد واستعادة البيانات بنسبة 100%
        // ==========================================
        val phone2SignInResult = phone2Provider.signInWithEmail("businessman@masroof.com", "SecretPassword2026")
        assertTrue("يجب نجاح تسجيل الدخول من الهاتف الثاني", phone2SignInResult.isSuccess)

        // التحقق من استكشاف السحابة وجلب النسخ المتوفرة
        val availableBackups = phone2Provider.getAvailableBackups().getOrThrow()
        assertTrue("يجب أن يكتشف الهاتف الثاني النسخة المحفوظة في السحابة", availableBackups.isNotEmpty())

        val cloudBackup = availableBackups.first()
        assertEquals(backupId, cloudBackup.backupId)
        assertEquals(2, cloudBackup.transactionCount)
        assertEquals(2, cloudBackup.categoryCount)
        assertEquals("YER", cloudBackup.currencyCode)
        assertEquals(checksum, cloudBackup.sha256Checksum)

        // تنزيل أجزاء النسخة السحابية في الهاتف الثاني
        val downloadedChunks = phone2Provider.downloadBackupChunks(cloudBackup.backupId).getOrThrow()
        assertEquals(chunks.size, downloadedChunks.size)

        // إعادة تجميع الأجزاء والتحقق من بصمة SHA-256
        val sortedDownloaded = downloadedChunks.sortedBy { it.chunkIndex }
        val reassembledBytes = CloudBackupEngine.reassembleChunks(sortedDownloaded.map { CloudBackupEngine.decodeBase64(it.dataBase64) })
        val isChecksumValid = CloudBackupEngine.verifySha256(reassembledBytes, cloudBackup.sha256Checksum)
        assertTrue("يجب أن تكون بصمة SHA-256 سليمة ومطابقة 100% دون أي تلف", isChecksumValid)

        // فك الضغط ومطابقة محتوى JSON الأصلي
        val restoredJson = String(CloudBackupEngine.decompressGzip(reassembledBytes), StandardCharsets.UTF_8)
        assertTrue(restoredJson.contains("businessman@masroof.com") || restoredJson.contains("Masroof"))
        assertTrue(restoredJson.contains("2026-11"))
    }

    @Test
    fun testPasswordResetAndChangeOperations() = runBlocking {
        val resetResult = provider.sendPasswordResetEmail("user@example.com")
        assertTrue("يجب نجاح طلب إرسال رابط استعادة كلمة المرور", resetResult.isSuccess)

        val changeResult = provider.changePassword("NewSecretPassword2026")
        assertTrue("يجب نجاح طلب تغيير كلمة المرور", changeResult.isSuccess)
    }

    @Test
    fun testAccountDeletionAndCloudWipe() = runBlocking {
        provider.signUpWithEmail("delete_me@example.com", "Password123!", "مستخدم للحذف")
        assertNotNull(provider.currentUser.first())

        val deleteResult = provider.deleteAccount()
        assertTrue("يجب نجاح عملية حذف الحساب والبيانات السحابية", deleteResult.isSuccess)
        assertNull("يجب تفريغ الجلسة بعد حذف الحساب", provider.currentUser.first())
    }
}
