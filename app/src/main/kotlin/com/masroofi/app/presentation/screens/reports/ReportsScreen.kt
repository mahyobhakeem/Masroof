package com.masroofi.app.presentation.screens.reports

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masroofi.app.data.preferences.PreferencesManager
import com.masroofi.app.domain.calculations.CategoryExpenseSummary
import kotlinx.coroutines.launch
import com.masroofi.app.domain.calculations.MonthlyComparisonItem
import com.masroofi.app.domain.calculations.MonthlyReportData
import com.masroofi.app.domain.calculations.WeeklySpendingItem
import com.masroofi.app.domain.model.ReportPeriodMode
import com.masroofi.app.presentation.components.CategoryIconHelper
import com.masroofi.app.presentation.components.LocalAppCurrency
import com.masroofi.app.presentation.components.MasroofDatePickerDialog
import com.masroofi.app.presentation.components.MasroofDateRangeDialog
import com.masroofi.app.presentation.components.MasroofTopHeaderBar
import com.masroofi.app.presentation.components.PdfExportDialog
import com.masroofi.app.presentation.components.PdfGeneratedSuccessDialog
import com.masroofi.app.presentation.theme.*
import com.masroofi.app.utils.PdfReportExporter
import com.masroofi.app.utils.formatNumber
import com.masroofi.app.utils.formatYearMonth
import java.io.File
import java.time.YearMonth

private val GridLevels = floatArrayOf(0.0f, 0.33f, 0.66f)

/**
 * شاشة التقارير والإحصائيات المالية المربوطة بالبيانات الحقيقية لقاعدة البيانات
 */
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activeCurrency = LocalAppCurrency.current
    val preferencesManager = remember { PreferencesManager(context) }
    val pdfOwnerName by preferencesManager.pdfOwnerName.collectAsState(initial = "")
    val coroutineScope = rememberCoroutineScope()
    var isPdfDialogOpen by remember { mutableStateOf(false) }
    var generatedPdf by remember { mutableStateOf<File?>(null) }

    // إدارة زر الرجوع خطوة بخطوة: إلغاء تحديد الشريحة أولاً قبل العودة للرئيسية
    BackHandler(enabled = uiState.selectedCategoryForDetails != null) {
        viewModel.selectCategory(null)
    }

    ReportsContent(
        uiState = uiState,
        onModeSelect = { viewModel.setPeriodMode(it) },
        onPrevious = { viewModel.selectPrevious() },
        onNext = { viewModel.selectNext() },
        onDateClick = { viewModel.onDateLabelClick() },
        onCategorySelect = { viewModel.selectCategory(it) },
        onExportPdfClick = { isPdfDialogOpen = true },
        modifier = modifier
    )

    if (isPdfDialogOpen) {
        PdfExportDialog(
            initialOwnerName = pdfOwnerName,
            onDismiss = { isPdfDialogOpen = false },
            onConfirmExport = { periodMode, specificDate, customStart, customEnd, typeFilter, ownerName ->
                isPdfDialogOpen = false
                if (ownerName.isNotBlank()) {
                    coroutineScope.launch {
                        preferencesManager.setPdfOwnerName(ownerName)
                    }
                }
                viewModel.exportPdfReport(
                    context = context,
                    periodMode = periodMode,
                    specificDate = specificDate,
                    customStartDate = customStart,
                    customEndDate = customEnd,
                    typeFilter = typeFilter,
                    currencySymbol = activeCurrency.symbolArabic,
                    ownerName = ownerName,
                    onSuccess = { file -> generatedPdf = file }
                )
            }
        )
    }

    generatedPdf?.let { pdfFile ->
        PdfGeneratedSuccessDialog(
            pdfFile = pdfFile,
            onDismiss = { generatedPdf = null },
            onOpen = {
                try {
                    PdfReportExporter.openPdf(context, pdfFile)
                } catch (_: Exception) {}
            },
            onShare = {
                try {
                    PdfReportExporter.sharePdf(context, pdfFile)
                } catch (_: Exception) {}
            }
        )
    }

    if (uiState.showDatePicker) {
        MasroofDatePickerDialog(
            initialDate = uiState.selectedDate,
            title = if (uiState.periodMode == ReportPeriodMode.WEEKLY) "اختر أسبوعاً" else "اختر يوماً محدداً",
            onDateSelected = { viewModel.setSelectedDate(it) },
            onDismiss = { viewModel.dismissDatePicker() }
        )
    }

    if (uiState.showDateRangePicker) {
        MasroofDateRangeDialog(
            initialStartDate = uiState.customStartDate,
            initialEndDate = uiState.customEndDate,
            onRangeSelected = { start, end -> viewModel.setCustomRange(start, end) },
            onDismiss = { viewModel.dismissDateRangePicker() }
        )
    }
}

/**
 * المكون المرئي لشاشة التقارير (ReportsContent)
 * مفصول عن ViewModel لدعم المعاينة واختبارات الواجهة
 */
@Composable
fun ReportsContent(
    uiState: ReportsUiState,
    onModeSelect: (ReportPeriodMode) -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onDateClick: () -> Unit = {},
    onCategorySelect: (CategoryExpenseSummary?) -> Unit = {},
    onExportPdfClick: () -> Unit = {},
    onPreviousMonth: () -> Unit = onPrevious,
    onNextMonth: () -> Unit = onNext,
    modifier: Modifier = Modifier
) {
    val report = uiState.reportData

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MasroofTopHeaderBar(
            title = "التقارير والإحصائيات",
            subtitle = "تحليل الأداء المالي والمصروفات بدقة",
            icon = Icons.Default.PieChart,
            actions = {
                IconButton(onClick = onExportPdfClick) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "تصدير تقرير PDF",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
        ) {
            // شريط التبديل الزمني (شهري، أسبوعي، يوم محدد، فترة مخصصة)
            item {
                ReportPeriodSegmentedSwitch(
                    selectedMode = uiState.periodMode,
                    onModeSelect = onModeSelect
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

        // شريط التنقل الزمني
        item {
            ReportsDateNavigator(
                label = uiState.periodLabel.ifEmpty { formatYearMonth(uiState.selectedYearMonth) },
                onPrevious = onPrevious,
                onNext = onNext,
                onDateClick = onDateClick
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // بطاقات المؤشرات المالية الأربعة
        item {
            ReportsSummaryCards(
                totalExpenses = report.totalExpenses,
                totalIncome = report.totalIncome,
                netCashflow = report.netCashflow,
                averageDailyExpense = report.averageDailyExpense
            )
            Spacer(modifier = Modifier.height(22.dp))
        }

        // في حال عدم وجود مصروفات للفترة المختارة: عرض بطاقة حالة فارغة أنيقة
        if (!report.hasExpenses) {
            item {
                EmptyReportsStateCard(periodLabel = uiState.periodLabel.ifEmpty { formatYearMonth(uiState.selectedYearMonth) })
                Spacer(modifier = Modifier.height(20.dp))
            }
        } else {
            // الرسم الأول: مخطط توزيع المصروفات الدائري المفرغ (Donut Chart)
            item {
                ExpenseBreakdownDonutChartCard(
                    categories = report.categoryBreakdown,
                    totalExpenses = report.totalExpenses,
                    selectedCategory = uiState.selectedCategoryForDetails,
                    onCategorySelect = onCategorySelect
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // الرسوم الإضافية للشهر فقط
            if (uiState.periodMode == ReportPeriodMode.MONTHLY) {
                // الرسم الثاني: مخطط توزيع المصروفات عبر أسابيع الشهر (Weekly Bar Chart)
                item {
                    WeeklySpendingBarChartCard(
                        weeklyList = report.weeklySpendingList,
                        peakPeriod = report.peakSpendingPeriod,
                        averageWeekly = report.averageWeeklyExpense
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // الرسم الثالث: مخطط مقارنة المصروفات عبر الأشهر (Monthly Comparison Bar Chart)
                item {
                    MonthlyComparisonBarChartCard(
                        comparisons = report.monthlyComparisons,
                        monthChangePercentage = report.monthChangePercentage
                    )
                }
            }
        }
    }
}
}

/**
 * ترويسة الشاشة
 */
@Composable
private fun ReportsHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "التقارير والإحصائيات",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "تحليل الأداء المالي والمصروفات بدقة",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

/**
 * شريط التبديل الزمني بين الأنماط الأربعة
 */
@Composable
private fun ReportPeriodSegmentedSwitch(
    selectedMode: ReportPeriodMode,
    onModeSelect: (ReportPeriodMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ReportPeriodMode.values().forEach { mode ->
                val isSelected = mode == selectedMode
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    tonalElevation = if (isSelected) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onModeSelect(mode) }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * محدد الفترات والتنقل الزمني لشاشة التقارير
 */
@Composable
private fun ReportsDateNavigator(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
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

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDateClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

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
        }
    }
}

/**
 * بطاقات المؤشرات المالية الأربعة في أعلى الشاشة
 */
@Composable
private fun ReportsSummaryCards(
    totalExpenses: Long,
    totalIncome: Long,
    netCashflow: Long,
    averageDailyExpense: Long
) {
    val currencySymbol = LocalAppCurrency.current.symbolArabic
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // الصف الأول: المصروفات والإيرادات
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // بطاقة إجمالي المصروفات
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EmeraldLightBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إجمالي المصروفات",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${formatNumber(totalExpenses)} $currencySymbol",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }

            // بطاقة إجمالي الإيرادات
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EmeraldLightBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إجمالي الإيرادات",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${formatNumber(totalIncome)} $currencySymbol",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }
            }
        }

        // الصف الثاني: صافي التدفق ومتوسط المصروف اليومي
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // بطاقة الصافي
            val netValue = maxOf(0L, netCashflow)
            val netColor = IncomeGreen
            val netPrefix = "+ "

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EmeraldLightBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "الصافي",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$netPrefix${formatNumber(netValue)} $currencySymbol",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = netColor
                        )
                    }
                }
            }

            // بطاقة متوسط المصروف اليومي
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "متوسط المصروف اليومي",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${formatNumber(averageDailyExpense)} $currencySymbol",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * بطاقة الحالة الفارغة عند عدم وجود مصروفات للفترة المختارة
 */
@Composable
private fun EmptyReportsStateCard(
    periodLabel: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "لا توجد مصروفات مسجلة لهذه الفترة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "لم يتم تسجيل أي نفقات في ($periodLabel) لعرض المخططات والتحليلات. يمكنك اختيار فترة أخرى أو إضافة معاملات جديدة.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// لوحة الألوان المالية التنفيذية المعتمدة للمخطط الدائري ومربعات الإيضاح (Executive Financial Palette)
val ExecutiveFinancialPalette = listOf(
    Color(0xFF16A67A), // 1. أخضر زمردي
    Color(0xFF2563EB), // 2. أزرق ملكي
    Color(0xFFD97706), // 3. ذهبي عنبري
    Color(0xFF0D9488), // 4. فيروزي عميق
    Color(0xFF7C3AED), // 5. بنفسجي مالي
    Color(0xFFE11D48), // 6. مرجاني ناعم
    Color(0xFF475569), // 7. كحلي صخري
    Color(0xFF65A30D)  // 8. زيتوني
)

/**
 * الرسم الأول: بطاقة مخطط توزيع المصروفات الدائري المفرغ (Donut Chart) مع قائمة التصنيفات التفاعلية
 */
@Composable
private fun ExpenseBreakdownDonutChartCard(
    categories: List<CategoryExpenseSummary>,
    totalExpenses: Long,
    selectedCategory: CategoryExpenseSummary?,
    onCategorySelect: (CategoryExpenseSummary?) -> Unit
) {
    val currencySymbol = LocalAppCurrency.current.symbolArabic
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "توزيع المصروفات حسب التصنيف",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "انقر على أي شريحة أو تصنيف لعرض تفاصيله الدقيقة",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // الرسم الدائري المفرغ (Donut Chart) مع لوحة الألوان المالية التنفيذية
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentAlignment = Alignment.Center
            ) {
                val strokeWidthDp = 34.dp

                Canvas(
                    modifier = Modifier
                        .size(190.dp)
                        .padding(8.dp)
                ) {
                    val strokePx = strokeWidthDp.toPx()
                    val selectedStrokePx = strokePx + 6.dp.toPx()
                    val normalStroke = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    val selectedStroke = Stroke(width = selectedStrokePx, cap = StrokeCap.Butt)
                    val diameter = size.minDimension - strokePx
                    val topLeft = Offset(strokePx / 2f, strokePx / 2f)
                    val arcSize = Size(diameter, diameter)

                    var startAngle = -90f
                    val total = if (totalExpenses > 0L) totalExpenses.toFloat() else 1f

                    categories.forEachIndexed { index, cat ->
                        val sweepAngle = (cat.totalSpent.toFloat() / total) * 360f
                        val sliceColor = ExecutiveFinancialPalette[index % ExecutiveFinancialPalette.size]
                        val isSelected = selectedCategory?.categoryId == cat.categoryId

                        drawArc(
                            color = sliceColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = if (isSelected) selectedStroke else normalStroke
                        )
                        startAngle += sweepAngle
                    }
                }

                // المحتوى في مركز الدائرة المفرغة
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    if (selectedCategory != null) {
                        val selectedIndex = categories.indexOfFirst { it.categoryId == selectedCategory.categoryId }
                        val activeColor = if (selectedIndex >= 0) {
                            ExecutiveFinancialPalette[selectedIndex % ExecutiveFinancialPalette.size]
                        } else PrimaryGreen

                        Text(
                            text = selectedCategory.categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = activeColor,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${formatNumber(selectedCategory.totalSpent)} $currencySymbol",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${selectedCategory.percentage}% • ${selectedCategory.transactionCount} عمليات",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "إجمالي المصروفات",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${formatNumber(totalExpenses)} $currencySymbol",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${categories.size} تصنيفات",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // إذا كان هناك تصنيف محدد، نعرض زر لإلغاء التحديد
            if (selectedCategory != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تم تمييز: ${selectedCategory.categoryName}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = { onCategorySelect(null) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("إلغاء التحديد", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // قائمة بنود التصنيفات مع الألوان ومربعات الإيضاح والأيقونات والنسب المئوية
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEachIndexed { index, category ->
                    val isSelected = selectedCategory?.categoryId == category.categoryId
                    val sliceColor = ExecutiveFinancialPalette[index % ExecutiveFinancialPalette.size]
                    val catIcon = CategoryIconHelper.getIcon(category.iconKey)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onCategorySelect(category) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) sliceColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(1.5.dp, sliceColor)
                        } else null,
                        tonalElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // مؤشر اللون المالي، الأيقونة واسم التصنيف وعدد العمليات
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // مربع إيضاح لوحة الألوان المالية
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(sliceColor)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldLightBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
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
                                        text = "${category.transactionCount} عمليات مسجلة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // النسبة المئوية والمبلغ
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${formatNumber(category.totalSpent)} $currencySymbol",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                                Text(
                                    text = "${category.percentage}% من الإجمالي",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * الرسم الثاني: مخطط توزيع المصروفات عبر أسابيع الشهر (Weekly Spending Bar Chart)
 */
@Composable
private fun WeeklySpendingBarChartCard(
    weeklyList: List<WeeklySpendingItem>,
    peakPeriod: String?,
    averageWeekly: Long
) {
    val currencySymbol = LocalAppCurrency.current.symbolArabic
    val maxAmount = remember(weeklyList) {
        weeklyList.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
    }
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val guideLineDash = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "تغير المصروفات خلال أسابيع الشهر",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "توزيع مبالغ الإنفاق عبر فترات الشهر الأربع",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // رسم الخطوط الإرشادية وقضبان الأعمدة عبر Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 26.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val bottomY = canvasHeight

                    // رسم 3 خطوط إرشادية أفقية منقطة (مستويات 100% و 66% و 33%)
                    GridLevels.forEach { level ->
                        val y = bottomY * level
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = guideLineDash
                        )
                    }

                    // خط الأساس السفلي
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, bottomY),
                        end = Offset(canvasWidth, bottomY),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // رسم أعمدة الأسابيع
                    val barCount = weeklyList.size
                    if (barCount > 0) {
                        val barWidth = 36.dp.toPx()
                        val totalBarWidth = barWidth * barCount
                        val spacing = (canvasWidth - totalBarWidth) / (barCount + 1)

                        weeklyList.forEachIndexed { index, item ->
                            val barHeight = (item.amount.toFloat() / maxAmount.toFloat()) * bottomY
                            val leftX = spacing * (index + 1) + barWidth * index
                            val topY = bottomY - barHeight
                            val isPeak = peakPeriod != null && item.periodName == peakPeriod && item.amount > 0L

                            drawRoundRect(
                                color = if (isPeak) primaryColor else primaryColor.copy(alpha = 0.55f),
                                topLeft = Offset(leftX, topY),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }
                }

                // عرض القيم الرقمية أعلى الأعمدة وتسميات الأسابيع في الأسفل
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeklyList.forEach { item ->
                        val isPeak = peakPeriod != null && item.periodName == peakPeriod && item.amount > 0L
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatNumber(item.amount),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPeak) primaryColor else TextSecondary,
                                fontSize = 11.sp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = item.periodName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isPeak) primaryColor else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = item.daysLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // مؤشرات إحصائية لملخص الأسابيع
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "متوسط الإنفاق الأسبوعي: ${formatNumber(averageWeekly)} $currencySymbol",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = if (peakPeriod != null) "أعلى إنفاق: $peakPeriod" else "لا توجد مصروفات",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * الرسم الثالث: مخطط مقارنة المصروفات عبر الأشهر (Monthly Comparison Bar Chart)
 */
@Composable
private fun MonthlyComparisonBarChartCard(
    comparisons: List<MonthlyComparisonItem>,
    monthChangePercentage: Int?
) {
    val maxAmount = remember(comparisons) {
        comparisons.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
    }
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val guideLineDash = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مقارنة المصروفات مع الأشهر السابقة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تتبع مسار الإنفاق الشهري لآخر 6 أشهر",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // شارة نسبة التغير مقارنة بالشهر السابق
                if (monthChangePercentage != null) {
                    val isIncrease = monthChangePercentage > 0
                    val isDecrease = monthChangePercentage < 0
                    val badgeColor = when {
                        isIncrease -> ExpenseRed
                        isDecrease -> IncomeGreen
                        else -> TextSecondary
                    }
                    val badgeText = when {
                        isIncrease -> "+$monthChangePercentage%"
                        isDecrease -> "$monthChangePercentage%"
                        else -> "0%"
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    isIncrease -> Icons.AutoMirrored.Filled.TrendingUp
                                    isDecrease -> Icons.AutoMirrored.Filled.TrendingDown
                                    else -> Icons.Default.Check
                                },
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // رسم الخطوط الإرشادية وقضبان الأعمدة عبر Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 26.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val bottomY = canvasHeight

                    // رسم 3 خطوط إرشادية أفقية منقطة (مستويات 100% و 66% و 33%)
                    GridLevels.forEach { level ->
                        val y = bottomY * level
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = guideLineDash
                        )
                    }

                    // خط الأساس السفلي
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, bottomY),
                        end = Offset(canvasWidth, bottomY),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // رسم أعمدة مقارنة الأشهر الستة
                    val barCount = comparisons.size
                    if (barCount > 0) {
                        val barWidth = 26.dp.toPx()
                        val totalBarWidth = barWidth * barCount
                        val spacing = (canvasWidth - totalBarWidth) / (barCount + 1)

                        comparisons.forEachIndexed { index, item ->
                            val barHeight = if (maxAmount > 0L) {
                                (item.amount.toFloat() / maxAmount.toFloat()) * bottomY
                            } else 0f
                            val leftX = spacing * (index + 1) + barWidth * index
                            val topY = bottomY - barHeight

                            val barColor = if (item.isCurrentSelected) {
                                primaryColor
                            } else {
                                primaryColor.copy(alpha = 0.32f)
                            }

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(leftX, topY),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                            )
                        }
                    }
                }

                // عرض القيم الرقمية أعلى الأعمدة وتسميات الأشهر في الأسفل
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    comparisons.forEach { item ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatNumber(item.amount),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (item.isCurrentSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (item.isCurrentSelected) primaryColor else TextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = item.monthLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (item.isCurrentSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (item.isCurrentSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ملاحظة توضيحية للشهر المختار
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "الشهر المختار مظلل باللون الأخضر المميز",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
