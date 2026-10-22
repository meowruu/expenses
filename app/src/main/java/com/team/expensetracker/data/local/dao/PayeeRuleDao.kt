package com.team.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.team.expensetracker.data.local.entity.PayeeRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for payee categorization rules ([PayeeRuleEntity]).
 */
@Dao
interface PayeeRuleDao {

    /**
     * Looks up the user-assigned category for the specified UPI ID.
     *
     * @param upiId The payee UPI identifier.
     * @return The assigned category name if found, or null if no custom rule exists.
     */
    @Query("SELECT category FROM payee_rules WHERE LOWER(TRIM(upiId)) = LOWER(TRIM(:upiId)) LIMIT 1")
    suspend fun getCategoryFor(upiId: String): String?

    /**
     * Inserts or updates a payee category rule.
     *
     * @param rule The rule entity to save.
     * @return The row ID of the inserted/replaced entity.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRule(rule: PayeeRuleEntity): Long

    /**
     * Observes all configured payee rules ordered alphabetically by UPI ID.
     */
    @Query("SELECT * FROM payee_rules ORDER BY upiId ASC")
    fun observeAll(): Flow<List<PayeeRuleEntity>>

    /**
     * Deletes a payee category rule for the given UPI ID.
     */
    @Query("DELETE FROM payee_rules WHERE LOWER(TRIM(upiId)) = LOWER(TRIM(:upiId))")
    suspend fun delete(upiId: String): Int

    /**
     * Deletes all payee category rules.
     */
    @Query("DELETE FROM payee_rules")
    suspend fun deleteAll(): Int
}
