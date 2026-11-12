package com.masroofi.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.masroofi.app.data.cloud.CloudBackupMetadata
import com.masroofi.app.data.cloud.CloudBackupStatus
import com.masroofi.app.data.cloud.CloudUserAccount
import com.masroofi.app.domain.model.CurrencyCatalog
import com.masroofi.app.presentation.theme.PrimaryGreen
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * بطاقة الحساب والنسخ الاحتياطي السحابي في شاشة الإعدادات
 */
@Composable
fun CloudBackupCard(
    currentUser: CloudUserAccount?,
    backupStatus: CloudBackupStatus,
    lastBackupTime: Long?,
    isAutoBackupEnabled: Boolean,
    isCloudLoading: Boolean,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onAutoBackupToggle: (Boolean) -> Unit,
    onBackupNowClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ترويسة البطاقة
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudSync,
                            contentDescription = "النسخ السحابي",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "الحساب والنسخ السحابي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentUser != null) "مربوط بحساب Google" else "نسخ احتياطي واستعادة عند تغيير الهاتف",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isCloudLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryGreen
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (currentUser == null) {
                // حالة ما قبل تسجيل الدخول
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "اربط حساب Google لحفظ نسخة احتياطية سحابية واستعادة بياناتك بأمان عند تغيير أو فقدان هاتفك.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )

                    Button(
                        onClick = onSignInClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تسجيل الدخول باستخدام Google",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // حالة ما بعد تسجيل الدخول
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // بيانات المستخدم المرتبط
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentUser.displayName.firstOrNull()?.toString() ?: "G",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Column {
                                Text(
                                    text = currentUser.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentUser.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(
                            onClick = onSignOutClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("خروج", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // شارة حالة المزامنة السحابية الدقيقة
                    CloudStatusBadge(status = backupStatus, lastBackupTime = lastBackupTime)

                    // خيار النسخ السحابي التلقائي
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "النسخ الاحتياطي السحابي التلقائي",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "نسخ تلقائي عند تعديل البيانات ومهمة يومية عند توفر الإنترنت",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAutoBackupEnabled,
                            onCheckedChange = onAutoBackupToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryGreen
                            )
                        )
                    }

                    // أزرار العمليات
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onBackupNowClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isCloudLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ الآن", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onRestoreClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isCloudLoading
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = PrimaryGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعادة", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * شارة توضح حالة النسخة السحابية بدقة وأمان
 */
@Composable
fun CloudStatusBadge(
    status: CloudBackupStatus,
    lastBackupTime: Long?,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, label) = when (status) {
        CloudBackupStatus.UP_TO_DATE -> {
            val formattedTime = formatTimestamp(lastBackupTime)
            Quadruple(
                Color(0xFFE8F5E9),
                Color(0xFF2E7D32),
                Icons.Filled.CheckCircle,
                "محدّثة سحابياً (آخر نسخة: $formattedTime)"
            )
        }
        CloudBackupStatus.PENDING -> {
            Quadruple(
                Color(0xFFFFF8E1),
                Color(0xFFF57F17),
                Icons.Filled.PendingActions,
                "توجد بيانات محلية معلقة بانتظار المزامنة"
            )
        }
        CloudBackupStatus.BACKING_UP -> {
            Quadruple(
                Color(0xFFE3F2FD),
                Color(0xFF1565C0),
                Icons.Filled.Sync,
                "جاري النسخ السحابي والضغط المشفر..."
            )
        }
        CloudBackupStatus.FAILED -> {
            Quadruple(
                Color(0xFFFFEBEE),
                Color(0xFFC62828),
                Icons.Filled.CloudOff,
                "فشل الاتصال بالسحابة — ستتم إعادة المحاولة تلقائياً"
            )
        }
        CloudBackupStatus.IDLE -> {
            Quadruple(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.onSurfaceVariant,
                Icons.Outlined.CloudQueue,
                "جاهز للنسخ السحابي الأول"
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * نافذة محاكاة تسجيل الدخول بحساب Google
 */
@Composable
fun GoogleSignInDialog(
    onDismiss: () -> Unit,
    onConfirmSignIn: (CloudUserAccount) -> Unit
) {
    var email by remember { mutableStateOf("masroof.user@gmail.com") }
    var name by remember { mutableStateOf("مستخدم مصروف") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "تسجيل الدخول بحساب Google",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "اختر حساب Google الذي تريد ربطه لحفظ واستعادة بياناتك في أي وقت ومن أي هاتف:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        val uid = "user_${email.replace("@", "_").replace(".", "_")}"
                        onConfirmSignIn(
                            CloudUserAccount(
                                userId = uid,
                                email = email.trim(),
                                displayName = name.ifBlank { "مستخدم مصروف" }
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = Color.White
                )
            ) {
                Text("تسجيل الدخول والمتابعة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * نافذة تأكيد استعادة البيانات من النسخة السحابية
 */
@Composable
fun CloudRestoreConfirmationDialog(
    metadata: CloudBackupMetadata,
    onDismiss: () -> Unit,
    onConfirmRestore: () -> Unit
) {
    val currency = CurrencyCatalog.getCurrencyByCode(metadata.currencyCode)
    val formattedDate = formatTimestamp(metadata.createdAt)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "استعادة البيانات من السحابة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "عُثر على أحدث نسخة احتياطية سحابية لحسابك بالتفاصيل التالية:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // بطاقة تفاصيل النسخة السحابية
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailRow("تاريخ النسخة:", formattedDate)
                        DetailRow("العملة المعتمدة:", "${currency.flagEmoji} ${currency.nameArabic} (${currency.symbolArabic})")
                        DetailRow("عدد العمليات المالية:", "${metadata.transactionCount} عملية")
                        DetailRow("عدد التصنيفات:", "${metadata.categoryCount} تصنيف")
                        DetailRow("الميزانيات الشهرية:", "${metadata.budgetCount} ميزانية")
                        DetailRow("الأجزاء وحجم الضغط:", "${metadata.totalChunks} أجزاء (${metadata.totalCompressedBytes / 1024} KB)")
                    }
                }

                // تنبيه الاستبدال الذري
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "تنبيه: سيقوم هذا الإجراء باستبدال البيانات المحلية بالنسخة السحابية بعد التحقق التام من البصمة الرقمية SHA-256.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFBF360C)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmRestore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = Color.White
                )
            ) {
                Text("تأكيد الاستعادة واستبدال البيانات", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return "غير متوفر"
    return try {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    } catch (_: Exception) {
        "غير متوفر"
    }
}
