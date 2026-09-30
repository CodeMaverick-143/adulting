package com.example.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.bills.RecurringBillsManagementScreen
import com.example.ui.forecast.MonthByMonthForecastScreen
import com.example.ui.income.IncomeManagementScreen
import com.example.ui.planner.MonthlyFinancialPlannerScreen
import com.example.ui.transactions.TransactionsScreen
import com.example.ui.viewmodel.FinancialPlannerViewModel

enum class AppDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    PLANNER("planner", "Planner", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    INCOME("income", "Income", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    BILLS("bills", "Bills", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    FORECAST("forecast", "Forecast", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
    TRANSACTIONS("transactions", "History", Icons.Filled.History, Icons.Outlined.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdultingAppContainer(viewModel: FinancialPlannerViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Triggers for cross-screen flows
    var triggerAddBill by remember { mutableStateOf(false) }
    var triggerAddIncome by remember { mutableStateOf(false) }
    var triggerEditBillId by remember { mutableStateOf<Long?>(null) }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Adulting App Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Adulting",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Monthly Financial Planner",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("app_navigation_bar")) {
                AppDestination.entries.forEach { destination ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title
                            )
                        },
                        label = { Text(destination.title) },
                        modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.PLANNER.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.PLANNER.route) {
                MonthlyFinancialPlannerScreen(
                    viewModel = viewModel,
                    onNavigateToAddBill = {
                        triggerAddBill = true
                        navController.navigate(AppDestination.BILLS.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAddIncome = {
                        triggerAddIncome = true
                        navController.navigate(AppDestination.INCOME.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToEditBill = { billId ->
                        triggerEditBillId = billId
                        navController.navigate(AppDestination.BILLS.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(AppDestination.INCOME.route) {
                IncomeManagementScreen(
                    viewModel = viewModel,
                    openAddDialogTrigger = triggerAddIncome,
                    onAddDialogClosed = { triggerAddIncome = false }
                )
            }

            composable(AppDestination.BILLS.route) {
                RecurringBillsManagementScreen(
                    viewModel = viewModel,
                    openAddDialogTrigger = triggerAddBill,
                    editBillIdTrigger = triggerEditBillId,
                    onTriggersConsumed = {
                        triggerAddBill = false
                        triggerEditBillId = null
                    }
                )
            }

            composable(AppDestination.FORECAST.route) {
                MonthByMonthForecastScreen(
                    viewModel = viewModel,
                    onSelectMonthToPlan = { targetMonth ->
                        viewModel.selectMonth(targetMonth)
                        navController.navigate(AppDestination.PLANNER.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(AppDestination.TRANSACTIONS.route) {
                TransactionsScreen(viewModel = viewModel)
            }
        }
    }
}
