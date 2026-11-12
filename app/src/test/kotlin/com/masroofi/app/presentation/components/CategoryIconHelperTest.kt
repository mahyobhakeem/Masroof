package com.masroofi.app.presentation.components

import androidx.compose.ui.graphics.Color
import com.masroofi.app.presentation.theme.EmeraldDark
import com.masroofi.app.presentation.theme.EmeraldDarkBg
import com.masroofi.app.presentation.theme.EmeraldLightBg
import com.masroofi.app.presentation.theme.EmeraldPrimary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * اختبارات وحدة للتحقق الصارم من توحيد هوية ألوان الأيقونات باللون الأخضر الزمردي
 * ومنع الألوان المشتتة (الأحمر والبرتقالي والبنفسجي والأزرق)
 */
class CategoryIconHelperTest {

    @Test
    fun testAllCategoryIconsReturnEmeraldPrimaryColor() {
        val testIcons = CategoryIconHelper.selectableIcons + listOf("unknown_icon", null, "custom_tag")

        for (iconKey in testIcons) {
            val color = CategoryIconHelper.getColor(iconKey)
            assertEquals(
                "يجب أن تستخدم الأيقونة $iconKey اللون الأخضر الزمردي الموحد PrimaryGreen (#16A67A)",
                EmeraldPrimary,
                color
            )
        }
    }

    @Test
    fun testCategoryIconsNeverReturnDisruptiveColors() {
        val prohibitedColors = listOf(
            Color(0xFFE76F75), // ExpenseRed
            Color(0xFFF04438), // Red
            Color(0xFFF79009), // Food Orange
            Color(0xFF2E90FA), // Bills Blue
            Color(0xFF7A5AF8), // Transport Purple
            Color(0xFFEE46BC)  // Shopping Pink
        )

        for (iconKey in CategoryIconHelper.selectableIcons) {
            val color = CategoryIconHelper.getColor(iconKey)
            for (forbidden in prohibitedColors) {
                assertNotEquals(
                    "يُمنع منعاً باتاً استخدام الألوان التحذيرية أو المشتتة لأيقونة $iconKey",
                    forbidden,
                    color
                )
            }
        }
    }

    @Test
    fun testDarkModeColorAndBackgroundIntegrity() {
        // الوضع الفاتح
        assertEquals(EmeraldPrimary, CategoryIconHelper.getColor(isDarkTheme = false))
        assertEquals(EmeraldLightBg, CategoryIconHelper.getBackgroundColor(isDarkTheme = false))

        // الوضع الداكن
        assertEquals(EmeraldDark, CategoryIconHelper.getColor(isDarkTheme = true))
        assertEquals(EmeraldDarkBg, CategoryIconHelper.getBackgroundColor(isDarkTheme = true))
    }
}
