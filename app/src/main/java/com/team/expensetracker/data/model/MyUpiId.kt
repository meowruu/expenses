package com.team.expensetracker.data.model

/**
 * Represents a UPI identifier belonging to the application user.
 *
 * Used to identify self-transfers between user-owned accounts (e.g., transfers between
 * checking accounts or wallet top-ups). Transactions matching a user-owned UPI ID are excluded
 * from expense spending totals.
 *
 * @property upiId The user's UPI identifier (primary key, e.g. "user@okhdfcbank").
 */
data class MyUpiId(
    val upiId: String
)
