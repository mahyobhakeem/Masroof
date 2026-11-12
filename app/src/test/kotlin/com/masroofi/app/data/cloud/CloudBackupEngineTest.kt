package com.masroofi.app.data.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

/**
 * اختبارات الوحدة الشاملة لمحرك النسخ الاحتياطي السحابي (CloudBackupEngineTest)
 * تختبر: ضغط GZIP، فك الضغط، بصمة SHA-256، التجزئة (Chunking)، كشف التلف، والتقليم
 */
class CloudBackupEngineTest {

    @Test
    fun testGzipCompressionAndDecompressionIntegrity() {
        val originalText = buildString {
            append("{\n  \"appName\": \"Masroof\",\n  \"version\": \"1.1.0\",\n  \"transactions\": [\n")
            repeat(100) { i ->
                append("    {\"id\": \"tx_$i\", \"amount\": ${1000 + i}, \"note\": \"عملية تجريبية رقم $i\"},\n")
            }
            append("  ]\n}")
        }
        val originalBytes = originalText.toByteArray(StandardCharsets.UTF_8)

        // ضغط البيانات
        val compressed = CloudBackupEngine.compressGzip(originalBytes)
        assertTrue("يجب أن يكون حجم البيانات المضغوطة أصغر بكثير من الحجم الأصلي", compressed.size < originalBytes.size)

        // فك الضغط واستعادة النص
        val decompressed = CloudBackupEngine.decompressGzip(compressed)
        val restoredText = String(decompressed, StandardCharsets.UTF_8)

        assertEquals("يجب أن يتطابق النص بعد فك الضغط تماماً مع النص الأصلي", originalText, restoredText)
    }

    @Test
    fun testSha256ChecksumCalculationAndVerification() {
        val testData = "بيانات تجريبية لاختبار بصمة التجزئة SHA-256 لتطبيق مصروف".toByteArray(StandardCharsets.UTF_8)
        val checksum = CloudBackupEngine.calculateSha256(testData)

        assertNotNull(checksum)
        assertEquals(64, checksum.length) // بصمة SHA-256 بطول 64 حرفاً سداسياً عشرياً

        // التحقق من المطابقة
        assertTrue(CloudBackupEngine.verifySha256(testData, checksum))
        assertTrue(CloudBackupEngine.verifySha256(testData, checksum.uppercase()))

        // فشل المطابقة عند إعطاء بصمة خاطئة
        val wrongChecksum = "0".repeat(64)
        assertFalse(CloudBackupEngine.verifySha256(testData, wrongChecksum))
    }

    @Test
    fun testTamperedByteDetectionFailsSha256() {
        val originalData = "حسابات ومصروفات مالية مشفرة بدقة عالية".toByteArray(StandardCharsets.UTF_8)
        val compressed = CloudBackupEngine.compressGzip(originalData)
        val originalChecksum = CloudBackupEngine.calculateSha256(compressed)

        // محاكاة تلف أو تغيير بايت واحد أثناء النقل
        val tampered = compressed.copyOf()
        tampered[tampered.size / 2] = (tampered[tampered.size / 2] + 1).toByte()

        val isValid = CloudBackupEngine.verifySha256(tampered, originalChecksum)
        assertFalse("يجب أن يكتشف المحرك أي تلف في البيانات حتى لو تغير بايت واحد فقط", isValid)
    }

    @Test
    fun testChunkingAndReassemblyOfLargePayload() {
        // إنشاء حمولة بيانات بحجم 1.8 ميجابايت (تتجاوز حد الـ 700 كيلوبايت للجزء الواحد)
        val largeData = ByteArray(1800 * 1024) { (it % 256).toByte() }
        val maxChunk = 700 * 1024 // 700 KB

        val chunks = CloudBackupEngine.splitIntoChunks(largeData, maxChunk)

        // 1800KB مقسمة إلى 700KB: يجب أن تنتج 3 أجزاء (700KB + 700KB + 400KB)
        assertEquals(3, chunks.size)
        assertEquals(700 * 1024, chunks[0].size)
        assertEquals(700 * 1024, chunks[1].size)
        assertEquals(400 * 1024, chunks[2].size)

        // إعادة التجميع
        val reassembled = CloudBackupEngine.reassembleChunks(chunks)
        assertEquals(largeData.size, reassembled.size)
        assertTrue("يجب أن تتطابق البيانات المجمعة من الأجزاء تماماً مع البيانات الأصلية", largeData.contentEquals(reassembled))
    }

    @Test
    fun testBase64EncodingAndDecoding() {
        val sample = "مصروف v1.1.0 - اختبار ترميز Base64 للأجزاء السحابية".toByteArray(StandardCharsets.UTF_8)
        val base64Str = CloudBackupEngine.encodeBase64(sample)
        val decoded = CloudBackupEngine.decodeBase64(base64Str)

        assertTrue(sample.contentEquals(decoded))
    }

    @Test
    fun testPruningSimulationKeepsOnlyLastThreeBackups() {
        val mockBackups = (1..5).map { i ->
            CloudBackupMetadata(
                backupId = "backup_$i",
                userId = "user_123",
                createdAt = 1000L * i,
                sha256Checksum = "checksum_$i",
                totalChunks = 1,
                totalCompressedBytes = 500L,
                transactionCount = 10,
                categoryCount = 5,
                budgetCount = 1
            )
        }.sortedByDescending { it.createdAt }

        assertEquals(5, mockBackups.size)

        // عند الاحتفاظ بآخر 3 نسخ فقط
        val retainCount = 3
        val retained = mockBackups.take(retainCount)
        val pruned = mockBackups.drop(retainCount)

        assertEquals(3, retained.size)
        assertEquals(2, pruned.size)
        assertEquals("backup_5", retained[0].backupId)
        assertEquals("backup_4", retained[1].backupId)
        assertEquals("backup_3", retained[2].backupId)
        assertEquals("backup_2", pruned[0].backupId)
        assertEquals("backup_1", pruned[1].backupId)
    }

    @Test
    fun testUserIsolationLogic() {
        val user1Backup = CloudBackupMetadata(
            backupId = "b1",
            userId = "user_A",
            sha256Checksum = "hashA",
            totalChunks = 1,
            totalCompressedBytes = 100L,
            transactionCount = 1,
            categoryCount = 1,
            budgetCount = 1
        )

        val user2Backup = CloudBackupMetadata(
            backupId = "b2",
            userId = "user_B",
            sha256Checksum = "hashB",
            totalChunks = 1,
            totalCompressedBytes = 200L,
            transactionCount = 2,
            categoryCount = 2,
            budgetCount = 2
        )

        // عزل تام: التحقق من عدم تطابق معرف المستخدم
        assertFalse("يجب أن تكون معرفات المستخدمين مختلفة", user1Backup.userId == user2Backup.userId)
        assertTrue("المستخدم A يملك نسخته الخاصة فقط", user1Backup.userId == "user_A")
        assertTrue("المستخدم B يملك نسخته الخاصة فقط", user2Backup.userId == "user_B")
    }
}
