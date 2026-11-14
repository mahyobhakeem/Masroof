package com.masroofi.app.presentation.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masroofi.app.data.preferences.ThemeMode
import com.masroofi.app.domain.model.Category
import com.masroofi.app.domain.model.CurrencyCatalog
import com.masroofi.app.domain.model.TransactionType
import com.masroofi.app.presentation.components.ChangePasswordDialog
import com.masroofi.app.presentation.components.CloudAccountAuthSheet
import com.masroofi.app.presentation.components.CloudBackupCard
import com.masroofi.app.presentation.components.CloudBackupFoundAutoPromptDialog
import com.masroofi.app.presentation.components.CloudRestoreConfirmationDialog
import com.masroofi.app.presentation.components.CurrencyChangeConfirmationDialog
import com.masroofi.app.presentation.components.CurrencySelectionSheet
import com.masroofi.app.presentation.components.DeleteAccountConfirmationDialog
import com.masroofi.app.presentation.components.GoogleSignInDialog
import com.masroofi.app.presentation.components.PrivacyPolicyDialog
import com.masroofi.app.presentation.components.LocalAppCurrency
import com.masroofi.app.presentation.components.MasroofTopHeaderBar
import com.masroofi.app.presentation.components.MasroofiExpandableCard
import com.masroofi.app.presentation.components.MasroofiItemCard
import com.masroofi.app.presentation.components.PdfExportDialog
import com.masroofi.app.presentation.components.PdfGeneratedSuccessDialog
import com.masroofi.app.utils.PdfReportExporter
import com.masroofi.app.presentation.theme.*
import com.masroofi.app.utils.formatNumber
import java.io.File
import java.time.LocalDate

/**
 * شاشة الإعدادات المربوطة بـ SettingsViewModel و Room و DataStore
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshLocalBackups(context)
    }

    // مشغلات إطار وصول التخزين القياسي (Storage Access Framework)
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToUri(context, uri)
        } else {
            viewModel.createBackupLocally(context)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.prepareRestoreFromUri(context, it) }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportCsvLocallyAndToUri(context, it) }
    }

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
        onTriggerBackup = {
            val dateStr = java.time.LocalDate.now().toString()
            backupLauncher.launch("masroof_backup_$dateStr.json")
        },
        onTriggerRestore = {
            viewModel.openRestoreSelection(context)
        },
        onTriggerCsvExport = {
            viewModel.exportCsvLocallyAndToUri(context, null)
        },
        onSelectExternalRestore = {
            restoreLauncher.launch(arrayOf("*/*"))
        },
        onSelectLocalRestore = { file ->
            viewModel.restoreFromLocalFile(file)
        },
        onCloseRestoreSelection = {
            viewModel.closeRestoreSelection()
        },
        onConfirmRestore = { viewModel.confirmRestore() },
        onCancelRestore = { viewModel.cancelRestore() },
        onOpenCurrencySelection = { viewModel.openCurrencySelection() },
        onCloudSignInClick = { viewModel.openGoogleSignInDialog() },
        onCloudSignOutClick = { viewModel.signOutFromGoogle() },
        onAutoBackupToggle = { viewModel.toggleAutoBackup(it) },
        onBackupNowClick = { viewModel.backupToCloudNow() },
        onCloudRestoreClick = { viewModel.requestCloudRestore() },
        onOpenPrivacyPolicy = { viewModel.openPrivacyPolicy() },
        onClosePrivacyPolicy = { viewModel.closePrivacyPolicy() },
        onOpenDeleteAccountDialog = { viewModel.openDeleteAccountDialog() },
        onCloseDeleteAccountDialog = { viewModel.closeDeleteAccountDialog() },
        onConfirmDeleteAccount = { wipe -> viewModel.deleteAccountAndCloudData(wipe) },
        onOpenChangePasswordDialog = { viewModel.openChangePasswordDialog() },
        onTriggerPdfExport = { viewModel.openPdfExportDialog() },
        modifier = modifier
    )

    val activeCurrency = LocalAppCurrency.current
    if (uiState.isPdfExportDialogOpen) {
        PdfExportDialog(
            onDismiss = { viewModel.closePdfExportDialog() },
            onConfirmExport = { periodMode, specificDate, customStart, customEnd, typeFilter ->
                viewModel.exportPdfReport(
                    context = context,
                    periodMode = periodMode,
                    specificDate = specificDate,
                    customStartDate = customStart,
                    customEndDate = customEnd,
                    typeFilter = typeFilter,
                    currencySymbol = activeCurrency.symbolArabic
                )
            }
        )
    }

    uiState.generatedPdfFile?.let { pdfFile ->
        PdfGeneratedSuccessDialog(
            pdfFile = pdfFile,
            onDismiss = { viewModel.dismissGeneratedPdfDialog() },
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

    if (uiState.showCloudAuthSheet || uiState.showGoogleSignInDialog) {
        CloudAccountAuthSheet(
            isLoading = uiState.isCloudLoading,
            onDismiss = { viewModel.dismissCloudAuthSheet() },
            onSignUp = { name, email, pass -> viewModel.signUpWithEmail(name, email, pass) },
            onSignIn = { email, pass -> viewModel.signInWithEmail(email, pass) },
            onForgotPassword = { email -> viewModel.sendPasswordResetEmail(email) }
        )
    }

    if (uiState.showChangePasswordDialog) {
        ChangePasswordDialog(
            isLoading = uiState.isCloudLoading,
            onDismiss = { viewModel.dismissChangePasswordDialog() },
            onConfirmChange = { newPass -> viewModel.changePassword(newPass) }
        )
    }

    val detectedBackup = uiState.detectedCloudBackup
    if (uiState.showCloudBackupFoundPrompt && detectedBackup != null) {
        CloudBackupFoundAutoPromptDialog(
            metadata = detectedBackup,
            onDismiss = { viewModel.dismissCloudBackupFoundPrompt() },
            onConfirmRestore = { viewModel.confirmAutoDetectedRestore() }
        )
    }

    if (uiState.isPrivacyPolicyOpen) {
        PrivacyPolicyDialog(
            onDismiss = { viewModel.closePrivacyPolicy() }
        )
    }

    if (uiState.isDeleteAccountDialogOpen) {
        DeleteAccountConfirmationDialog(
            onDismiss = { viewModel.closeDeleteAccountDialog() },
            onExportLocalFirst = { viewModel.createBackupLocally(context) },
            onConfirmDelete = { wipe -> viewModel.deleteAccountAndCloudData(wipe) }
        )
    }

    val pendingCloudMetadata = uiState.pendingCloudRestoreMetadata
    if (uiState.isCloudRestoreConfirmationOpen && pendingCloudMetadata != null) {
        CloudRestoreConfirmationDialog(
            metadata = pendingCloudMetadata,
            onDismiss = { viewModel.dismissCloudRestoreConfirmation() },
            onConfirmRestore = { viewModel.confirmCloudRestore() }
        )
    }

    if (uiState.isCurrencySelectionOpen) {
        CurrencySelectionSheet(
            selectedCurrencyCode = uiState.selectedCurrencyCode,
            onCurrencySelected = { viewModel.selectCurrency(it) },
            onDismiss = { viewModel.dismissCurrencySelection() }
        )
    }

    if (uiState.showCurrencyChangeWarningDialog) {
        uiState.pendingCurrencyCode?.let { code ->
            val newCurrency = CurrencyCatalog.getCurrencyByCode(code)
            CurrencyChangeConfirmationDialog(
                newCurrency = newCurrency,
                onConfirm = { viewModel.confirmCurrencyChange() },
                onDismiss = { viewModel.dismissCurrencyChangeWarning() }
            )
        }
    }
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
    onTriggerBackup: () -> Unit = {},
    onTriggerRestore: () -> Unit = {},
    onTriggerCsvExport: () -> Unit = {},
    onTriggerPdfExport: () -> Unit = {},
    onSelectExternalRestore: () -> Unit = {},
    onSelectLocalRestore: (File) -> Unit = {},
    onCloseRestoreSelection: () -> Unit = {},
    onConfirmRestore: () -> Unit = {},
    onCancelRestore: () -> Unit = {},
    onOpenCurrencySelection: () -> Unit = {},
    onCloudSignInClick: () -> Unit = {},
    onCloudSignOutClick: () -> Unit = {},
    onAutoBackupToggle: (Boolean) -> Unit = {},
    onBackupNowClick: () -> Unit = {},
    onCloudRestoreClick: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onClosePrivacyPolicy: () -> Unit = {},
    onOpenDeleteAccountDialog: () -> Unit = {},
    onCloseDeleteAccountDialog: () -> Unit = {},
    onConfirmDeleteAccount: (Boolean) -> Unit = {},
    onOpenChangePasswordDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // إدارة حالة الأقسام القابلة للتوسيع للتحكم في زر الرجوع خطوة بخطوة
    var isCustomizationExpanded by remember { mutableStateOf(false) }
    var isCloudExpanded by remember { mutableStateOf(false) }
    var isBackupExpanded by remember { mutableStateOf(false) }

    val anySectionExpanded = isCustomizationExpanded || isCloudExpanded || isBackupExpanded

    BackHandler(enabled = anySectionExpanded) {
        // إغلاق أي قسم مفتوح خطوة للخلف والبقاء داخل شاشة الإعدادات
        isCustomizationExpanded = false
        isCloudExpanded = false
        isBackupExpanded = false
    }

    // عرض رسائل التنبيه والنجاح
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            snackbarHostState.showSnackbar(msg)
            onClearMessages()
        }
        uiState.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            snackbarHostState.showSnackbar(msg)
            onClearMessages()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            MasroofTopHeaderBar(
                title = "الإعدادات",
                subtitle = "تخصيص المظهر وإدارة الميزانيات والنسخ الاحتياطي",
                icon = Icons.Default.Settings
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

            // 1. بطاقة مظهر التطبيق (3 أوضاع: فاتح، داكن، تلقائي)
            item {
                ThemeModeSelectorCard(
                    currentMode = uiState.themeMode,
                    onModeSelect = onSetThemeMode
                )
            }

            // 2. بطاقة إدارة الميزانية الشهرية
            item {
                val currencySymbol = LocalAppCurrency.current.symbolArabic
                val budgetText = if (uiState.currentMonthBudget != null) {
                    "${formatNumber(uiState.currentMonthBudget)} $currencySymbol للشهر الحالي"
                } else {
                    "غير محددة للشهر الحالي • انقر للإدارة"
                }
                MasroofiItemCard(
                    title = "إدارة الميزانية الشهرية",
                    subtitle = budgetText,
                    icon = Icons.Default.AccountBalanceWallet,
                    iconColor = PrimaryGreen,
                    onClick = onOpenBudgetManagement
                )
            }

            // 2. بطاقة إدارة التصنيفات المالية
            item {
                MasroofiItemCard(
                    title = "إدارة التصنيفات المالية",
                    subtitle = "${uiState.allCategories.size} تصنيفاً (المصروفات والإيرادات)",
                    icon = Icons.Default.Category,
                    iconColor = PrimaryGreen,
                    onClick = onOpenCategoryManagement
                )
            }

            // 3. بطاقة تخصيص التطبيق والعملة (قسم قابل للتوسيع)
            item {
                val currentCurrency = remember(uiState.selectedCurrencyCode) {
                    CurrencyCatalog.getCurrencyByCode(uiState.selectedCurrencyCode)
                }

                MasroofiExpandableCard(
                    title = "تخصيص التطبيق والعملة",
                    icon = Icons.Default.Tune,
                    iconColor = PrimaryGreen,
                    isExpanded = isCustomizationExpanded,
                    onExpandedChange = { isCustomizationExpanded = it }
                ) {
                    // بطاقة اختيار العملة الرئيسية التفاعلية
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenCurrencySelection)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "العملة الرئيسية",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "انقر لتغيير عملة الحسابات والعرض",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentCurrency.flagEmoji,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = "${currentCurrency.nameArabic} (${currentCurrency.code})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
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

            // 5. بطاقة الحساب والنسخ الاحتياطي السحابي (Google Cloud Backup)
            item {
                CloudBackupCard(
                    currentUser = uiState.currentUser,
                    backupStatus = uiState.cloudBackupStatus,
                    lastBackupTime = uiState.lastCloudBackupTime,
                    isAutoBackupEnabled = uiState.isAutoBackupEnabled,
                    isCloudLoading = uiState.isCloudLoading,
                    onSignInClick = onCloudSignInClick,
                    onSignOutClick = onCloudSignOutClick,
                    onAutoBackupToggle = onAutoBackupToggle,
                    onBackupNowClick = onBackupNowClick,
                    onRestoreClick = onCloudRestoreClick,
                    onDeleteAccountClick = onOpenDeleteAccountDialog,
                    onPrivacyPolicyClick = onOpenPrivacyPolicy,
                    onChangePasswordClick = onOpenChangePasswordDialog,
                    isExpanded = isCloudExpanded,
                    onExpandedChange = { isCloudExpanded = it }
                )
            }

            // 6. بطاقة النسخ الاحتياطي وتصدير البيانات (JSON & CSV)
            item {
                MasroofiExpandableCard(
                    title = "النسخ الاحتياطي وتصدير البيانات",
                    icon = Icons.Default.CloudSync,
                    iconColor = PrimaryGreen,
                    isExpanded = isBackupExpanded,
                    onExpandedChange = { isBackupExpanded = it }
                ) {
                    // أ) إنشاء نسخة احتياطية JSON
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTriggerBackup() }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryGreen.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إنشاء نسخة احتياطية (JSON)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "حفظ نسخة كاملة من العمليات والتصنيفات والميزانيات محلياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.8.dp)

                    // ب) استعادة نسخة احتياطية JSON
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTriggerRestore() }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryGreen.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsBackupRestore,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "استعادة نسخة احتياطية",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "استرجاع البيانات مع فحص الأمان وتأكيد الاستبدال",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.8.dp)

                    // ج) تصدير سجل العمليات CSV
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTriggerCsvExport() }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryGreen.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تصدير العمليات (CSV)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تصدير ملف جدول Excel متوافق تماماً مع العربية",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.8.dp)

                    // د) تصدير تقرير مالي PDF
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTriggerPdfExport() }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryGreen.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تصدير تقرير مالي (PDF)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "توليد مستند مالي منسق للمصروفات والإيرادات والمشاركة",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (uiState.localBackupFiles.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "النسخ الاحتياطية المتوفرة محلياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${uiState.localBackupFiles.size} نسخة",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 7. بطاقة سياسة الخصوصية
            item {
                MasroofiItemCard(
                    title = "سياسة الخصوصية",
                    subtitle = "حماية بياناتك، العمل المحلي 100%، والنسخ السحابي الاختياري",
                    icon = Icons.Default.Security,
                    iconColor = PrimaryGreen,
                    onClick = onOpenPrivacyPolicy
                )
            }

            // 8. بطاقة حول تطبيق مصروف
            item {
                MasroofiItemCard(
                    title = "عن تطبيق مصروف",
                    subtitle = "الإصدار 1.1.0 • تطبيق محلي لإدارة المصروفات",
                    icon = Icons.Default.Info,
                    iconColor = PrimaryGreen,
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
                        text = "مصروف | Masroof",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "الإصدار 1.1.0 • يعمل محلياً دون إنترنت",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
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

    // نافذة اختيار ملف النسخة الاحتياطية (محلية أو خارجية)
    if (uiState.isRestoreSelectionOpen) {
        RestoreSelectionDialog(
            localFiles = uiState.localBackupFiles,
            onSelectExternalFile = onSelectExternalRestore,
            onSelectLocalFile = onSelectLocalRestore,
            onDismiss = onCloseRestoreSelection
        )
    }

    // نافذة تأكيد استعادة النسخة الاحتياطية
    if (uiState.isRestoreConfirmationOpen && uiState.pendingRestoreSummary != null) {
        RestoreConfirmationDialog(
            summary = uiState.pendingRestoreSummary,
            onConfirm = onConfirmRestore,
            onDismiss = onCancelRestore
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.5.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // حاوية الأيقونة المربعة بزوايا مستديرة ناعمة موحدة
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (currentMode) {
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                        },
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "مظهر التطبيق",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
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
