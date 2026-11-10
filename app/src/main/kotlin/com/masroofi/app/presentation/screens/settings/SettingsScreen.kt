package com.masroofi.app.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masroofi.app.data.preferences.ThemeMode
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.presentation.components.MasroofiExpandableCard
import com.masroofi.app.presentation.components.MasroofiItemCard
import com.masroofi.app.presentation.theme.*
import com.masroofi.app.utils.formatNumber

/**
 * شاشة الإعدادات المربوطة بـ SettingsViewModel و Room و DataStore
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsContent(
        uiState = uiState,
        onSetThemeMode = { viewModel.setThemeMode(it) },
        onOpenBudgetManagement = { viewModel.openBudgetManagement() },
        onCloseBudgetManagement = { viewModel.closeBudgetManagement() },
        onOpenEditBudget = { viewModel.openEditBudget(it) },
        onCloseEditBudget = { viewModel.closeEditBudgetDialog() },
        onSaveBudget = { ym, amount -> viewModel.setBudgetForMonth(ym, amount) },
        onOpenCategoryManagement = { viewModel.openCategoryManagement() },
        onCloseCategoryManagement = { viewModel.closeCategoryManagement() },
        onToggleCategoryActive = { viewModel.toggleCategoryActive(it) },
        onOpenAddCategory = { viewModel.openAddCategory(it) },
        onOpenEditCategory = { viewModel.openEditCategory(it) },
        onDeleteCategory = { viewModel.deleteCategory(it) },
        onSaveCategory = { id, name, type, iconKey -> viewModel.saveCategory(id, name, type, iconKey) },
        onCloseAddEditCategoryDialog = { viewModel.closeAddEditCategoryDialog() },
        onOpenAboutDialog = { viewModel.openAboutDialog() },
        onCloseAboutDialog = { viewModel.closeAboutDialog() },
        onClearMessages = { viewModel.clearMessages() },
        modifier = modifier
    )
}

/**
 * المكون المرئي لشاشة الإعدادات (SettingsContent)
 */
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onSetThemeMode: (ThemeMode) -> Unit,
    onOpenBudgetManagement: () -> Unit,
    onCloseBudgetManagement: () -> Unit,
    onOpenEditBudget: (com.masroofi.app.domain.model.MonthlyBudget?) -> Unit,
    onCloseEditBudget: () -> Unit,
    onSaveBudget: (yearMonth: String, amount: Long) -> Unit,
    onOpenCategoryManagement: () -> Unit,
    onCloseCategoryManagement: () -> Unit,
    onToggleCategoryActive: (Category) -> Unit,
    onOpenAddCategory: (TransactionType) -> Unit,
    onOpenEditCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onSaveCategory: (id: String?, name: String, type: TransactionType, iconKey: String) -> Unit,
    onCloseAddEditCategoryDialog: () -> Unit,
    onOpenAboutDialog: () -> Unit,
    onCloseAboutDialog: () -> Unit,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // عرض رسائل التنبيه والنجاح
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ترويسة الشاشة
            item {
                Text(
                    text = "الإعدادات",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تخصيص المظهر وإدارة الميزانيات والتصنيفات",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 1. بطاقة مظهر التطبيق (3 أوضاع: فاتح، داكن، تلقائي)
            item {
                ThemeModeSelectorCard(
                    currentMode = uiState.themeMode,
                    onModeSelect = onSetThemeMode
                )
            }

            // 2. بطاقة إدارة الميزانية الشهرية
            item {
                val budgetText = if (uiState.currentMonthBudget != null) {
                    "${formatNumber(uiState.currentMonthBudget)} ريال للشهر الحالي"
                } else {
                    "غير محددة للشهر الحالي • انقر للإدارة"
                }
                MasroofiItemCard(
                    title = "إدارة الميزانية الشهرية",
                    subtitle = budgetText,
                    icon = Icons.Default.AccountBalanceWallet,
                    iconColor = IncomeGreen,
                    onClick = onOpenBudgetManagement
                )
            }

            // 3. بطاقة إدارة التصنيفات المالية
            item {
                MasroofiItemCard(
                    title = "إدارة التصنيفات المالية",
                    subtitle = "${uiState.allCategories.size} تصنيفاً (المصروفات والإيرادات)",
                    icon = Icons.Default.Category,
                    iconColor = CategoryFood,
                    onClick = onOpenCategoryManagement
                )
            }

            // 4. بطاقة تخصيص التطبيق والعملة (قسم قابل للتوسيع)
            item {
                MasroofiExpandableCard(
                    title = "تخصيص التطبيق والعملة",
                    icon = Icons.Default.Tune,
                    iconColor = CategoryTransport
                ) {
                    // العملة الرسمية
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "العملة المعتمدة",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "الريال اليمني (YER)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.8.dp)

                    // طبيعة العمل
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "طبيعة حفظ البيانات",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "محلي 100% (Offline-First)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.8.dp)

                    // بداية الأسبوع
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بداية الأسبوع المالي",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "يوم السبت",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 5. بطاقة حول تطبيق مصروفي
            item {
                MasroofiItemCard(
                    title = "عن تطبيق مصروفي",
                    subtitle = "الإصدار 1.0.0 • تطبيق محلي لإدارة المصروفات",
                    icon = Icons.Default.Info,
                    iconColor = TextSecondary,
                    onClick = onOpenAboutDialog
                )
            }

            // تذييل الصفحة برقم الإصدار
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "مصروفي | Masroofi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "الإصدار 1.0.0 • يعمل محلياً دون إنترنت",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    // شاشة إدارة التصنيفات الكاملة
    if (uiState.isCategoryManagementOpen) {
        Surface(modifier = Modifier.fillMaxSize()) {
            CategoryManagementSheet(
                uiState = uiState,
                onDismiss = onCloseCategoryManagement,
                onToggleCategoryActive = onToggleCategoryActive,
                onOpenAddCategory = onOpenAddCategory,
                onOpenEditCategory = onOpenEditCategory,
                onDeleteCategory = onDeleteCategory,
                onSaveCategory = onSaveCategory,
                onCloseAddEditDialog = onCloseAddEditCategoryDialog
            )
        }
    }

    // شاشة إدارة الميزانية وسجل الميزانيات
    if (uiState.isBudgetManagementOpen) {
        Surface(modifier = Modifier.fillMaxSize()) {
            BudgetManagementSheet(
                uiState = uiState,
                onDismiss = onCloseBudgetManagement,
                onOpenEditBudget = onOpenEditBudget,
                onCloseEditBudget = onCloseEditBudget,
                onSaveBudget = onSaveBudget
            )
        }
    }

    // نافذة حول التطبيق
    if (uiState.isAboutDialogOpen) {
        AboutAppDialog(onDismiss = onCloseAboutDialog)
    }
}

/**
 * بطاقة اختيار وضع المظهر (فاتح / داكن / تلقائي)
 */
@Composable
private fun ThemeModeSelectorCard(
    currentMode: ThemeMode,
    onModeSelect: (ThemeMode) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (currentMode) {
                        ThemeMode.LIGHT -> Icons.Default.LightMode
                        ThemeMode.DARK -> Icons.Default.DarkMode
                        ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "مظهر التطبيق",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (currentMode) {
                            ThemeMode.LIGHT -> "الوضع الفاتح مفعل"
                            ThemeMode.DARK -> "الوضع الداكن مفعل"
                            ThemeMode.SYSTEM -> "تلقائي حسب إعدادات الجهاز"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // أزرار الاختيار الثلاثية
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOptionChip(
                    title = "فاتح",
                    icon = Icons.Default.LightMode,
                    isSelected = currentMode == ThemeMode.LIGHT,
                    onClick = { onModeSelect(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionChip(
                    title = "داكن",
                    icon = Icons.Default.DarkMode,
                    isSelected = currentMode == ThemeMode.DARK,
                    onClick = { onModeSelect(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionChip(
                    title = "تلقائي",
                    icon = Icons.Default.SettingsBrightness,
                    isSelected = currentMode == ThemeMode.SYSTEM,
                    onClick = { onModeSelect(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionChip(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
