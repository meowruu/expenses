package com.team.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.team.expensetracker.data.local.entity.MyUpiIdEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user-owned UPI IDs ([MyUpiIdEntity]).
 */
@Dao
interface MyUpiIdDao {

    /**
     * Observes all user-owned UPI IDs ordered alphabetically.
     */
    @Query("SELECT * FROM my_upi_ids ORDER BY upiId ASC")
    fun observeAll(): Flow<List<MyUpiIdEntity>>

    /**
     * Adds a user-owned UPI ID. If it already exists, the insert is ignored.
     *
     * @param myUpiId The entity to insert.
     * @return The row ID, or -1 if ignored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(myUpiId: MyUpiIdEntity): Long

    /**
     * Removes a user-owned UPI ID.
     *
     * @param id The UPI ID string to remove.
     * @return Number of rows deleted.
     */
    @Query("DELETE FROM my_upi_ids WHERE LOWER(TRIM(upiId)) = LOWER(TRIM(:id))")
    suspend fun remove(id: String): Int

    /**
     * Checks if a UPI ID is registered as user-owned.
     *
     * @param id The UPI ID string to verify.
     * @return true if owned by user; false otherwise.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM my_upi_ids WHERE LOWER(TRIM(upiId)) = LOWER(TRIM(:id)) LIMIT 1)")
    suspend fun isMine(id: String): Boolean

    /**
     * Deletes all user-owned UPI IDs.
     */
    @Query("DELETE FROM my_upi_ids")
    suspend fun deleteAll(): Int
}
