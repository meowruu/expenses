package com.team.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.team.expensetracker.data.local.entity.SplitShareEntity
import com.team.expensetracker.data.local.model.PersonOwedShare
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for split expense shares ([SplitShareEntity]).
 */
@Dao
interface SplitShareDao {

    /**
     * Inserts a list of split shares into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShares(shares: List<SplitShareEntity>): List<Long>

    /**
     * Inserts a single split share.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShare(share: SplitShareEntity): Long

    /**
     * Observes all split shares for a particular transaction ordered by ID ascending.
     */
    @Query("SELECT * FROM split_shares WHERE transactionId = :transactionId ORDER BY id ASC")
    fun observeSharesByTransaction(transactionId: Long): Flow<List<SplitShareEntity>>

    /**
     * Fetches all split shares for a transaction synchronously.
     */
    @Query("SELECT * FROM split_shares WHERE transactionId = :transactionId ORDER BY id ASC")
    suspend fun getSharesByTransaction(transactionId: Long): List<SplitShareEntity>

    /**
     * Observes total paise owed per person across all transactions where settled = 0.
     * Excludes settled shares.
     */
    @Query(
        """
        SELECT personName, COALESCE(SUM(sharePaise), 0) AS totalOwedPaise
        FROM split_shares
        WHERE settled = 0
        GROUP BY personName
        ORDER BY totalOwedPaise DESC
        """
    )
    fun observeTotalsOwedPerPerson(): Flow<List<PersonOwedShare>>

    /**
     * Updates the settled state of a specific split share.
     */
    @Query("UPDATE split_shares SET settled = :settled WHERE id = :id")
    suspend fun markSettled(id: Long, settled: Boolean = true): Int

    /**
     * Deletes all split shares belonging to a specific transaction.
     */
    @Query("DELETE FROM split_shares WHERE transactionId = :transactionId")
    suspend fun deleteByTransaction(transactionId: Long): Int

    /**
     * Deletes all split shares from the table.
     * Used by B4's data wipe routine.
     */
    @Query("DELETE FROM split_shares")
    suspend fun deleteAll(): Int
}
