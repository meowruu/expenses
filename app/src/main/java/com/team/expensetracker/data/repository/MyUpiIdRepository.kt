package com.team.expensetracker.data.repository

import com.team.expensetracker.data.model.MyUpiId
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user-owned UPI IDs ([MyUpiId]).
 *
 * Used to identify self-transfers so that internal money transfers between the user's
 * own bank accounts / wallets are excluded from expense totals.
 */
interface MyUpiIdRepository {

    /**
     * Observes the complete list of user-owned UPI IDs.
     *
     * @return A [Flow] emitting the updated list of [MyUpiId] records.
     */
    fun observeAll(): Flow<List<MyUpiId>>

    /**
     * Adds a user-owned UPI ID to the repository.
     *
     * @param id The UPI ID string to add (e.g., "myname@okhdfcbank").
     */
    suspend fun add(id: String)

    /**
     * Removes a user-owned UPI ID from the repository.
     *
     * @param id The UPI ID string to remove.
     */
    suspend fun remove(id: String)

    /**
     * Checks whether the specified UPI ID belongs to the user.
     *
     * @param id The UPI ID string to verify.
     * @return `true` if the UPI ID is registered as user-owned; `false` otherwise.
     */
    suspend fun isMine(id: String): Boolean
}
