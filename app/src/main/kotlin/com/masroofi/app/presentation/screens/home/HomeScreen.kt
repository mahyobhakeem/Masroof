package com.masroofi.app.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masroofi.app.domain.calculations.CategoryExpenseSummary
import com.masroofi.app.domain.calculations.FinancialSummary
import com.masroofi.app.domain.model.TimePeriodMode
import com.masroofi.app.domain.model.TransactionItem
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.presentation.components.CategoryIconHelper
import com.masroofi.app.presentation.components.LocalAppCurrency
import com.masroofi.app.presentation.components.MasroofDatePickerDialog
import com.masroofi.app.presentation.components.MasroofDateRangeDialog
import com.masroofi.app.presentation.components.MasroofiItemCard
import com.masroofi.app.presentation.components.TimePeriodSegmentedSwitch
import com.masroofi.app.presentation.screens.transactions.TransactionDetailsBottomSheet
import com.masroofi.app.presentation.theme.*
import com.masroofi.app.utils.FormatUtils
import com.masroofi.app.utils.formatNumber
import com.masroofi.app.utils.formatYearMonth
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

/**
 * الشاشة الرئيسية المرتبطة بنموذج العرض (Stateful HomeScreen)
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTransactions: () -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeScreenContent(
            uiState = uiState,
            onNavigateToTransactions = onNavigateToTransactions,
            onPeriodModeSelect = viewModel::setPeriodMode,
            onPrevious = {
                when (uiState.selectedPeriodMode) {
                    TimePeriodMode.DAILY -> viewModel.selectPreviousDay()
                    TimePeriodMode.WEEKLY -> viewModel.selectPreviousWeek()
                    TimePeriodMode.MONTHLY -> viewModel.selectPreviousMonth()
                    TimePeriodMode.CUSTOM_RANGE -> viewModel.openCustomRangePicker()
                }
            },
            onNext = {
                when (uiState.selectedPeriodMode) {
                    TimePeriodMode.DAILY -> viewModel.selectNextDay()
                    TimePeriodMode.WEEKLY -> viewModel.selectNextWeek()
                    TimePeriodMode.MONTHLY -> viewModel.selectNextMonth()
                    TimePeriodMode.CUSTOM_RANGE -> viewModel.openCustomRangePicker()
                }
            },
            onDateClick = {
                when (uiState.selectedPeriodMode) {
                    TimePeriodMode.DAILY -> viewModel.openDatePicker()
                    TimePeriodMode.CUSTOM_RANGE -> viewModel.openCustomRangePicker()
                    else -> {}
                }
            },
            onBackToToday = { viewModel.setSelectedDate(LocalDate.now()) },
            onOpenBudgetDialog = viewModel::openBudgetDialog,
            onTransactionClick = viewModel::onTransactionClick,
            modifier = Modifier.fillMaxSize()
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    // نافذة اختيار تاريخ يوم محدد
    if (uiState.isDatePickerOpen) {
        MasroofDatePickerDialog(
            initialDate = uiState.selectedDate,
            onDateSelected = viewModel::setSelectedDate,
            onDismiss = viewModel::dismissDatePicker,
            title = "اختيار يوم محدد للاستعراض"
        )
    }

    // نافذة اختيار فترة مخصصة (من - إلى)
    if (uiState.isCustomRangePickerOpen) {
        MasroofDateRangeDialog(
            initialStartDate = uiState.customStartDate,
            initialEndDate = uiState.customEndDate,
            onRangeSelected = viewModel::setCustomRange,
            onDismiss = viewModel::dismissCustomRangePicker
        )
    }

    // نافذة تحديد الميزانية
    if (uiState.isBudgetDialogOpen) {
        SetBudgetDialog(
            yearMonth = uiState.selectedMonth,
            amountInput = uiState.budgetInput,
            errorMessage = uiState.budgetError,
            isSaving = uiState.isSavingBudget,
            onAmountChange = viewModel::onBudgetInputChanged,
            onSave = viewModel::saveMonthlyBudget,
            onDismiss = viewModel::dismissBudgetDialog
        )
    }

    // نافذة تفاصيل العملية السفلية
    uiState.selectedTransactionForDetails?.let { transaction ->
        TransactionDetailsBottomSheet(
            transaction = transaction,
            onDismiss = viewModel::dismissTransactionDetails,
            onEdit = {
                viewModel.dismissTransactionDetails()
                onEditTransaction(transaction)
            },
            onDelete = {
                viewModel.requestDeleteTransaction(transaction)
            }
        )
    }

    // نافذة تأكيد حذف العملية
    if (uiState.isDeleteDialogOpen && uiState.transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
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
                    onClick = viewModel::confirmDeleteTransaction,
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
                TextButton(onClick = viewModel::dismissDeleteDialog) {
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
 * المكون الصامت لواجهة الشاشة الرئيسية (Stateless HomeScreenContent)
 */
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onNavigateToTransactions: () -> Unit,
    onPeriodModeSelect: (TimePeriodMode) -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onDateClick: () -> Unit = {},
    onBackToToday: () -> Unit = {},
    onOpenBudgetDialog: () -> Unit = {},
    onTransactionClick: (TransactionItem) -> Unit = {},
    onPreviousMonth: () -> Unit = onPrevious,
    onNextMonth: () -> Unit = onNext,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // 1. الترويسة العلوية الثابتة
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "مصروف | Masroof",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "إدارة المصروفات والميزانية الشخصية",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 2. شريط التبديل الزمني (اليوم / الأسبوع / الشهر / فترة مخصصة)
        item {
            TimePeriodSegmentedSwitch(
                selectedMode = uiState.selectedPeriodMode,
                onModeSelected = onPeriodModeSelect
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. شريط التنقل الزمني والتاريخ التفاعلي
        item {
            HomeDateNavigator(
                uiState = uiState,
                onPrevious = onPrevious,
                onNext = onNext,
                onDateClick = onDateClick,
                onBackToToday = onBackToToday
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 4. بطاقة الملخص المالي بحسب الوضع المختار
        item {
            if (uiState.selectedPeriodMode == TimePeriodMode.MONTHLY) {
                FinancialSummaryCard(
                    summary = uiState.summary,
                    onOpenBudgetDialog = onOpenBudgetDialog
                )
            } else {
                PeriodFinancialSummaryCard(
                    uiState = uiState,
                    onBackToToday = onBackToToday
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 5. قسم أبرز التصنيفات في الفترة المحددة
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أبرز التصنيفات",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (uiState.topCategories.isNotEmpty()) {
                    Text(
                        text = "${uiState.topCategories.size} تصنيفات",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            if (uiState.topCategories.isEmpty()) {
                EmptyTopCategoriesCard()
            } else {
                TopCategoriesGrid(topCategories = uiState.topCategories)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 6. قسم العمليات المسجلة في الفترة
        item {
            val periodLabel = when (uiState.selectedPeriodMode) {
                TimePeriodMode.DAILY -> FormatUtils.formatArabicDate(uiState.selectedDate, true)
                TimePeriodMode.WEEKLY -> {
                    val (wStart, wEnd) = FormatUtils.getWeekRangeSaturday(uiState.selectedWeekStart)
                    FormatUtils.formatDateRange(wStart, wEnd)
                }
                TimePeriodMode.MONTHLY -> formatYearMonth(uiState.selectedMonth)
                TimePeriodMode.CUSTOM_RANGE -> FormatUtils.formatDateRange(uiState.customStartDate, uiState.customEndDate)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.selectedPeriodMode == TimePeriodMode.MONTHLY) "آخر العمليات" else "العمليات المسجلة",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "المعاملات المالية للفترة: $periodLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                if (uiState.recentTransactions.isNotEmpty()) {
                    TextButton(onClick = onNavigateToTransactions) {
                        Text(
                            text = "عرض الكل",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.recentTransactions.isEmpty()) {
            item {
                EmptyRecentTransactionsCard()
            }
        } else {
            items(
                items = uiState.recentTransactions,
                key = { it.id },
                contentType = { "transaction_item" }
            ) { transaction ->
                HomeTransactionRowItem(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction) }
                )
            }
        }
    }
}

/**
 * شريط التنقل الزمني والتاريخ التفاعلي مع دعم النقر لفتح التقويم
 */
@Composable
private fun HomeDateNavigator(
    uiState: HomeUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateClick: () -> Unit,
    onBackToToday: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.selectedPeriodMode != TimePeriodMode.CUSTOM_RANGE) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "السابق",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            // محتوى التاريخ التفاعلي
            val isClickable = uiState.selectedPeriodMode == TimePeriodMode.DAILY || uiState.selectedPeriodMode == TimePeriodMode.CUSTOM_RANGE

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isClickable) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .then(if (isClickable) Modifier.clickable(onClick = onDateClick) else Modifier)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (uiState.selectedPeriodMode) {
                            TimePeriodMode.DAILY -> Icons.Default.Today
                            TimePeriodMode.WEEKLY -> Icons.Default.CalendarMonth
                            TimePeriodMode.MONTHLY -> Icons.Default.CalendarMonth
                            TimePeriodMode.CUSTOM_RANGE -> Icons.Default.DateRange
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    val displayText = when (uiState.selectedPeriodMode) {
                        TimePeriodMode.DAILY -> {
                            val isToday = uiState.selectedDate == LocalDate.now()
                            if (isToday) {
                                "اليوم (${FormatUtils.formatArabicDate(uiState.selectedDate)})"
                            } else {
                                FormatUtils.formatArabicDate(uiState.selectedDate, true)
                            }
                        }
                        TimePeriodMode.WEEKLY -> {
                            val (wStart, wEnd) = FormatUtils.getWeekRangeSaturday(uiState.selectedWeekStart)
                            FormatUtils.formatDateRange(wStart, wEnd)
                        }
                        TimePeriodMode.MONTHLY -> formatYearMonth(uiState.selectedMonth)
                        TimePeriodMode.CUSTOM_RANGE -> FormatUtils.formatDateRange(uiState.customStartDate, uiState.customEndDate)
                    }

                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isClickable) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تغيير",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (uiState.selectedPeriodMode != TimePeriodMode.CUSTOM_RANGE) {
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "التالي",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

/**
 * بطاقة الملخص المالي المرنة للأوضاع غير الشهرية (اليوم، الأسبوع، الفترة المخصصة)
 */
@Composable
private fun PeriodFinancialSummaryCard(
    uiState: HomeUiState,
    onBackToToday: () -> Unit
) {
    val currencySymbol = LocalAppCurrency.current.symbolArabic
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            val periodTitle = when (uiState.selectedPeriodMode) {
                TimePeriodMode.DAILY -> if (uiState.selectedDate == LocalDate.now()) "إجمالي مصروفات اليوم" else "مصروفات يوم ${uiState.selectedDate}"
                TimePeriodMode.WEEKLY -> "إجمالي مصروفات الأسبوع"
                TimePeriodMode.CUSTOM_RANGE -> "إجمالي مصروفات الفترة"
                TimePeriodMode.MONTHLY -> "إجمالي المصروفات"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = periodTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatNumber(uiState.periodTotalExpenses)} $currencySymbol",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // صفي الإيرادات وصافي التدفق
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // إجمالي الإيرادات
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldLightBg,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إجمالي الإيرادات",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "+${formatNumber(uiState.periodTotalIncome)} $currencySymbol",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }

                // الصافي
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val netValue = maxOf(0L, uiState.periodNetCashflow)
                    val amountTextColor = IncomeGreen

                    Surface(
                        shape = CircleShape,
                        color = EmeraldLightBg,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "الصافي",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "+${formatNumber(netValue)} $currencySymbol",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = amountTextColor
                        )
                    }
                }
            }

            // زر العودة لليوم الحالي إن كان التاريخ المختار ليس اليوم
            if (uiState.selectedPeriodMode == TimePeriodMode.DAILY && uiState.selectedDate != LocalDate.now()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onBackToToday)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "العودة لليوم الحالي (${LocalDate.now()})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// بطاقة الملخص المالي الشهري القياسية
@Composable
private fun FinancialSummaryCard(
    summary: FinancialSummary,
    onOpenBudgetDialog: () -> Unit
) {
    val currencySymbol = LocalAppCurrency.current.symbolArabic
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // إجمالي مصروفات الشهر
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "إجمالي مصروفات الشهر",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatNumber(summary.totalMonthlyExpenses)} $currencySymbol",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // شريط وحالة الميزانية الشهرية
            if (summary.monthlyBudget != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (summary.isBudgetExceeded) {
                                    "تجاوز الميزانية: ${formatNumber(summary.budgetExcessAmount)} $currencySymbol (${summary.consumptionPercentage}%)"
                                } else {
                                    "المتبقي: ${formatNumber(summary.remainingBudget ?: 0L)} $currencySymbol (${summary.consumptionPercentage}%)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.isBudgetExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenBudgetDialog)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل الميزانية",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "الميزانية: ${formatNumber(summary.monthlyBudget)} $currencySymbol",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val progress = (summary.consumptionPercentage / 100f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (summary.isBudgetExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenBudgetDialog)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "لم تحدد ميزانية لهذا الشهر بعد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "تحديد ميزانية +",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // مؤشرات إيرادات الشهر ومصروفات اليوم
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // إيرادات الشهر
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldLightBg,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إيرادات الشهر",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${formatNumber(summary.totalMonthlyIncome)} $currencySymbol",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }

                // مصروفات اليوم
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldLightBg,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مصروفات اليوم",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${formatNumber(summary.totalDailyExpenses)} $currencySymbol",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

// شبكة أبرز التصنيفات
@Composable
private fun TopCategoriesGrid(topCategories: List<CategoryExpenseSummary>) {
    val currencySymbol = LocalAppCurrency.current.symbolArabic
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        topCategories.take(4).forEach { category ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldLightBg,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(category.iconKey),
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = category.categoryName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${category.transactionCount} عملية",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${formatNumber(category.totalSpent)} $currencySymbol",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${category.percentage}% من الإجمالي",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// صف عنصر العملية المالية في القائمة
@Composable
private fun HomeTransactionRowItem(
    transaction: TransactionItem,
    onClick: () -> Unit
) {
    val currencySymbol = LocalAppCurrency.current.symbolArabic
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldLightBg,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = CategoryIconHelper.getIcon(transaction.categoryIconKey ?: "category"),
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.categoryName ?: "غير مصنف",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = transaction.transactionDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            val isExpense = transaction.type == TransactionType.EXPENSE
            val sign = if (isExpense) "-" else "+"
            val amountColor = if (isExpense) MaterialTheme.colorScheme.onSurface else IncomeGreen

            Text(
                text = "$sign${formatNumber(transaction.amount)} $currencySymbol",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
private fun EmptyTopCategoriesCard() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "لا توجد مصروفات مسجلة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "أضف مصروفات في هذا الشهر لعرض توزيع النفقات حسب التصنيف",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun EmptyRecentTransactionsCard() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "لا توجد عمليات بعد",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "اضغط على زر (+) في الأسفل لإضافة أول عملية مالية",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
