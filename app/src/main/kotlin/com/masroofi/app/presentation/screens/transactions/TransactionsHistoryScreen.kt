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
        onCategorySelect = { viewModel.setSelectedCategoryName(it) },
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
    onCategorySelect: (String) -> Unit,
    onResetFilters: () -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    onDeleteTransaction: (TransactionItem) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val filterTabs = listOf("الكل", "المصروفات", "الإيرادات")
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionItem?>(null) }

    val allCategoryNames = remember(uiState.categories) {
        listOf("جميع التصنيفات") + uiState.categories.map { it.name }
    }

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
                    text = "عرض وتصفية جميع العمليات المالية المجمعة حسب التاريخ",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // شريط البحث المعتمد بتصميم OutlinedTextField
            item {
                MasroofiTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = "بحث في العمليات",
                    placeholder = "ابحث بالمبلغ، التصنيف، أو الملاحظة...",
                    leadingIcon = Icons.Default.Search
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
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
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

            // شريط تصفية التصنيفات الأفقي
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allCategoryNames) { categoryName ->
                        val isSelected = uiState.selectedCategoryName == categoryName
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCategorySelect(categoryName) }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    }
                                ),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // عدد نتائج البحث الإجمالي وزر إعادة التعيين
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "العمليات المسجلة (${uiState.transactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (uiState.selectedCategoryName != "جميع التصنيفات" || uiState.selectedFilterIndex != 0 || uiState.searchQuery.isNotEmpty()) {
                        TextButton(onClick = onResetFilters) {
                            Text(
                                text = "إعادة التعيين",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // القائمة المجمعة حسب التاريخ
            if (uiState.transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد عمليات مسجلة تطابق التصفية",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                groupedTransactions.forEach { (date, transactions) ->
                    item(key = "date_header_$date") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                                if (dailyExpenseSum > 0) {
                                    Text(
                                        text = "إجمالي: - ${formatNumber(dailyExpenseSum)} ريال",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = ExpenseRed,
                                        fontSize = 11.sp
                                    )
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
                selectedTransactionForDetails = null
                onDeleteTransaction(it)
            }
        )
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
