package com.masroofi.app.presentation.navigation

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
import com.masroofi.app.presentation.screens.home.HomeScreen
import com.masroofi.app.presentation.screens.reports.ReportsScreen
import com.masroofi.app.presentation.screens.settings.SettingsScreen
import com.masroofi.app.presentation.screens.transactions.AddTransactionScreen
import com.masroofi.app.presentation.screens.transactions.TransactionsHistoryScreen
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
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!showAddTransactionSheet) {
                MasroofiBottomNavigationBar(
                    currentScreen = currentScreen,
                    onScreenSelected = { currentScreen = it },
                    onAddClick = { showAddTransactionSheet = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    AppScreen.HOME -> HomeScreen(
                        onNavigateToTransactions = { currentScreen = AppScreen.TRANSACTIONS }
                    )
                    AppScreen.TRANSACTIONS -> TransactionsHistoryScreen()
                    AppScreen.REPORTS -> ReportsScreen()
                    AppScreen.SETTINGS -> SettingsScreen(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = onToggleDarkMode
                    )
                }
            }

            // شاشة إضافة عملية جديدة
            if (showAddTransactionSheet) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AddTransactionScreen(
                        onDismiss = { showAddTransactionSheet = false }
                    )
                }
            }
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
