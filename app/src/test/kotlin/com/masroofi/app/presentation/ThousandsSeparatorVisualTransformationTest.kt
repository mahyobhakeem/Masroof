package com.masroofi.app.presentation

import androidx.compose.ui.text.AnnotatedString
import com.masroofi.app.presentation.components.ThousandsSeparatorVisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * اختبارات وحدة للمحول البصري لفواصل الآلاف في خانات إدخال الأرقام
 */
class ThousandsSeparatorVisualTransformationTest {

    private val transformation = ThousandsSeparatorVisualTransformation()

    @Test
    fun `empty string returns empty`() {
        val result = transformation.filter(AnnotatedString(""))
        assertEquals("", result.text.text)
    }

    @Test
    fun `numbers below one thousand have no commas`() {
        assertEquals("5", transformation.filter(AnnotatedString("5")).text.text)
        assertEquals("50", transformation.filter(AnnotatedString("50")).text.text)
        assertEquals("500", transformation.filter(AnnotatedString("500")).text.text)
    }

    @Test
    fun `thousands formatting produces correct commas`() {
        assertEquals("1,000", transformation.filter(AnnotatedString("1000")).text.text)
        assertEquals("12,345", transformation.filter(AnnotatedString("12345")).text.text)
        assertEquals("123,456", transformation.filter(AnnotatedString("123456")).text.text)
    }

    @Test
    fun `millions and billions formatting is correct`() {
        assertEquals("1,500,000", transformation.filter(AnnotatedString("1500000")).text.text)
        assertEquals("15,000,000", transformation.filter(AnnotatedString("15000000")).text.text)
        assertEquals("100,000,000", transformation.filter(AnnotatedString("100000000")).text.text)
        assertEquals("500,000,000,000", transformation.filter(AnnotatedString("500000000000")).text.text)
    }

    @Test
    fun `offset mapping bidirectional bounds safety`() {
        val input = "15000000"
        val result = transformation.filter(AnnotatedString(input))
        val transformed = result.text.text // "15,000,000"
        val mapping = result.offsetMapping

        // التحقق من أن كل موقع مؤشر في النص الأصلي ينتج موقعاً صالحاً ضمن حدود النص المنسق
        for (i in 0..input.length) {
            val transOffset = mapping.originalToTransformed(i)
            org.junit.Assert.assertTrue(
                "Original offset $i -> Transformed $transOffset must be in 0..${transformed.length}",
                transOffset in 0..transformed.length
            )
        }

        // التحقق من أن كل موقع مؤشر في النص المنسق ينتج موقعاً صالحاً ضمن حدود النص الأصلي
        for (t in 0..transformed.length) {
            val origOffset = mapping.transformedToOriginal(t)
            org.junit.Assert.assertTrue(
                "Transformed offset $t -> Original $origOffset must be in 0..${input.length}",
                origOffset in 0..input.length
            )
        }
    }
}
