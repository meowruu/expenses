package com.team.expensetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.team.expensetracker.data.FakeData
import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType
import com.team.expensetracker.ui.screens.detail.TransactionDetailScreen
import com.team.expensetracker.ui.screens.entry.ManualEntryScreen
import com.team.expensetracker.ui.screens.home.HomeScreen
import com.team.expensetracker.ui.screens.lock.LockScreen
import com.team.expensetracker.ui.screens.settings.MyUpiIdsScreen
import com.team.expensetracker.ui.screens.settings.PermissionExplainerScreen
import com.team.expensetracker.ui.screens.settings.SettingsScreen
import java.util.concurrent.atomic.AtomicLong

/**
 * Root navigation host managing screen navigation across Home, Manual Entry,
 * Transaction Detail, Settings, and My UPI IDs.
 *
 * Implements pure state hoisting: screens take plain data + lambdas, zero ViewModel coupling.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val idCounter = remember { AtomicLong(100L) }

    // In-memory transaction state initialized with FakeData
    val transactions = remember {
        mutableStateListOf<Transaction>().apply {
            addAll(FakeData.initialTransactions)
        }
    }

    // In-memory list of user-owned UPI IDs
    val myUpiIds = remember {
        mutableStateListOf("user@okhdfcbank", "user@axisbank")
    }

    val thisMonthSpentPaise = remember(transactions.toList()) {
        FakeData.calculateTotalSpent(transactions)
    }

    val categoryTotals = remember(transactions.toList()) {
        FakeData.calculateCategoryTotals(transactions)
    }

    val categorySummaries = remember(transactions.toList()) {
        FakeData.calculateCategorySummaries(transactions)
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
                thisMonthSpentPaise = thisMonthSpentPaise,
                categoryTotals = categoryTotals,
                categorySummaries = categorySummaries,
                recentTransactions = transactions.toList(),
                onTransactionClick = { transactionId ->
                    navController.navigate(Route.TransactionDetail.createRoute(transactionId))
                },
                onAddClick = {
                    navController.navigate(Route.ManualEntry.route)
                },
                onSettingsClick = {
                    navController.navigate(Route.Settings.route)
                },
                onSimulateSms = { _ ->
                    // Stub for B3 integration
                }
            )
        }

        // 2. Manual Expense Entry Screen
        composable(route = Route.ManualEntry.route) {
            ManualEntryScreen(
                categories = FakeData.CATEGORIES,
                onSave = { amountPaise, note, category, dateMillis, upiId ->
                    val newTransaction = Transaction(
                        id = idCounter.incrementAndGet(),
                        amountPaise = amountPaise,
                        type = TransactionType.DEBIT,
                        merchant = note,
                        upiId = upiId,
                        refNumber = "MANUAL/${System.currentTimeMillis().toString().takeLast(8)}",
                        date = dateMillis,
                        bank = "CASH/UPI",
                        category = category
                    )
                    transactions.add(0, newTransaction)
                    navController.popBackStack()
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
            val transactionIndex = transactions.indexOfFirst { it.id == transactionId }

            if (transactionIndex != -1) {
                val transaction = transactions[transactionIndex]
                TransactionDetailScreen(
                    transaction = transaction,
                    categories = FakeData.CATEGORIES,
                    onCategoryChange = { updatedCategory ->
                        transactions[transactionIndex] = transaction.copy(category = updatedCategory)
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
                    transactions.clear()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 5. My UPI IDs Management Screen
        composable(route = Route.MyUpiIds.route) {
            MyUpiIdsScreen(
                upiIds = myUpiIds.toList(),
                onAdd = { newUpiId ->
                    myUpiIds.add(newUpiId)
                },
                onRemove = { upiIdToRemove ->
                    myUpiIds.remove(upiIdToRemove)
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
