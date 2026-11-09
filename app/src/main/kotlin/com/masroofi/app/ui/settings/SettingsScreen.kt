package com.masroofi.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masroofi.app.model.MockData
import com.masroofi.app.ui.components.MasroofiExpandableCard
import com.masroofi.app.ui.components.MasroofiItemCard
import com.masroofi.app.ui.home.formatNumber
import com.masroofi.app.ui.theme.*

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // ترويسة الشاشة
        item {
            Text(
                text = "الإعدادات",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "تخصيص التطبيق والبيانات المحلية",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // بطاقة المظهر (فاتح / داكن)
        item {
            MasroofiItemCard(
                title = "مظهر التطبيق",
                subtitle = if (isDarkMode) "الوضع الداكن مفعل" else "الوضع الفاتح مفعل",
                icon = Icons.Default.DarkMode,
                iconColor = MaterialTheme.colorScheme.primary,
                trailingContent = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onToggleDarkMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.surface,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            )
        }

        // بطاقة الميزانية الشهرية
        item {
            MasroofiItemCard(
                title = "الميزانية الشهرية",
                subtitle = "${formatNumber(MockData.MONTHLY_BUDGET)} ريال يمني YER",
                icon = Icons.Default.AccountBalanceWallet,
                iconColor = IncomeGreen,
                onClick = { /* تعديل الميزانية */ }
            )
        }

        // بطاقة إدارة التصنيفات
        item {
            MasroofiItemCard(
                title = "إدارة التصنيفات",
                subtitle = "${MockData.allCategories.size} تصنيفات مالية مخصصة",
                icon = Icons.Default.Category,
                iconColor = CategoryFood,
                onClick = { /* فتح إدارة التصنيفات */ }
            )
        }

        // بطاقة تخصيص التطبيق (قسم قابل للتوسيع Expandable Section)
        item {
            MasroofiExpandableCard(
                title = "تخصيص التطبيق والعملة",
                icon = Icons.Default.Tune,
                iconColor = CategoryTransport
            ) {
                // عنصر فرعي 1: العملة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "العملة الافتراضية",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "الريال اليمني (YER)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.8.dp)

                // عنصر فرعي 2: بداية الأسبوع
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بداية الأسبوع المالي",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "يوم السبت",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.8.dp)

                // عنصر فرعي 3: تنبيهات تجاوز الميزانية
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تنبيه عند بلوغ 80% من الميزانية",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "مفعل محلياً",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
            }
        }

        // بطاقة النسخ الاحتياطي
        item {
            MasroofiItemCard(
                title = "النسخ الاحتياطي المحلي",
                subtitle = "حفظ نسخة من بياناتك في ذاكرة الهاتف دون إنترنت",
                icon = Icons.Default.CloudUpload,
                iconColor = CategoryBills,
                onClick = { /* نسخ احتياطي محلي */ }
            )
        }

        // بطاقة تصدير البيانات
        item {
            MasroofiItemCard(
                title = "تصدير البيانات",
                subtitle = "تصدير التقارير والعمليات بصيغة ملف محلي",
                icon = Icons.Default.FileDownload,
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = { /* تصدير البيانات */ }
            )
        }

        // بطاقة حول التطبيق
        item {
            MasroofiItemCard(
                title = "عن تطبيق مصروفي",
                subtitle = "تطبيق محلي 100% لإدارة المصروفات الشخصية",
                icon = Icons.Default.Info,
                iconColor = TextSecondary,
                onClick = { /* معلومات التطبيق */ }
            )
        }

        // تذييل الصفحة برقم الإصدار
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "مصروفي | Masroofi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "الإصدار 1.0.0 • يعمل محلياً دون اتصال",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
