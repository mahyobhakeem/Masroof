package com.masroofi.app.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.masroofi.app.presentation.components.LocalAppCurrency
import com.masroofi.app.presentation.theme.PrimaryGreen
import com.masroofi.app.presentation.theme.TextSecondary

/**
 * نافذة حوار معلومات التطبيق والإصدار (AboutAppDialog)
 */
@Composable
fun AboutAppDialog(
    onDismiss: () -> Unit
) {
    val activeCurrency = LocalAppCurrency.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // أيقونة التطبيق
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "مصروف | Masroof",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "الإصدار 1.1.0 • الحل المالي الذكي",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(18.dp))

                // بطاقات الميزات المحدثة للإصدار 1.1.0
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InfoFeatureRow(
                        icon = Icons.Default.AutoGraph,
                        title = "إدارة مالية شاملة وذكية",
                        subtitle = "تتبع المصروفات والإيرادات والميزانيات الشهرية وحماية الرصيد الصافي مع استعراض زمني مرن (يومي، أسبوعي، شهري، وتاريخ أو فترة مخصصة)."
                    )
                    InfoFeatureRow(
                        icon = Icons.Default.Payments,
                        title = "دعم 24 عملة عربية وعالمية",
                        subtitle = "يدعم جميع عملات الدول العربية والعملات الدولية (ISO 4217) مع تنسيق فواصل الآلاف التلقائي، والعملة المفعلة حالياً: ${activeCurrency.nameArabic} (${activeCurrency.symbolArabic})."
                    )
                    InfoFeatureRow(
                        icon = Icons.Default.CloudDone,
                        title = "عمل محلي 100% مع نسخ سحابي اختياري آمن",
                        subtitle = "يعمل التطبيق بالكامل دون إنترنت، مع إتاحة ربط حساب المستخدم (عبر البريد الإلكتروني وكلمة المرور) لرفع واستعادة النسخ الاحتياطية السحابية المشفرة على أي هاتف."
                    )
                    InfoFeatureRow(
                        icon = Icons.Default.PictureAsPdf,
                        title = "تقارير بيانية وتصدير احترافي",
                        subtitle = "مخططات تفاعلية وتصدير كشوفات حساب مالية منسقة بصيغة PDF وملفات Excel (CSV) ونسخ احتياطية كاملة بصيغة JSON."
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
