package com.team.expensetracker.parser

import com.team.expensetracker.data.model.PayeeRule
import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType

/**
 * Pure Kotlin categorizer interface for determining transaction expense categories.
 *
 * Architectural Invariants:
 * - Pure JVM logic: Free of Android framework and database dependencies.
 * - User Overrides First: Matching [PayeeRule] instances take absolute precedence over heuristics.
 * - Strict Contract Compliance: Emits ONLY category strings recognized by project contracts ([Categories]).
 */
interface TransactionCategorizer {

    /**
     * Determines the category for a candidate [ParsedTransaction].
     *
     * @param transaction The parsed transaction candidate.
     * @param rules The list of user-defined [PayeeRule] instances loaded from repository.
     * @return A category string recognized by [Categories].
     */
    fun categorize(
        transaction: ParsedTransaction,
        rules: List<PayeeRule> = emptyList()
    ): String

    /**
     * Determines the category from granular transaction metadata.
     *
     * @param merchant Clean merchant/payee name (nullable).
     * @param upiId Payee UPI identifier / VPA (nullable).
     * @param type The transaction direction ([TransactionType.DEBIT] or [TransactionType.CREDIT]).
     * @param rules The list of user-defined [PayeeRule] instances loaded from repository.
     * @return A category string recognized by [Categories].
     */
    fun categorize(
        merchant: String?,
        upiId: String?,
        type: TransactionType = TransactionType.DEBIT,
        rules: List<PayeeRule> = emptyList()
    ): String
}

/**
 * Pipeline convenience extension to produce a persistent [Transaction] directly with categorization applied.
 */
fun ParsedTransaction.toCategorizedTransaction(
    categorizer: TransactionCategorizer,
    rules: List<PayeeRule> = emptyList(),
    id: Long = 0L
): Transaction {
    val category = categorizer.categorize(this, rules)
    return toTransaction(category = category, id = id)
}
