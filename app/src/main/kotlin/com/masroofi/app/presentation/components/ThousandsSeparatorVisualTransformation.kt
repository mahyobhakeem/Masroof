package com.masroofi.app.presentation.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * محول بصري لعرض فواصل الآلاف (,) بين كل ثلاثة أرقام تلقائياً في خانات إدخال الأرقام
 * يتيح للمستخدم رؤية الأرقام منسقة (مثل: 15,000,000) مع بقاء القيمة الخام أرقاماً نقية
 */
class ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty() || !originalText.all { it.isDigit() }) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val originalLength = originalText.length
        val formatted = buildString {
            for (i in 0 until originalLength) {
                append(originalText[i])
                val remainingDigits = originalLength - 1 - i
                if (remainingDigits > 0 && remainingDigits % 3 == 0) {
                    append(',')
                }
            }
        }

        // بناء مصفوفات التخطيط الدقيق لموقع المؤشر بين النص الأصلي والمنسق
        val origToTrans = IntArray(originalLength + 1)
        var commasCount = 0
        for (i in 0 until originalLength) {
            origToTrans[i] = i + commasCount
            val remainingDigits = originalLength - 1 - i
            if (remainingDigits > 0 && remainingDigits % 3 == 0) {
                commasCount++
            }
        }
        origToTrans[originalLength] = originalLength + commasCount

        val transToOrig = IntArray(formatted.length + 1)
        var origIndex = 0
        for (i in 0 until formatted.length) {
            transToOrig[i] = origIndex
            if (formatted[i] != ',') {
                origIndex++
            }
        }
        transToOrig[formatted.length] = originalLength

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, originalLength)
                return origToTrans[clamped]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                return transToOrig[clamped]
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
