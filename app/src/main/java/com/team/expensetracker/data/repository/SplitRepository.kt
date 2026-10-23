package com.team.expensetracker.data.repository

import com.team.expensetracker.data.model.SplitShare
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing local expense splitting and debtor tracking.
 *
 * Privacy Invariant:
 * Operates purely locally. No third-party APIs (such as Splitwise), no cloud sync,
 * and zero INTERNET permissions required.
 */
interface SplitRepository {

    /**
     * Splits an expense equally among the given list of [people].
     *
     * Remainder Distribution Invariant:
     * When totalPaise is not evenly divisible by people.size, the remainder paise
     * (totalPaise % people.size) is distributed 1 paise at a time across the first
     * N individuals. This guarantees that the sum of all individual share amounts
     * in paise matches [totalPaise] exactly, with 0 rounding loss.
     *
     * Existing shares for this [transactionId] are replaced.
     *
     * @param transactionId Primary key ID of the transaction being split.
     * @param totalPaise Total amount of the transaction in paise.
     * @param people Names of people participating in the split.
     * @return List of persisted [SplitShare] objects.
     */
    suspend fun splitEqually(
        transactionId: Long,
        totalPaise: Long,
        people: List<String>
    ): List<SplitShare>

    /**
     * Observes all split shares belonging to the given transaction ID.
     */
    fun observeSharesForTransaction(transactionId: Long): Flow<List<SplitShare>>

    /**
     * Observes a map of personName to total unsettled paise owed by that person.
     * Only includes shares where `settled = false`.
     */
    fun observeOwedPerPerson(): Flow<Map<String, Long>>

    /**
     * Updates the settlement status of an individual split share.
     *
     * @param shareId Primary key of the [SplitShare].
     * @param settled True if paid/settled, false if still owed.
     */
    suspend fun markSettled(shareId: Long, settled: Boolean = true)

    /**
     * Deletes all split shares associated with the specified transaction.
     */
    suspend fun deleteSharesForTransaction(transactionId: Long)

    /**
     * Deletes all split shares from the database.
     */
    suspend fun deleteAll()
}
