package com.masroofi.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.masroofi.app.presentation.theme.PrimaryGreen
import com.masroofi.app.presentation.theme.TextSecondary
import com.masroofi.app.utils.FormatUtils
import java.time.LocalDate

/**
 * خيارات الفترة الزمنية لتصدير تقرير PDF
 */
enum class PdfPeriodMode(val title: String) {
    TODAY("اليوم"),
    SPECIFIC_DAY("يوم محدد"),
    THIS_WEEK("هذا الأسبوع"),
    THIS_MONTH("هذا الشهر"),
    PREVIOUS_MONTH("الشهر السابق"),
    CUSTOM_RANGE("فترة مخصصة")
}

/**
 * فلترة نوع العمليات المشمولة في تقرير PDF
 */
enum class TransactionTypeFilter(val title: String) {
    ALL("المصروفات والإيرادات معاً"),
    EXPENSE_ONLY("المصروفات فقط"),
    INCOME_ONLY("الإيرادات فقط")
}

/**
 * نافذة تخصيص وتوليد تقرير PDF المالي
 */
@Composable
fun PdfExportDialog(
    initialOwnerName: String = "",
    onDismiss: () -> Unit,
    onConfirmExport: (
        periodMode: PdfPeriodMode,
        specificDate: LocalDate?,
        customStartDate: LocalDate?,
        customEndDate: LocalDate?,
        typeFilter: TransactionTypeFilter,
        ownerName: String
    ) -> Unit
) {
    var ownerName by remember { mutableStateOf(initialOwnerName) }
    var selectedPeriodMode by remember { mutableStateOf(PdfPeriodMode.THIS_MONTH) }
    var specificDate by remember { mutableStateOf(LocalDate.now()) }
    var customStartDate by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var customEndDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTypeFilter by remember { mutableStateOf(TransactionTypeFilter.ALL) }

    var isDatePickerOpen by remember { mutableStateOf(false) }
    var isRangePickerOpen by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .imePadding(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ترويسة النافذة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تصدير تقرير مالي (PDF)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "توليد مستند مالي منسق جاهز للطباعة والمشاركة",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // حقل اسم صاحب التقرير (يظهر في ملف PDF)
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("اسم صاحب التقرير (يظهر في ملف PDF)") },
                    placeholder = { Text("مثال: عبد الرحمن السعيد") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = PrimaryGreen)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        focusedLabelColor = PrimaryGreen,
                        cursorColor = PrimaryGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. قسم اختيار الفترة الزمنية
                Text(
                    text = "الفترة الزمنية للتقرير:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // شبكة خيارات الفترات
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val periods = PdfPeriodMode.values().toList()
                    periods.chunked(2).forEach { rowPeriods ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPeriods.forEach { period ->
                                val isSelected = selectedPeriodMode == period
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedPeriodMode = period },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) PrimaryGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = period.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            if (rowPeriods.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // تخصيص اليوم المحدد إن تم اختياره
                AnimatedVisibility(visible = selectedPeriodMode == PdfPeriodMode.SPECIFIC_DAY) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isDatePickerOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = FormatUtils.formatArabicDate(specificDate, includeDayOfWeek = true),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text("تغيير", style = MaterialTheme.typography.bodySmall, color = PrimaryGreen)
                            }
                        }
                    }
                }

                // تخصيص الفترة المخصصة إن تم اختيارها
                AnimatedVisibility(visible = selectedPeriodMode == PdfPeriodMode.CUSTOM_RANGE) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isRangePickerOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = FormatUtils.formatDateRange(customStartDate, customEndDate),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text("تغيير", style = MaterialTheme.typography.bodySmall, color = PrimaryGreen)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. قسم فلترة نوع العمليات
                Text(
                    text = "نوع العمليات المشمولة:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransactionTypeFilter.values().forEach { filter ->
                        val isSelected = selectedTypeFilter == filter
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTypeFilter = filter },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PrimaryGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedTypeFilter = filter },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = filter.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // أزرار الإجراءات (تصدير وإلغاء)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onConfirmExport(
                                selectedPeriodMode,
                                if (selectedPeriodMode == PdfPeriodMode.SPECIFIC_DAY) specificDate else null,
                                if (selectedPeriodMode == PdfPeriodMode.CUSTOM_RANGE) customStartDate else null,
                                if (selectedPeriodMode == PdfPeriodMode.CUSTOM_RANGE) customEndDate else null,
                                selectedTypeFilter,
                                ownerName.trim()
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "تصدير التقرير الآن",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.7f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }

    if (isDatePickerOpen) {
        MasroofDatePickerDialog(
            initialDate = specificDate,
            onDateSelected = { specificDate = it },
            onDismiss = { isDatePickerOpen = false },
            title = "اختيار يوم التقرير"
        )
    }

    if (isRangePickerOpen) {
        MasroofDateRangeDialog(
            initialStartDate = customStartDate,
            initialEndDate = customEndDate,
            onRangeSelected = { start, end ->
                customStartDate = start
                customEndDate = end
            },
            onDismiss = { isRangePickerOpen = false }
        )
    }
}
