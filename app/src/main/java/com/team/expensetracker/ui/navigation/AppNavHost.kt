package com.team.expensetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.team.expensetracker.data.CategorySummary
import com.team.expensetracker.data.model.TransactionType
import com.team.expensetracker.parser.Categories
import com.team.expensetracker.ui.screens.detail.TransactionDetailScreen
import com.team.expensetracker.ui.screens.entry.ManualEntryScreen
import com.team.expensetracker.ui.screens.home.HomeScreen
import com.team.expensetracker.ui.screens.lock.LockScreen
import com.team.expensetracker.ui.screens.settings.MyUpiIdsScreen
import com.team.expensetracker.ui.screens.settings.PermissionExplainerScreen
import com.team.expensetracker.ui.screens.settings.SettingsScreen
import com.team.expensetracker.ui.viewmodel.ExpenseViewModelFactory
import com.team.expensetracker.ui.viewmodel.HomeViewModel
import com.team.expensetracker.ui.viewmodel.ManualEntryViewModel
import com.team.expensetracker.ui.viewmodel.MyUpiIdsViewModel
import com.team.expensetracker.ui.viewmodel.SettingsViewModel
import com.team.expensetracker.ui.viewmodel.TransactionDetailViewModel

/**
 * Root navigation host managing screen navigation across Home, Manual Entry,
 * Transaction Detail, Settings, My UPI IDs, and Permission Explainer.
 *
 * Connects B2 ViewModels via [ExpenseViewModelFactory] and feeds reactive state
 * into the screens while maintaining state hoisting.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val viewModelFactory = remember(context) { ExpenseViewModelFactory.create(context) }

    val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    val manualEntryViewModel: ManualEntryViewModel = viewModel(factory = viewModelFactory)
    val transactionDetailViewModel: TransactionDetailViewModel = viewModel(factory = viewModelFactory)
    val myUpiIdsViewModel: MyUpiIdsViewModel = viewModel(factory = viewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)

    val transactions by homeViewModel.transactions.collectAsState()
    val monthTotalPaise by homeViewModel.monthTotalPaise.collectAsState()
    val categorySummary by homeViewModel.categorySummary.collectAsState()
    val myUpiIdEntities by myUpiIdsViewModel.upiIds.collectAsState()

    val categorySummaries = remember(categorySummary, transactions) {
        categorySummary.entries.map { (category, totalPaise) ->
            CategorySummary(
                category = category,
                totalPaise = totalPaise,
                transactionCount = transactions.count { it.category == category && it.type == TransactionType.DEBIT }
            )
        }.sortedByDescending { it.totalPaise }
    }

    // Placeholder function for Simulate SMS
    fun onSimulateSms() {
        // TODO: B3 will wire the real pipeline
    }

    NavHost(
        navController = navController,
        startDestination = Route.Home.route,
        modifier = modifier
    ) {
        // Lock Screen
        composable(route = Route.Lock.route) {
            LockScreen(
                onPinComplete = { _ ->
                    // B4: Replace this stub with real PIN verification
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Lock.route) { inclusive = true }
                    }
                },
                onBiometricClick = {
                    // B4: Connect real biometric authentication here
                }
            )
        }

        // 1. Home Dashboard Screen
        composable(route = Route.Home.route) {
            HomeScreen(
                thisMonthSpentPaise = monthTotalPaise,
                categoryTotals = categorySummary,
                categorySummaries = categorySummaries,
                recentTransactions = transactions,
                onTransactionClick = { transactionId ->
                    navController.navigate(Route.TransactionDetail.createRoute(transactionId))
                },
                onAddClick = {
                    navController.navigate(Route.ManualEntry.route)
                },
                onSettingsClick = {
                    navController.navigate(Route.Settings.route)
                },
                onSimulateSms = {
                    onSimulateSms()
                }
            )
        }

        // 2. Manual Expense Entry Screen
        composable(route = Route.ManualEntry.route) {
            ManualEntryScreen(
                categories = Categories.ALL.toList(),
                onSave = { amountPaise, note, category, dateMillis, upiId ->
                    manualEntryViewModel.addExpense(
                        amountPaise = amountPaise,
                        note = note,
                        category = category,
                        date = dateMillis,
                        upiId = upiId
                    ) { success ->
                        if (success) {
                            navController.popBackStack()
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 3. Transaction Detail Screen
        composable(
            route = Route.TransactionDetail.route,
            arguments = listOf(
                navArgument(Route.TransactionDetail.ARG_TRANSACTION_ID) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong(Route.TransactionDetail.ARG_TRANSACTION_ID) ?: 0L
            val transaction = transactions.firstOrNull { it.id == transactionId }

            if (transaction != null) {
                TransactionDetailScreen(
                    transaction = transaction,
                    categories = Categories.ALL.toList(),
                    onCategoryChange = { updatedCategory ->
                        transactionDetailViewModel.updateCategory(
                            id = transaction.id,
                            newCategory = updatedCategory,
                            upiId = transaction.upiId,
                            saveAsRule = true
                        )
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // 4. Settings Screen
        composable(route = Route.Settings.route) {
            SettingsScreen(
                onNavigateToMyUpiIds = {
                    navController.navigate(Route.MyUpiIds.route)
                },
                onNavigateToPermissionExplainer = {
                    navController.navigate(Route.PermissionExplainer.route)
                },
                onDeleteAllData = {
                    settingsViewModel.deleteAllData {
                        navController.popBackStack()
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 5. My UPI IDs Management Screen
        composable(route = Route.MyUpiIds.route) {
            val upiIds = remember(myUpiIdEntities) { myUpiIdEntities.map { it.upiId } }
            MyUpiIdsScreen(
                upiIds = upiIds,
                onAdd = { newUpiId ->
                    myUpiIdsViewModel.addUpiId(newUpiId)
                },
                onRemove = { upiIdToRemove ->
                    myUpiIdsViewModel.removeUpiId(upiIdToRemove)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 6. Permission Explainer Screen (SMS auto-logging onboarding)
        // B3 integration point: onEnableAutoLogging is a no-op stub.
        // Real READ_SMS / RECEIVE_SMS permission request must be wired here by B3.
        composable(route = Route.PermissionExplainer.route) {
            PermissionExplainerScreen(
                onEnableAutoLogging = {
                    // B3: Replace with real runtime SMS permission request.
                    // Do NOT add permission logic here — B3 owns this.
                },
                onNotNow = {
                    navController.popBackStack()
                }
            )
        }
    }
}
