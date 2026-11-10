package com.masroofi.app.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.masroofi.app.domain.model.MonthlyBudget
import com.masroofi.app.presentation.components.MasroofiTextField
import com.masroofi.app.presentation.theme.IncomeGreen
import com.masroofi.app.presentation.theme.PrimaryGreen
import com.masroofi.app.presentation.theme.TextSecondary
import com.masroofi.app.utils.FormatUtils
import com.masroofi.app.utils.formatNumber
import java.time.YearMonth

/**
 * شاشة إدارة الميزانية الشهرية وسجل الميزانيات للأشهر السابقة (BudgetManagementSheet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetManagementSheet(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onOpenEditBudget: (MonthlyBudget?) -> Unit,
    onCloseEditBudget: () -> Unit,
    onSaveBudget: (yearMonth: String, amount: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYearMonth = YearMonth.now()
    val currentYearMonthStr = currentYearMonth.toString()
    val currentBudget = uiState.allBudgets.find { it.yearMonth == currentYearMonthStr }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "إدارة الميزانية الشهرية",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // بطاقة ميزانية الشهر الحالي
            item {
                CurrentMonthBudgetCard(
                    yearMonth = currentYearMonth,
                    currentAmount = currentBudget?.amount,
                    onEditClick = {
                        onOpenEditBudget(currentBudget ?: MonthlyBudget(id = "", yearMonth = currentYearMonthStr, amount = 0L))
                    }
                )
            }

            // ترويسة سجل الميزانيات السابقة
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل ميزانيات الأشهر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${uiState.allBudgets.size} أشهر مسجلة",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            if (uiState.allBudgets.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "لا توجد ميزانيات مسجلة بعد",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(uiState.allBudgets, key = { it.yearMonth }) { budget ->
                    BudgetHistoryRow(
                        budget = budget,
                        isCurrentMonth = budget.yearMonth == currentYearMonthStr,
                        onEdit = { onOpenEditBudget(budget) }
                    )
                }
            }
        }
    }

    // نافذة تعديل الميزانية
    if (uiState.showEditBudgetDialog && uiState.budgetToEdit != null) {
        EditBudgetDialog(
            budget = uiState.budgetToEdit,
            onDismiss = onCloseEditBudget,
            onSave = onSaveBudget
        )
    }
}

/**
 * بطاقة عرض ميزانية الشهر الحالي
 */
@Composable
private fun CurrentMonthBudgetCard(
    yearMonth: YearMonth,
    currentAmount: Long?,
    onEditClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(IncomeGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ميزانية الشهر الحالي",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = FormatUtils.formatYearMonth(yearMonth),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (currentAmount != null) "تعديل" else "تحديد")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المبلغ المعتمد:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = if (currentAmount != null) "${formatNumber(currentAmount)} ريال" else "غير محددة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (currentAmount != null) PrimaryGreen else TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * صف عرض ميزانية شهر في السجل
 */
@Composable
private fun BudgetHistoryRow(
    budget: MonthlyBudget,
    isCurrentMonth: Boolean,
    onEdit: () -> Unit
) {
    val parsedYm = try {
        YearMonth.parse(budget.yearMonth)
    } catch (_: Exception) {
        null
    }
    val monthLabel = parsedYm?.let { FormatUtils.formatYearMonth(it) } ?: budget.yearMonth

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isCurrentMonth) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "الشهر الحالي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${formatNumber(budget.amount)} ريال يمني",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "تعديل الميزانية",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * نافذة حوار تعديل أو تحديد ميزانية شهر معين
 */
@Composable
private fun EditBudgetDialog(
    budget: MonthlyBudget,
    onDismiss: () -> Unit,
    onSave: (yearMonth: String, amount: Long) -> Unit
) {
    var amountText by remember { mutableStateOf(if (budget.amount > 0) budget.amount.toString() else "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val parsedYm = try {
        YearMonth.parse(budget.yearMonth)
    } catch (_: Exception) {
        null
    }
    val monthTitle = parsedYm?.let { FormatUtils.formatYearMonth(it) } ?: budget.yearMonth

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "تحديد ميزانية $monthTitle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "أدخل الحد الأقصى للمصروفات المخططة لهذا الشهر بالريال اليمني",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                MasroofiTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        amountText = digits
                        if (digits.isNotEmpty()) errorText = null
                    },
                    label = "الميزانية الشهرية (ريال يمني)",
                    placeholder = "مثال: 150000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorText != null,
                    supportingText = errorText
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toLongOrNull()
                            if (amount == null || amount < 0) {
                                errorText = "يرجى إدخال مبلغ صحيح أكبر من أو يساوي الصفر"
                                return@Button
                            }
                            onSave(budget.yearMonth, amount)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ الميزانية")
                    }
                }
            }
        }
    }
}
