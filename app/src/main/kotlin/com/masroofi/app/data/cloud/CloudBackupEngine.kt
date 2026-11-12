package com.masroofi.app.data.cloud

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * المحرك البرمجي المسؤول عن معالجة البيانات السحابية:
 * 1. ضغط البيانات وفك الضغط عبر GZIP لتقليل الحجم بأكثر من 80%
 * 2. احتساب بصمة التجزئة الرقمية SHA-256 والتحقق من سلامة البيانات ومقاومة التلف
 * 3. تقسيم البيانات المضغوطة الكبيرة إلى أجزاء (Chunks) لا تتجاوز 700KB لتناسب حدود Firestore
 * 4. إعادة تجميع الأجزاء مع التحقق الذري من سلامة البصمة قبل فك الضغط
 */
object CloudBackupEngine {

    /**
     * الحد الأقصى لحجم الجزء الواحد من النسخة السحابية (700 كيلوبايت)
     * ليتوافق بأمان مع سقف حجم المستند الواحد في Cloud Firestore (1 ميجابايت)
     */
    const val MAX_CHUNK_SIZE_BYTES: Int = 700 * 1024

    /**
     * ضغط مصفوفة البايتات باستخدام GZIP
     */
    fun compressGzip(data: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzip ->
            gzip.write(data)
            gzip.finish()
        }
        return outputStream.toByteArray()
    }

    /**
     * فك ضغط البيانات المضغوطة عبر GZIP
     */
    fun decompressGzip(compressedData: ByteArray): ByteArray {
        val inputStream = ByteArrayInputStream(compressedData)
        val outputStream = ByteArrayOutputStream()
        GZIPInputStream(inputStream).use { gzip ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (gzip.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
        }
        return outputStream.toByteArray()
    }

    /**
     * احتساب البصمة الرقمية المشفرة SHA-256 لأي بيانات
     */
    fun calculateSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * التحقق الصارم من تطابق بصمة SHA-256
     */
    fun verifySha256(data: ByteArray, expectedChecksum: String): Boolean {
        if (expectedChecksum.isBlank()) return false
        val calculated = calculateSha256(data)
        return calculated.equals(expectedChecksum.trim(), ignoreCase = true)
    }

    /**
     * تقسيم البيانات المضغوطة إلى أجزاء (Chunks) بحجم أقصى 700KB للجزء الواحد
     */
    fun splitIntoChunks(data: ByteArray, maxChunkBytes: Int = MAX_CHUNK_SIZE_BYTES): List<ByteArray> {
        require(maxChunkBytes > 0) { "حجم الجزء يجب أن يكون أكبر من الصفر" }
        if (data.isEmpty()) return listOf(ByteArray(0))

        val chunks = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < data.size) {
            val length = minOf(maxChunkBytes, data.size - offset)
            val chunk = ByteArray(length)
            System.arraycopy(data, offset, chunk, 0, length)
            chunks.add(chunk)
            offset += length
        }
        return chunks
    }

    /**
     * إعادة تجميع الأجزاء في مصفوفة بايتات واحدة متكاملة
     */
    fun reassembleChunks(chunks: List<ByteArray>): ByteArray {
        val totalSize = chunks.sumOf { it.size }
        val output = ByteArray(totalSize)
        var offset = 0
        for (chunk in chunks) {
            System.arraycopy(chunk, 0, output, offset, chunk.size)
            offset += chunk.size
        }
        return output
    }

    /**
     * تشفير البايتات إلى نص Base64
     */
    fun encodeBase64(data: ByteArray): String {
        return Base64.getEncoder().encodeToString(data)
    }

    /**
     * فك تشفير نص Base64 إلى مصفوفة بايتات
     */
    fun decodeBase64(base64Str: String): ByteArray {
        return Base64.getDecoder().decode(base64Str)
    }
}
