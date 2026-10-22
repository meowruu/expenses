package com.team.expensetracker.data.repository

import com.team.expensetracker.data.model.PayeeRule

/**
 * Repository interface for managing payee category mapping rules ([PayeeRule]).
 *
 * Used by the categorizer to query and persist user-configured category overrides for specific UPI IDs.
 */
interface PayeeRuleRepository {

    /**
     * Retrieves the user-assigned category for the specified UPI ID.
     *
     * @param upiId The payee UPI identifier (e.g. "swiggy@icici").
     * @return The assigned category name if a rule exists for [upiId], or `null` if no rule is found.
     */
    suspend fun getCategoryFor(upiId: String): String?

    /**
     * Saves or updates a category rule for the specified UPI ID.
     *
     * If a rule for [upiId] already exists, its category will be updated to [category].
     *
     * @param upiId The payee UPI identifier (primary key).
     * @param category The category name to associate with this payee.
     */
    suspend fun saveRule(upiId: String, category: String)
}
