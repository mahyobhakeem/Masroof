package com.masroofi.app.ui.transactions

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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masroofi.app.model.MockData
import com.masroofi.app.model.Transaction
import com.masroofi.app.ui.components.MasroofiTextField
import com.masroofi.app.ui.home.TransactionRowItem
import com.masroofi.app.ui.home.formatNumber
import com.masroofi.app.ui.theme.*

@Composable
fun TransactionsHistoryScreen(
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: الكل, 1: مصروفات, 2: إيرادات
    val filterTabs = listOf("الكل", "المصروفات", "الإيرادات")

    val allCategoryNames = remember {
        listOf("جميع التصنيفات") + MockData.allCategories.map { it.name }
    }
    var selectedCategory by remember { mutableStateOf("جميع التصنيفات") }

    val filteredTransactions = remember(searchQuery, selectedFilterIndex, selectedCategory) {
        MockData.transactionsList.filter { item ->
            val matchesSearch = searchQuery.isEmpty() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true) ||
                    item.notes.contains(searchQuery, ignoreCase = true)

            val matchesType = when (selectedFilterIndex) {
                1 -> item.isExpense
                2 -> !item.isExpense
                else -> true
            }

            val matchesCategory = selectedCategory == "جميع التصنيفات" || item.category == selectedCategory

            matchesSearch && matchesType && matchesCategory
        }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { it.date }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
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
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "بحث في العمليات",
                placeholder = "ابحث بالاسم، التصنيف، أو الملاحظة...",
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
                    val isSelected = selectedFilterIndex == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFilterIndex = index }
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
                    val isSelected = selectedCategory == categoryName
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedCategory = categoryName }
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

        // عدد نتائج البحث الإجمالي
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "العمليات المسجلة (${filteredTransactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (selectedCategory != "جميع التصنيفات" || selectedFilterIndex != 0 || searchQuery.isNotEmpty()) {
                    TextButton(onClick = {
                        searchQuery = ""
                        selectedFilterIndex = 0
                        selectedCategory = "جميع التصنيفات"
                    }) {
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
        if (filteredTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد عمليات تطابق معايير التصفية المحددة",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }
        } else {
            groupedTransactions.forEach { (date, transactions) ->
                // ترويسة كل تاريخ
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
                            val dailyExpenseSum = transactions.filter { it.isExpense }.sumOf { it.amount }
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

                // عمليات ذلك التاريخ
                items(transactions, key = { it.id }) { transaction ->
                    TransactionRowItem(transaction = transaction)
                }
            }
        }
    }
}
