package com.masroofi.app.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.presentation.components.CategoryIconHelper
import com.masroofi.app.presentation.components.MasroofiTextField
import com.masroofi.app.presentation.theme.ExpenseRed
import com.masroofi.app.presentation.theme.IncomeGreen
import com.masroofi.app.presentation.theme.PrimaryGreen
import com.masroofi.app.presentation.theme.TextSecondary

/**
 * نافذة إدارة التصنيفات الكاملة (CategoryManagementSheet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementSheet(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onToggleCategoryActive: (Category) -> Unit,
    onOpenAddCategory: (TransactionType) -> Unit,
    onOpenEditCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onSaveCategory: (id: String?, name: String, type: TransactionType, iconKey: String) -> Unit,
    onCloseAddEditDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val currentList = if (selectedTab == 0) uiState.expenseCategories else uiState.incomeCategories
    val currentType = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "إدارة التصنيفات المالية",
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
                actions = {
                    TextButton(onClick = { onOpenAddCategory(currentType) }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تصنيف جديد", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // شريط التبويب: المصروفات / الإيرادات
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "تصنيفات المصروفات (${uiState.expenseCategories.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "تصنيفات الإيرادات (${uiState.incomeCategories.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // قائمة التصنيفات التفاعلية
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = currentList,
                    key = { it.id },
                    contentType = { "category_item" }
                ) { category ->
                    val count = uiState.categoryTransactionCounts[category.id] ?: 0
                    CategoryManagementRow(
                        category = category,
                        transactionCount = count,
                        onToggleActive = { onToggleCategoryActive(category) },
                        onEdit = { onOpenEditCategory(category) },
                        onDelete = { onDeleteCategory(category) }
                    )
                }
            }
        }
    }

    // نافذة إضافة / تعديل التصنيف
    if (uiState.showAddEditCategoryDialog) {
        val categoryToEdit = uiState.categoryToEdit
        val existingCount = categoryToEdit?.let { uiState.categoryTransactionCounts[it.id] } ?: 0

        AddEditCategoryDialog(
            category = categoryToEdit,
            initialType = uiState.addEditCategoryType,
            hasTransactions = existingCount > 0,
            onDismiss = onCloseAddEditDialog,
            onSave = onSaveCategory
        )
    }
}

/**
 * صف عرض وإدارة التصنيف المالي
 */
@Composable
private fun CategoryManagementRow(
    category: Category,
    transactionCount: Int,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val catColor = CategoryIconHelper.getColor(category.iconKey)
    val catIcon = CategoryIconHelper.getIcon(category.iconKey)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (category.isActive) 2.dp else 0.dp,
        border = if (!category.isActive) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // أيقونة التصنيف واسمه
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (category.isActive) catColor.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = catIcon,
                        contentDescription = null,
                        tint = if (category.isActive) catColor else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (category.isActive) MaterialTheme.colorScheme.onSurface else TextSecondary
                        )
                        if (category.isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "افتراضي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (transactionCount > 0) "$transactionCount عملية مسجلة" else "لا توجد عمليات مسجلة",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // أدوات التحكم: مفتاح التعطيل، زر التعديل، وزر الحذف
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // مفتاح التبديل للتعطيل أو التفعيل
                Switch(
                    checked = category.isActive,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.padding(end = 4.dp)
                )

                // زر التعديل
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // زر الحذف (إذا لم تكن هناك عمليات مسجلة ولم يكن تصنيفاً افتراضياً)
                if (transactionCount == 0 && !category.isDefault) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = ExpenseRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * نافذة حوار إضافة أو تعديل تصنيف مالي
 */
@Composable
private fun AddEditCategoryDialog(
    category: Category?,
    initialType: TransactionType,
    hasTransactions: Boolean,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, type: TransactionType, iconKey: String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedType by remember { mutableStateOf(category?.type ?: initialType) }
    var selectedIcon by remember { mutableStateOf(category?.iconKey ?: "fastfood") }
    var nameError by remember { mutableStateOf<String?>(null) }

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
                    text = if (category == null) "إضافة تصنيف جديد" else "تعديل التصنيف",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                // حقل اسم التصنيف
                MasroofiTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = "اسم التصنيف بالعربية",
                    placeholder = "مثال: بقالة، كهرباء، صيانة...",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    isError = nameError != null,
                    supportingText = nameError
                )

                Spacer(modifier = Modifier.height(14.dp))

                // نوع التصنيف: مصروف أو إيراد
                Text(
                    text = "نوع التصنيف",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (hasTransactions && category != null) {
                    // إذا كان مرتبطاً بعمليات لا يجوز تغيير نوعه
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "نوع التصنيف مقفل (${if (selectedType == TransactionType.EXPENSE) "مصروف" else "إيراد"}) لوجود عمليات سابقة",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == TransactionType.EXPENSE,
                            onClick = { selectedType = TransactionType.EXPENSE },
                            label = { Text("مصروف") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseRed.copy(alpha = 0.15f),
                                selectedLabelColor = ExpenseRed
                            )
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.INCOME,
                            onClick = { selectedType = TransactionType.INCOME },
                            label = { Text("إيراد") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IncomeGreen.copy(alpha = 0.15f),
                                selectedLabelColor = IncomeGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // شبكة اختيار الأيقونة
                Text(
                    text = "اختر أيقونة التصنيف",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = CategoryIconHelper.selectableIcons,
                        key = { it },
                        contentType = { "category_icon" }
                    ) { iconKey ->
                        val isIconSelected = selectedIcon == iconKey
                        val iconColor = CategoryIconHelper.getColor(iconKey)
                        val iconVec = CategoryIconHelper.getIcon(iconKey)

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isIconSelected) iconColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = if (isIconSelected) 2.dp else 0.dp,
                                    color = if (isIconSelected) iconColor else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedIcon = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVec,
                                contentDescription = null,
                                tint = if (isIconSelected) iconColor else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // أزرار الحفظ والإلغاء
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
                            if (name.trim().isBlank()) {
                                nameError = "يرجى كتابة اسم التصنيف بالعربية"
                                return@Button
                            }
                            onSave(category?.id, name.trim(), selectedType, selectedIcon)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (category == null) "إضافة" else "حفظ التعديل")
                    }
                }
            }
        }
    }
}
