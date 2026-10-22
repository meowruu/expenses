package com.team.expensetracker.data.model

/**
 * Represents a user-configured categorization rule mapping a UPI payee ID to an expense category.
 *
 * When an incoming transaction with a matching [upiId] is processed, the categorizer prioritizes
 * this user-configured rule over default heuristics.
 *
 * @property upiId The UPI ID of the payee (primary key, e.g. "swiggy@icici").
 * @property category The category assigned by the user for this payee (e.g. "Food").
 */
data class PayeeRule(
    val upiId: String,
    val category: String
)
