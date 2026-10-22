package com.team.expensetracker.data.repository

import com.team.expensetracker.data.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing and observing [Transaction] entities.
 *
 * Implementations are responsible for local persistent storage, deduplication,
 * and exposing reactive data streams for UI consumption.
 */
interface TransactionRepository {

    /**
     * Inserts a transaction into persistent storage.
     *
     * Deduplication requirement:
     * If a transaction with the same reference number ([Transaction.refNumber]) already exists
     * in the repository (and is non-null/non-blank), the insertion must be rejected and this method
     * must return `false`. If the transaction is successfully inserted, this method returns `true`.
     *
     * @param tx The [Transaction] entity to insert.
     * @return `true` if inserted successfully; `false` if rejected due to a duplicate reference number.
     */
    suspend fun insert(tx: Transaction): Boolean

    /**
     * Observes all recorded transactions ordered by date descending.
     *
     * @return A [Flow] emitting the updated list of all [Transaction] records.
     */
    fun observeAll(): Flow<List<Transaction>>

    /**
     * Observes the total expenditure (in paise) within the specified date range.
     *
     * Excludes self-transfers (transfers to/from user's own UPI IDs) and non-debit entries.
     *
     * @param monthStart Start epoch timestamp in milliseconds (inclusive).
     * @param monthEnd End epoch timestamp in milliseconds (inclusive).
     * @return A [Flow] emitting the total spending in paise for the given period.
     */
    fun observeMonthTotal(monthStart: Long, monthEnd: Long): Flow<Long>

    /**
     * Observes the category-wise spending breakdown within the specified date range.
     *
     * @param monthStart Start epoch timestamp in milliseconds (inclusive).
     * @param monthEnd End epoch timestamp in milliseconds (inclusive).
     * @return A [Flow] emitting a map of category names to total spending in paise for the given period.
     */
    fun observeCategorySummary(monthStart: Long, monthEnd: Long): Flow<Map<String, Long>>

    /**
     * Updates the category of an existing transaction.
     *
     * @param id The unique identifier ([Transaction.id]) of the transaction to update.
     * @param category The new category name to assign.
     */
    suspend fun updateCategory(id: Long, category: String)

    /**
     * Deletes all transactions from persistent storage.
     *
     * Supports the privacy data-wipe feature ("delete all data").
     */
    suspend fun deleteAll()
}
