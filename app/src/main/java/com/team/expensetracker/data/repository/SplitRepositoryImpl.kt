package com.team.expensetracker.data.repository

import com.team.expensetracker.data.local.dao.SplitShareDao
import com.team.expensetracker.data.local.entity.SplitShareEntity
import com.team.expensetracker.data.local.entity.toDomain
import com.team.expensetracker.data.model.SplitShare
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Production implementation of [SplitRepository] backed by Room database.
 */
class SplitRepositoryImpl(
    private val dao: SplitShareDao
) : SplitRepository {

    override suspend fun splitEqually(
        transactionId: Long,
        totalPaise: Long,
        people: List<String>
    ): List<SplitShare> {
        val cleanedPeople = people.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanedPeople.isEmpty() || totalPaise <= 0L) {
            return emptyList()
        }

        val count = cleanedPeople.size
        val baseShare = totalPaise / count
        val remainder = totalPaise % count

        val entities = cleanedPeople.mapIndexed { index, person ->
            val share = if (index < remainder) baseShare + 1L else baseShare
            SplitShareEntity(
                transactionId = transactionId,
                personName = person,
                sharePaise = share,
                settled = false
            )
        }

        // Clean up prior split shares for this transaction before inserting new shares
        dao.deleteByTransaction(transactionId)
        dao.insertShares(entities)

        return dao.getSharesByTransaction(transactionId).map { it.toDomain() }
    }

    override fun observeSharesForTransaction(transactionId: Long): Flow<List<SplitShare>> {
        return dao.observeSharesByTransaction(transactionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeOwedPerPerson(): Flow<Map<String, Long>> {
        return dao.observeTotalsOwedPerPerson().map { list ->
            list.associate { it.personName to it.totalOwedPaise }
        }
    }

    override suspend fun markSettled(shareId: Long, settled: Boolean) {
        dao.markSettled(id = shareId, settled = settled)
    }

    override suspend fun deleteSharesForTransaction(transactionId: Long) {
        dao.deleteByTransaction(transactionId)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}
