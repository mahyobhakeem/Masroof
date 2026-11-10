package com.masroofi.app.presentation.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.presentation.components.CategoryIconHelper
import com.masroofi.app.presentation.theme.*
import com.masroofi.app.utils.formatNumber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * نافذة سفلية أنيقة (ModalBottomSheet) لعرض التفاصيل الكاملة للعملية المالية
 * مع خيارات التعديل والحذف المؤكد
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsBottomSheet(
    transaction: TransactionItem,
    onDismiss: () -> Unit,
    onEdit: (TransactionItem) -> Unit,
    onDelete: (TransactionItem) -> Unit
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // شريط النوع والمبلغ الرئيسي
            val isExpense = transaction.type == TransactionType.EXPENSE
            val typeColor = if (isExpense) ExpenseRed else IncomeGreen
            val typeLabel = if (isExpense) "مصروف" else "إيراد"
            val categoryColor = CategoryIconHelper.getColor(transaction.categoryIconKey)
            val categoryIcon = CategoryIconHelper.getIcon(transaction.categoryIconKey)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // شارة نوع العملية
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = typeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = typeColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // المبلغ المالي بخط عريض وواضح
                Text(
                    text = "${if (isExpense) "-" else "+"} ${formatNumber(transaction.amount)} ريال",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // قسم التصنيف
            DetailInfoRow(
                icon = categoryIcon,
                iconColor = categoryColor,
                title = "التصنيف المالي",
                value = transaction.categoryName ?: "تصنيف عام"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // قسم تاريخ العملية
            DetailInfoRow(
                icon = Icons.Default.CalendarToday,
                iconColor = MaterialTheme.colorScheme.primary,
                title = "تاريخ المعاملة",
                value = transaction.transactionDate
            )

            // قسم الملاحظات (إن وجدت)
            if (!transaction.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                DetailInfoRow(
                    icon = Icons.Default.Description,
                    iconColor = CategoryTransport,
                    title = "الملاحظات والتفاصيل",
                    value = transaction.note
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // قسم توقيت التسجيل
            val timeFormatter = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()) }
            val formattedCreated = remember(transaction.createdAt) { timeFormatter.format(Date(transaction.createdAt)) }
            DetailInfoRow(
                icon = Icons.Default.Schedule,
                iconColor = TextSecondary,
                title = "وقت التسجيل في النظام",
                value = formattedCreated
            )

            Spacer(modifier = Modifier.height(26.dp))

            // أزرار الإجراءات (تعديل وحذف)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // زر تعديل العملية
                Button(
                    onClick = {
                        onDismiss()
                        onEdit(transaction)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تعديل",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // زر حذف العملية
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ExpenseRed
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = ExpenseRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حذف",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // نافذة تأكيد الحذف باللغة العربية
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "تأكيد حذف العملية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف هذه العملية بشكل نهائي؟ لا يمكن التراجع عن هذا الإجراء.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDismiss()
                        onDelete(transaction)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("نعم، حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun DetailInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
