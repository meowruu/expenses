package com.team.expensetracker.data.model

/**
 * Specifies the financial direction/type of a transaction.
 */
enum class TransactionType {
    /**
     * Debit transaction representing money spent or debited from an account.
     */
    DEBIT,

    /**
     * Credit transaction representing money received, credited, or refunded to an account.
     */
    CREDIT
}
