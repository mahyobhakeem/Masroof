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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.presentation.components.CategoryIconHelper
import com.masroofi.app.presentation.components.MasroofiTextField
import com.masroofi.app.presentation.theme.*
import com.masroofi.app.utils.formatNumber

@Composable
fun TransactionsHistoryScreen(
    viewModel: TransactionsHistoryViewModel,
    onEditTransaction: (TransactionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    TransactionsHistoryContent(
        uiState = uiState,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onFilterIndexChange = { viewModel.setFilterIndex(it) },
        onCategorySelect = { viewModel.setSelectedCategoryId(it) },
        onDatePeriodSelect = { viewModel.setDatePeriod(it) },
        onResetFilters = { viewModel.resetFilters() },
        onEditTransaction = onEditTransaction,
        onDeleteTransaction = { viewModel.deleteTransaction(it.id) },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@Composable
fun TransactionsHistoryContent(
    uiState: TransactionsHistoryUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterIndexChange: (Int) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onDatePeriodSelect: (DateRangeFilter) -> Unit,
    onResetFilters: () -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    onDeleteTransaction: (TransactionItem) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val filterTabs = listOf("الكل", "المصروفات", "الإيرادات")
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionItem?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionItem?>(null) }

    val groupedTransactions = remember(uiState.transactions) {
        uiState.transactions.groupBy { it.transactionDate }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            // ترويسة الشاشة
            item {
                Text(
                    text = "سجل العمليات",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "عرض وتصفية وبحث جميع العمليات المالية في سجل موحد",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // بطاقة ملخص النتائج المصفاة
            item {
                FilteredResultsSummaryCard(
                    totalExpenses = uiState.totalExpenses,
                    totalIncome = uiState.totalIncome,
                    netTotal = uiState.netTotal,
                    count = uiState.transactions.size
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // شريط البحث المعتمد مع زر المسح
            item {
                MasroofiTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = "بحث في العمليات",
                    placeholder = "ابحث بالمبلغ أو الملاحظة أو اسم التصنيف...",
                    leadingIcon = Icons.Default.Search,
                    trailingText = if (uiState.searchQuery.isNotEmpty()) "مسح" else null,
                    onClick = null
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ألسنة تصفية نوع العملية (الكل / المصروفات / الإيرادات)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterTabs.forEachIndexed { index, tabName ->
                        val isSelected = uiState.selectedFilterIndex == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onFilterIndexChange(index) }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                    } else {
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    }
                                ),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // شريط تصفية الفترة الزمنية
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(DateRangeFilter.values()) { period ->
                        val isSelected = uiState.selectedDatePeriod == period
                        FilterChip(
                            selected = isSelected,
                            onClick = { onDatePeriodSelect(period) },
                            label = {
                                Text(
                                    text = period.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // شريط تصفية التصنيفات الأفقي (يعتمد categoryId وليس الاسم)
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // خيار "جميع التصنيفات"
                    item {
                        val isAllSelected = uiState.selectedCategoryId == null
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCategorySelect(null) }
                                .then(
                                    if (isAllSelected) {
                                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    }
                                ),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAllSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "جميع التصنيفات",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    // عناصر التصنيفات المتاحة
                    items(uiState.availableFilterCategories, key = { it.id }) { category ->
                        val isSelected = uiState.selectedCategoryId == category.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCategorySelect(category.id) }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    }
                                ),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val catIcon = CategoryIconHelper.getIcon(category.iconKey)
                                val catColor = CategoryIconHelper.getColor(category.iconKey)
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else catColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // شريط عدد النتائج وزر إعادة التعيين
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "العمليات المعروضة (${uiState.transactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    val isFiltered = uiState.selectedCategoryId != null ||
                            uiState.selectedFilterIndex != 0 ||
                            uiState.selectedDatePeriod != DateRangeFilter.ALL ||
                            uiState.searchQuery.isNotBlank()

                    if (isFiltered) {
                        TextButton(onClick = onResetFilters) {
                            Text(
                                text = "إعادة التعيين",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // القائمة المجمعة حسب التاريخ أو الحالات الفارغة
            if (uiState.transactions.isEmpty()) {
                item {
                    EmptyFilteredStateCard(
                        hasFilters = uiState.selectedCategoryId != null ||
                                uiState.selectedFilterIndex != 0 ||
                                uiState.selectedDatePeriod != DateRangeFilter.ALL ||
                                uiState.searchQuery.isNotBlank(),
                        onResetFilters = onResetFilters
                    )
                }
            } else {
                groupedTransactions.forEach { (date, transactions) ->
                    item(key = "date_header_$date") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = date,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                val dailyExpenseSum = transactions
                                    .filter { it.type == TransactionType.EXPENSE }
                                    .sumOf { it.amount }
                                val dailyIncomeSum = transactions
                                    .filter { it.type == TransactionType.INCOME }
                                    .sumOf { it.amount }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (dailyExpenseSum > 0) {
                                        Text(
                                            text = "- ${formatNumber(dailyExpenseSum)} ريال",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (dailyIncomeSum > 0) {
                                        Text(
                                            text = "+ ${formatNumber(dailyIncomeSum)} ريال",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(transactions, key = { it.id }) { transaction ->
                        RealTransactionRowItem(
                            transaction = transaction,
                            onClick = { selectedTransactionForDetails = transaction }
                        )
                    }
                }
            }
        }
    }

    // نافذة تفاصيل العملية السفلية مع التعديل والحذف
    selectedTransactionForDetails?.let { tx ->
        TransactionDetailsBottomSheet(
            transaction = tx,
            onDismiss = { selectedTransactionForDetails = null },
            onEdit = {
                selectedTransactionForDetails = null
                onEditTransaction(it)
            },
            onDelete = {
                transactionToDelete = it
            }
        )
    }

    // نافذة تأكيد حذف العملية
    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "تأكيد حذف العملية",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف هذه العملية بشكل نهائي؟ لا يمكن التراجع عن هذا الإجراء.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = transactionToDelete
                        transactionToDelete = null
                        selectedTransactionForDetails = null
                        if (toDel != null) {
                            onDeleteTransaction(toDel)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = "نعم، حذف",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text(
                        text = "إلغاء",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

/**
 * بطاقة ملخص النتائج المصفاة
 */
@Composable
private fun FilteredResultsSummaryCard(
    totalExpenses: Long,
    totalIncome: Long,
    netTotal: Long,
    count: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // إجمالي المصروفات
            Column {
                Text(
                    text = "المصروفات",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "- ${formatNumber(totalExpenses)} ريال",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            }

            // إجمالي الإيرادات
            Column {
                Text(
                    text = "الإيرادات",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "+ ${formatNumber(totalIncome)} ريال",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                )
            }

            // صافي التدفق
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "الصافي",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                val netColor = if (netTotal >= 0) IncomeGreen else ExpenseRed
                val prefix = if (netTotal >= 0) "+ " else "- "
                Text(
                    text = "$prefix${formatNumber(kotlin.math.abs(netTotal))} ريال",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = netColor
                )
            }
        }
    }
}

/**
 * بطاقة الحالة الفارغة عند عدم وجود نتائج
 */
@Composable
private fun EmptyFilteredStateCard(
    hasFilters: Boolean,
    onResetFilters: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (hasFilters) "لا توجد نتائج تطابق معايير البحث والتصفية" else "لا توجد عمليات مسجلة بعد",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasFilters) "جرّب تغيير كلمات البحث أو إعادة ضبط المرشحات" else "اضغط على زر (+) في الأسفل لإضافة أول معاملة مالية",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            if (hasFilters) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onResetFilters,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إعادة ضبط المرشحات")
                }
            }
        }
    }
}

/**
 * صف عرض العملية المالية الحقيقية المسجلة في قاعدة البيانات
 */
@Composable
fun RealTransactionRowItem(
    transaction: TransactionItem,
    onClick: () -> Unit
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) ExpenseRed else IncomeGreen
    val amountPrefix = if (isExpense) "- " else "+ "
    val categoryIcon = CategoryIconHelper.getIcon(transaction.categoryIconKey)
    val categoryColor = CategoryIconHelper.getColor(transaction.categoryIconKey)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // أيقونة التصنيف
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // اسم التصنيف والملاحظة
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.categoryName ?: "عملية مالية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!transaction.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // المبلغ المالي بالريال اليمني
            Text(
                text = "$amountPrefix${formatNumber(transaction.amount)} ريال",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
