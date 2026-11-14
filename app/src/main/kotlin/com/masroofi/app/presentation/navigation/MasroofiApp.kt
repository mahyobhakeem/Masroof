package com.masroofi.app.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masroofi.app.MasroofiApplication
import com.masroofi.app.domain.model.CurrencyCatalog
import com.masroofi.app.presentation.components.LocalAppCurrency
import com.masroofi.app.presentation.components.WelcomeCurrencyDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.masroofi.app.presentation.screens.home.HomeScreen
import com.masroofi.app.presentation.screens.home.HomeViewModel
import com.masroofi.app.presentation.screens.home.HomeViewModelFactory
import com.masroofi.app.presentation.screens.reports.ReportsScreen
import com.masroofi.app.presentation.screens.reports.ReportsViewModel
import com.masroofi.app.presentation.screens.reports.ReportsViewModelFactory
import com.masroofi.app.presentation.screens.settings.SettingsScreen
import com.masroofi.app.presentation.screens.settings.SettingsViewModel
import com.masroofi.app.presentation.screens.settings.SettingsViewModelFactory
import com.masroofi.app.presentation.screens.transactions.AddTransactionScreen
import com.masroofi.app.presentation.screens.transactions.TransactionFormViewModel
import com.masroofi.app.presentation.screens.transactions.TransactionFormViewModelFactory
import com.masroofi.app.presentation.screens.transactions.TransactionsHistoryScreen
import com.masroofi.app.presentation.screens.transactions.TransactionsHistoryViewModel
import com.masroofi.app.presentation.screens.transactions.TransactionsHistoryViewModelFactory
import com.masroofi.app.presentation.theme.TextSecondary

// تعداد شاشات التطبيق الرئيسية
enum class AppScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("الرئيسية", Icons.Filled.Home, Icons.Outlined.Home),
    TRANSACTIONS("العمليات", Icons.AutoMirrored.Filled.ListAlt, Icons.AutoMirrored.Outlined.ListAlt),
    REPORTS("التقارير", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    SETTINGS("الإعدادات", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun MasroofiApp(
    isDarkMode: Boolean = false,
    onToggleDarkMode: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MasroofiApplication
    val container = app.container

    val formViewModel: TransactionFormViewModel = viewModel(
        factory = TransactionFormViewModelFactory(
            transactionRepository = container.transactionRepository,
            categoryRepository = container.categoryRepository
        )
    )

    val historyViewModel: TransactionsHistoryViewModel = viewModel(
        factory = TransactionsHistoryViewModelFactory(
            transactionRepository = container.transactionRepository,
            categoryRepository = container.categoryRepository
        )
    )

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            getMonthlyFinancialSummaryUseCase = container.getMonthlyFinancialSummaryUseCase,
            setMonthlyBudgetUseCase = container.setMonthlyBudgetUseCase,
            transactionRepository = container.transactionRepository
        )
    )

    val reportsViewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModelFactory(
            getMonthlyReportUseCase = container.getMonthlyReportUseCase,
            transactionRepository = container.transactionRepository
        )
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            preferencesManager = container.preferencesManager,
            categoryRepository = container.categoryRepository,
            monthlyBudgetRepository = container.monthlyBudgetRepository,
            transactionRepository = container.transactionRepository,
            backupManager = container.backupManager,
            cloudBackupManager = container.cloudBackupManager
        )
    )

    val coroutineScope = rememberCoroutineScope()
    val selectedCurrencyCode by container.preferencesManager.selectedCurrencyCode.collectAsState(initial = "YER")
    val hasSelectedCurrency by container.preferencesManager.hasSelectedCurrency.collectAsState(initial = true)
    val activeCurrency = remember(selectedCurrencyCode) {
        CurrencyCatalog.getCurrencyByCode(selectedCurrencyCode)
    }

    var showWelcomeCurrencyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val hasSelected = container.preferencesManager.hasSelectedCurrency.first()
        if (!hasSelected) {
            val transactions = container.transactionRepository.getAllTransactions().first()
            if (transactions.isNotEmpty()) {
                // مستخدم سابق لديه عمليات مسجلة: اعتماد الريال اليمني وتفعيل الإشعار تلقائياً دون إزعاجه
                container.preferencesManager.setSelectedCurrencyCode("YER")
                container.preferencesManager.setHasSelectedCurrency(true)
            } else {
                // مستخدم جديد كلياً: إظهار نافذة الترحيب لاختيار عملته الرئيسية
                showWelcomeCurrencyDialog = true
            }
        }
    }

    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }

    // إدارة زر الرجوع في النظام: إغلاق النموذج أولاً، أو الرجوع إلى الشاشة الرئيسية
    BackHandler(enabled = showAddTransactionSheet) {
        showAddTransactionSheet = false
    }

    BackHandler(enabled = !showAddTransactionSheet && currentScreen != AppScreen.HOME) {
        currentScreen = AppScreen.HOME
    }

    CompositionLocalProvider(LocalAppCurrency provides activeCurrency) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!showAddTransactionSheet) {
                    MasroofiBottomNavigationBar(
                        currentScreen = currentScreen,
                        onScreenSelected = { currentScreen = it },
                        onAddClick = {
                            formViewModel.initForAdd()
                            showAddTransactionSheet = true
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    label = "ScreenTransition"
                ) { targetScreen ->
                    when (targetScreen) {
                        AppScreen.HOME -> HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToTransactions = { currentScreen = AppScreen.TRANSACTIONS },
                            onEditTransaction = { transaction ->
                                formViewModel.initForEdit(transaction)
                                showAddTransactionSheet = true
                            }
                        )
                        AppScreen.TRANSACTIONS -> TransactionsHistoryScreen(
                            viewModel = historyViewModel,
                            onEditTransaction = { transaction ->
                                formViewModel.initForEdit(transaction)
                                showAddTransactionSheet = true
                            }
                        )
                        AppScreen.REPORTS -> ReportsScreen(
                            viewModel = reportsViewModel
                        )
                        AppScreen.SETTINGS -> SettingsScreen(
                            viewModel = settingsViewModel
                        )
                    }
                }

                // شاشة إضافة / تعديل العملية المالية
                if (showAddTransactionSheet) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AddTransactionScreen(
                            viewModel = formViewModel,
                            onDismiss = { showAddTransactionSheet = false }
                        )
                    }
                }
            }
        }

        // نافذة الترحيب لاختيار العملة للمستخدم الجديد
        if (showWelcomeCurrencyDialog) {
            WelcomeCurrencyDialog(
                initialCurrencyCode = selectedCurrencyCode,
                onConfirm = { currencyCode ->
                    coroutineScope.launch {
                        container.preferencesManager.setSelectedCurrencyCode(currencyCode)
                        container.preferencesManager.setHasSelectedCurrency(true)
                        showWelcomeCurrencyDialog = false
                    }
                }
            )
        }
    }
}

// شريط التنقل السفلي المعتمد مع زر الإضافة المركزي البارز
@Composable
fun MasroofiBottomNavigationBar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    onAddClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // التبويب 1: الرئيسية
            BottomNavItem(
                screen = AppScreen.HOME,
                isSelected = currentScreen == AppScreen.HOME,
                onClick = { onScreenSelected(AppScreen.HOME) },
                modifier = Modifier.weight(1f)
            )

            // التبويب 2: العمليات
            BottomNavItem(
                screen = AppScreen.TRANSACTIONS,
                isSelected = currentScreen == AppScreen.TRANSACTIONS,
                onClick = { onScreenSelected(AppScreen.TRANSACTIONS) },
                modifier = Modifier.weight(1f)
            )

            // الزر المركزي البارز لإضافة عملية جديدة
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = onAddClick,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة عملية",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // التبويب 3: التقارير
            BottomNavItem(
                screen = AppScreen.REPORTS,
                isSelected = currentScreen == AppScreen.REPORTS,
                onClick = { onScreenSelected(AppScreen.REPORTS) },
                modifier = Modifier.weight(1f)
            )

            // التبويب 4: الإعدادات
            BottomNavItem(
                screen = AppScreen.SETTINGS,
                isSelected = currentScreen == AppScreen.SETTINGS,
                onClick = { onScreenSelected(AppScreen.SETTINGS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    screen: AppScreen,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
            contentDescription = screen.title,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = screen.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
            fontSize = 11.sp
        )
    }
}
