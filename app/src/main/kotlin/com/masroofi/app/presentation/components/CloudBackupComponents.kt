package com.masroofi.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.masroofi.app.data.cloud.CloudBackupMetadata
import com.masroofi.app.data.cloud.CloudBackupStatus
import com.masroofi.app.data.cloud.CloudUserAccount
import com.masroofi.app.domain.model.CurrencyCatalog
import com.masroofi.app.presentation.theme.EmeraldLightBg
import com.masroofi.app.presentation.theme.EmeraldPrimary
import com.masroofi.app.presentation.theme.PrimaryGreen
import com.masroofi.app.presentation.theme.TextSecondary
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * بطاقة الحساب والنسخ الاحتياطي السحابي في شاشة الإعدادات
 * قابلة للطي والتوسيع مع ترويسة تفاعلية وملخص حالة وزر طي سفلي
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
    onDeleteAccountClick: (() -> Unit)? = null,
    onPrivacyPolicyClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isExpanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null
) {
    var localExpanded by remember { mutableStateOf(false) }
    val expandedState = isExpanded ?: localExpanded
    val rotationAngle by animateFloatAsState(targetValue = if (expandedState) -90f else 0f, label = "cloud_chevron")

    // ملخص الحالة التلقائي بجانب العنوان
    val statusSummary = remember(currentUser, backupStatus, lastBackupTime) {
        if (currentUser == null) {
            "غير متصل • اضغط للتسجيل والنسخ"
        } else {
            when (backupStatus) {
                CloudBackupStatus.UP_TO_DATE -> "محدّثة سحابياً (${currentUser.displayName})"
                CloudBackupStatus.PENDING -> "بيانات معلقة (${currentUser.displayName})"
                CloudBackupStatus.BACKING_UP -> "جاري المزامنة..."
                CloudBackupStatus.FAILED -> "فشل المزامنة • إعادة المحاولة لاحقاً"
                CloudBackupStatus.IDLE -> "جاهز للنسخ (${currentUser.displayName})"
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.5.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ترويسة البطاقة القابلة للنقر للطي والتوسيع
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (onExpandedChange != null) {
                            onExpandedChange(!expandedState)
                        } else {
                            localExpanded = !localExpanded
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الحساب والنسخ السحابي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = statusSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (currentUser != null && backupStatus == CloudBackupStatus.UP_TO_DATE) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCloudLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = PrimaryGreen
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = if (expandedState) "طي" else "توسيع",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle)
                    )
                }
            }

            AnimatedVisibility(visible = expandedState) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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

                            // خيار النسخ السحابي التلقائي مع زر التبديل الموحد MasroofSwitch
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
                                MasroofSwitch(
                                    checked = isAutoBackupEnabled,
                                    onCheckedChange = onAutoBackupToggle
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

                            // خيار حذف الحساب والبيانات السحابية
                            if (onDeleteAccountClick != null) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onDeleteAccountClick() }
                                        .padding(vertical = 4.dp, horizontal = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "حذف الحساب والبيانات السحابية",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // رابط سياسة الخصوصية
                    if (onPrivacyPolicyClick != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPrivacyPolicyClick() }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "سياسة الخصوصية وأمان البيانات",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
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
 * نافذة ورقة اعتماد واختيار حساب Google القياسية الرسمية
 */
@Composable
fun GoogleSignInDialog(
    onDismiss: () -> Unit,
    onConfirmSignIn: (CloudUserAccount) -> Unit
) {
    var isUsingOtherAccount by remember { mutableStateOf(false) }
    var defaultEmail by remember { mutableStateOf("masroof.user@gmail.com") }
    var defaultName by remember { mutableStateOf("مستخدم مصروف") }
    var customEmail by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }

    val activeEmail = if (isUsingOtherAccount) customEmail else defaultEmail
    val activeName = if (isUsingOtherAccount) customName else defaultName

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ترويسة Google الرسمية
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4285F4)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "تسجيل الدخول باستخدام Google",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "لمتابعة النسخ الاحتياطي السحابي المشفر في تطبيق مصروف",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // بطاقة اختيار الحساب الحالي المعتمد
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (!isUsingOtherAccount) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (!isUsingOtherAccount) 1.5.dp else 1.dp,
                        color = if (!isUsingOtherAccount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { isUsingOtherAccount = false }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // أفاتار الحساب
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A67A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = defaultName.firstOrNull()?.toString() ?: "م",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = defaultName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "حساب معتمد ومحمي",
                                    tint = Color(0xFF16A67A),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = defaultEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        if (!isUsingOtherAccount) {
                            Icon(
                                imageVector = Icons.Default.RadioButtonChecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // خيار استخدام حساب آخر
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isUsingOtherAccount) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isUsingOtherAccount) 1.5.dp else 1.dp,
                        color = if (isUsingOtherAccount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { isUsingOtherAccount = true }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "استخدام حساب آخر",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            if (isUsingOtherAccount) {
                                Icon(
                                    imageVector = Icons.Default.RadioButtonChecked,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isUsingOtherAccount) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("الاسم الكامل", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customEmail,
                                onValueChange = { customEmail = it },
                                label = { Text("عنوان البريد الإلكتروني (Gmail)", fontSize = 12.sp) },
                                placeholder = { Text("example@gmail.com", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // إشعار أمني في الأسفل حول خصوصية وحماية البيانات
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "لمتابعة استخدام حسابك، ستشارك Google اسمك وبريدك الإلكتروني مع تطبيق مصروف. يتم تشفير كافة النسخ الاحتياطية سحابياً بتقنية AES-256.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // زر المتابعة وزر الإلغاء
                Button(
                    onClick = {
                        val finalEmail = activeEmail.trim()
                        if (finalEmail.isNotBlank()) {
                            val uid = "user_${finalEmail.replace("@", "_").replace(".", "_")}"
                            onConfirmSignIn(
                                CloudUserAccount(
                                    userId = uid,
                                    email = finalEmail,
                                    displayName = activeName.ifBlank { "مستخدم مصروف" }
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = Color.White
                    ),
                    enabled = activeEmail.isNotBlank()
                ) {
                    Text(
                        text = if (!isUsingOtherAccount) "المتابعة باسم $defaultName" else "تسجيل الدخول والمتابعة",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إلغاء", color = TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
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

/**
 * نافذة سياسة الخصوصية الرسمية داخل التطبيق
 */
@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EmeraldLightBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "سياسة الخصوصية وأمان البيانات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "يلتزم تطبيق مصروف (Masroof) بأعلى معايير حماية الخصوصية والشفافية وفق المبادئ المعتمدة التالية:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PrivacySection(
                    title = "1. العمل المحلي أولاً (Offline-First):",
                    content = "يعمل تطبيق مصروف محلياً 100% دون حاجة إلى اتصال بالإنترنت. لا نجمع ولا نرسل أي بيانات مالية (المصروفات، الإيرادات، الميزانيات، أو التصنيفات) إلى أي خوادم افتراضياً. بياناتك محفوظة على ذاكرة هاتفك فقط."
                )

                PrivacySection(
                    title = "2. النسخ السحابي الاختياري عبر Google:",
                    content = "عندما يختار المستخدم تفعيل النسخ السحابي اختيارياً، يتم استخدام Firebase Authentication للتحقق من هوية حساب Google، وخدمة Cloud Firestore لحفظ النسخ بعد ضغطها بنظام GZIP وتشفيرها عبر اتصال HTTPS آمن لاستعادتها عند تغيير الهاتف."
                )

                PrivacySection(
                    title = "3. عزل البيانات وانعدام الوصول الخارجي:",
                    content = "بياناتك السحابية معزولة تماماً بقواعد أمان مشددة، ولا يمكن لأي طرف ثالث أو حتى مطوري التطبيق قراءة بياناتك أو الوصول إليها."
                )

                PrivacySection(
                    title = "4. عدم مشاركة البيانات إطلاقاً:",
                    content = "لا نقوم ببيع أو مشاركة أو تأجير بيانات المستخدمين مع أي شركات إعلانية أو جهات خارجية، والتطبيق خالٍ تماماً من متعقبات الإعلانات."
                )

                PrivacySection(
                    title = "5. حذف الحساب والبيانات السحابية:",
                    content = "يمكنك في أي وقت إيقاف النسخ التلقائي، أو حذف كافة النسخ السحابية وحسابك نهائياً من داخل التطبيق عبر خيار (حذف الحساب والبيانات السحابية) أو عبر صفحة الويب المخصصة لمتجر Google Play."
                )

                PrivacySection(
                    title = "6. التواصل والدعم:",
                    content = "لأي استفسار بشأن الخصوصية، تواصل معنا عبر: support@masroofapp.com"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = Color.White
                )
            ) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PrivacySection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp
        )
    }
}

/**
 * نافذة تأكيد حذف الحساب والبيانات السحابية (متطلب Google Play الإلزامي)
 */
@Composable
fun DeleteAccountConfirmationDialog(
    onDismiss: () -> Unit,
    onExportLocalFirst: () -> Unit,
    onConfirmDelete: (wipeLocal: Boolean) -> Unit
) {
    var wipeLocalData by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "حذف الحساب والبيانات السحابية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "سيتم حذف حسابك السحابي وكافة النسخ الاحتياطية السحابية وأجزائها من خوادم السحابة نهائياً ولا يمكن استرجاعها.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // زر تصدير نسخة محلية أولاً
                OutlinedButton(
                    onClick = onExportLocalFirst,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تصدير نسخة احتياطية محلية أولاً (JSON)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // خيار الإبقاء على البيانات المحلية أو مسحها أيضاً
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { wipeLocalData = !wipeLocalData }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = wipeLocalData,
                        onCheckedChange = { wipeLocalData = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
                    )
                    Column {
                        Text(
                            text = "مسح البيانات المحلية أيضاً من هذا الهاتف",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (wipeLocalData) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (wipeLocalData)
                                "تحذير: سيتم تفريغ التطبيق بالكامل ومسح كل السجلات من الهاتف!"
                            else
                                "الإبقاء على بياناتك في الهاتف ومتابعة استخدامه محلياً (موصى به).",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(wipeLocalData) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                )
            ) {
                Text("تأكيد الحذف النهائي", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
