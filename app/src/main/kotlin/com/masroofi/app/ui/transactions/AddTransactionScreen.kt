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
import com.masroofi.app.model.CategoryItem
import com.masroofi.app.model.MockData
import com.masroofi.app.ui.components.MasroofiTextField
import com.masroofi.app.ui.theme.*

@Composable
fun AddTransactionScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpense by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(MockData.allCategories.first()) }
    var transactionDate by remember { mutableStateOf("9 نوفمبر 2026") }
    var notes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        // ترويسة الشاشة
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إضافة عملية جديدة",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
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
                isExpense = isExpense,
                onToggle = { isExpense = it }
            )
            Spacer(modifier = Modifier.height(22.dp))
        }

        // حقل إدخال المبلغ
        item {
            MasroofiTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                label = "المبلغ",
                placeholder = "0",
                trailingText = "ريال يمني YER",
                leadingIcon = Icons.Default.Payments,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // قسم اختيار التصنيف
        item {
            Text(
                text = "اختيار التصنيف",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            CategorySelectorSection(
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it }
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // حقل التاريخ
        item {
            MasroofiTextField(
                value = transactionDate,
                onValueChange = { transactionDate = it },
                label = "تاريخ العملية",
                leadingIcon = Icons.Default.CalendarToday,
                readOnly = false
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // حقل الملاحظات الاختيارية
        item {
            MasroofiTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "ملاحظات وتفاصيل إضافية (اختياري)",
                placeholder = "اكتب أي تفاصيل توضيحية هنا...",
                leadingIcon = Icons.Default.Description,
                singleLine = false,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // الأزرار السفلية العريضة
        item {
            ActionButtonsSection(
                onSave = { onDismiss() },
                onCancel = {
                    amount = ""
                    notes = ""
                    onDismiss()
                }
            )
        }
    }
}

// زر التبديل بين المصروف والإيراد
@Composable
private fun TransactionTypeToggle(
    isExpense: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            // زر المصروف
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isExpense) ExpenseRed else Color.Transparent)
                    .clickable { onToggle(true) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "مصروف",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) Color.White else TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // زر الإيراد
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isExpense) IncomeGreen else Color.Transparent)
                    .clickable { onToggle(false) },
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

// قسم اختيار التصنيفات
@Composable
private fun CategorySelectorSection(
    selectedCategory: CategoryItem?,
    onSelectCategory: (CategoryItem) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(MockData.allCategories) { category ->
            val isSelected = category.id == selectedCategory?.id
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectCategory(category) }
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        } else {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        }
                    ),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(category.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = category.color,
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
}

// أزرار الحفظ والإلغاء
@Composable
private fun ActionButtonsSection(
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // زر الحفظ الأساسي
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "حفظ العملية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // زر الإلغاء الثانوي بإطار خارجي
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = 1.dp
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextSecondary
            )
        ) {
            Text(
                text = "إلغاء والعودة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        }
    }
}
