package com.masroofi.app.presentation.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.presentation.components.CategoryIconHelper
import com.masroofi.app.presentation.components.MasroofiTextField
import com.masroofi.app.presentation.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AddTransactionScreen(
    viewModel: TransactionFormViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // إغلاق الشاشة تلقائياً عند نجاح الحفظ
    LaunchedEffect(uiState.isSavedSuccess) {
        if (uiState.isSavedSuccess) {
            onDismiss()
        }
    }

    // عرض رسائل التنبيه والخطأ إن وجدت
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    AddTransactionContent(
        uiState = uiState,
        onTypeChange = { viewModel.setType(it) },
        onAmountChange = { viewModel.setAmount(it) },
        onCategorySelect = { viewModel.setCategory(it) },
        onDateSelect = { viewModel.setDate(it) },
        onNoteChange = { viewModel.setNote(it) },
        onSaveClick = { viewModel.saveTransaction() },
        onDismiss = onDismiss,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionContent(
    uiState: TransactionFormUiState,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategorySelect: (Category) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // ترويسة الشاشة (إضافة أو تعديل)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (uiState.isEditing) "تعديل العملية المالية" else "إضافة عملية جديدة",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (uiState.isEditing) "تحديث بيانات وسجل العملية الحالية" else "سجل مصروفاً أو إيراداً جديداً في النظام",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // التبديل السلس بين مصروف وإيراد
            item {
                TransactionTypeToggle(
                    selectedType = uiState.type,
                    onToggle = onTypeChange
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // حقل إدخال المبلغ
            item {
                MasroofiTextField(
                    value = uiState.amount,
                    onValueChange = onAmountChange,
                    label = "المبلغ المالي",
                    placeholder = "0",
                    trailingText = "ريال يمني YER",
                    leadingIcon = Icons.Default.Payments,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.amountError != null,
                    supportingText = uiState.amountError
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            // قسم اختيار التصنيف
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اختيار التصنيف",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (uiState.categoryError != null) {
                        Text(
                            text = uiState.categoryError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.availableCategories, key = { it.id }) { category ->
                        val isSelected = uiState.selectedCategory?.id == category.id
                        val catColor = CategoryIconHelper.getColor(category.iconKey)
                        val catIcon = CategoryIconHelper.getIcon(category.iconKey)

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onCategorySelect(category) }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                    } else {
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    }
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(catColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // حقل تاريخ العملية المنسق مع منتقي التاريخ
            item {
                val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale.forLanguageTag("ar")) }
                val formattedDate = remember(uiState.transactionDate) {
                    uiState.transactionDate.format(dateFormatter)
                }

                Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                    MasroofiTextField(
                        value = formattedDate,
                        onValueChange = {},
                        label = "تاريخ المعاملة",
                        leadingIcon = Icons.Default.CalendarToday,
                        readOnly = true,
                        isError = uiState.dateError != null,
                        supportingText = uiState.dateError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // حقل ملاحظات وتفاصيل إضافية
            item {
                MasroofiTextField(
                    value = uiState.note,
                    onValueChange = onNoteChange,
                    label = "ملاحظات وتفاصيل إضافية (اختياري)",
                    placeholder = "اكتب أي تفاصيل أخرى عن هذه المعاملة...",
                    leadingIcon = Icons.Default.Description,
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(30.dp))
            }

            // زر الحفظ الرئيسي مع حماية ضد النقر المتكرر
            item {
                Button(
                    onClick = onSaveClick,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "جاري الحفظ...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isEditing) "حفظ التعديلات" else "حفظ العملية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    )
                ) {
                    Text(
                        text = "إلغاء وتراجع",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // منتقي التاريخ المعتمد مع منع اختيار تواريخ مستقبلية
    if (showDatePicker) {
        val todayUtcMillis = remember {
            LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        }
        val initialSelectedMillis = remember(uiState.transactionDate) {
            uiState.transactionDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // منع اختيار أي تاريخ في المستقبل
                    return utcTimeMillis <= todayUtcMillis
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedLocalDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            onDateSelect(selectedLocalDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("تأكيد", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// أزرار التبديل السلس بين مصروف وإيراد
@Composable
private fun TransactionTypeToggle(
    selectedType: TransactionType,
    onToggle: (TransactionType) -> Unit
) {
    val isExpense = selectedType == TransactionType.EXPENSE
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // زر مصروف
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggle(TransactionType.EXPENSE) },
                shape = RoundedCornerShape(10.dp),
                color = if (isExpense) ExpenseRed else Color.Transparent
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "مصروف",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpense) Color.White else TextSecondary
                    )
                }
            }

            // زر إيراد
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggle(TransactionType.INCOME) },
                shape = RoundedCornerShape(10.dp),
                color = if (!isExpense) IncomeGreen else Color.Transparent
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "إيراد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (!isExpense) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}
