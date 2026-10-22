package com.team.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.team.expensetracker.data.local.entity.TransactionEntity
import com.team.expensetracker.data.local.model.CategorySpending
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [TransactionEntity] persistence operations.
 */
@Dao
interface TransactionDao {

    /**
     * Inserts a transaction entity.
     * Uses [OnConflictStrategy.IGNORE] to safely prevent inserting duplicates when
     * a unique index conflict occurs (e.g. on [TransactionEntity.refNumber]).
     *
     * @param transaction The entity to persist.
     * @return The newly generated row ID, or -1 if the insert was ignored due to conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    /**
     * Checks if a transaction with the given reference number already exists in the database.
     *
     * @param refNumber The bank reference / UTR number to check.
     * @return true if a transaction with [refNumber] exists; false otherwise.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE refNumber = :refNumber AND refNumber IS NOT NULL AND TRIM(refNumber) != '')")
    suspend fun existsByRefNumber(refNumber: String): Boolean

    /**
     * Observes all recorded transactions ordered chronologically descending (newest first).
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    /**
     * Fetches a single transaction by its primary key ID.
     */
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    /**
     * Observes total expense expenditure (in paise) within the given timestamp window.
     *
     * Excludes:
     * 1. Non-DEBIT transactions (e.g. CREDIT, refunds).
     * 2. Self-transfers: transactions where the payee [upiId] belongs to the user (present in `my_upi_ids`).
     *
     * Because this query reads from `my_upi_ids`, Room automatically re-emits when either
     * transactions or own-UPI IDs change.
     *
     * @param monthStart Epoch timestamp in milliseconds (inclusive).
     * @param monthEnd Epoch timestamp in milliseconds (inclusive).
     * @return A [Flow] emitting the aggregated spending in paise. Defaults to 0 when table is empty.
     */
    @Query(
        """
        SELECT COALESCE(SUM(amountPaise), 0)
        FROM transactions
        WHERE type = 'DEBIT'
          AND date >= :monthStart AND date <= :monthEnd
          AND (upiId IS NULL OR TRIM(upiId) = '' OR LOWER(TRIM(upiId)) NOT IN (SELECT LOWER(TRIM(upiId)) FROM my_upi_ids WHERE upiId IS NOT NULL))
        """
    )
    fun observeMonthTotal(monthStart: Long, monthEnd: Long): Flow<Long>

    /**
     * Observes category-wise aggregated expenditure (in paise) within the given timestamp window.
     *
     * Excludes non-DEBIT transactions and self-transfers involving user-owned UPI IDs.
     * Automatically re-emits on transaction or own-UPI ID changes.
     *
     * @param monthStart Epoch timestamp in milliseconds (inclusive).
     * @param monthEnd Epoch timestamp in milliseconds (inclusive).
     * @return A [Flow] emitting the list of category spending totals.
     */
    @Query(
        """
        SELECT category, COALESCE(SUM(amountPaise), 0) AS totalPaise
        FROM transactions
        WHERE type = 'DEBIT'
          AND date >= :monthStart AND date <= :monthEnd
          AND (upiId IS NULL OR TRIM(upiId) = '' OR LOWER(TRIM(upiId)) NOT IN (SELECT LOWER(TRIM(upiId)) FROM my_upi_ids WHERE upiId IS NOT NULL))
        GROUP BY category
        ORDER BY totalPaise DESC
        """
    )
    fun observeCategorySummary(monthStart: Long, monthEnd: Long): Flow<List<CategorySpending>>

    /**
     * Updates the category of an existing transaction.
     *
     * @param id The transaction primary key ID.
     * @param category The new category name.
     * @return The number of rows updated (1 if found, 0 if not).
     */
    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String): Int

    /**
     * Deletes all transactions.
     * Used by B4's privacy data-wipe routine.
     *
     * @return The number of deleted rows.
     */
    @Query("DELETE FROM transactions")
    suspend fun deleteAll(): Int
}
