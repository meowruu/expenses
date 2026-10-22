package com.team.expensetracker.ui.navigation

/**
 * Sealed Route class defining screen destinations for Navigation Compose.
 */
sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Lock : Route("lock")
    data object ManualEntry : Route("manual_entry")
    data object Settings : Route("settings")
    data object MyUpiIds : Route("my_upi_ids")
    data object PermissionExplainer : Route("permission_explainer")
    data object TransactionDetail : Route("transaction_detail/{transactionId}") {
        const val ARG_TRANSACTION_ID = "transactionId"
        fun createRoute(transactionId: Long): String = "transaction_detail/$transactionId"
    }
}
