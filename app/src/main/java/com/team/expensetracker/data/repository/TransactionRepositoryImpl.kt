package com.team.expensetracker.data.repository

import com.team.expensetracker.data.local.dao.MyUpiIdDao
import com.team.expensetracker.data.local.dao.PayeeRuleDao
import com.team.expensetracker.data.local.dao.TransactionDao
import com.team.expensetracker.data.local.entity.toDomain
import com.team.expensetracker.data.local.entity.toEntity
import com.team.expensetracker.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Production implementation of [TransactionRepository] backed by Room database.
 *
 * Privacy Invariant:
 * Operates purely on structured [Transaction] entities. Zero raw SMS content is handled or persisted.
 *
 * Deduplication Invariant:
 * Guarantees duplicate reference numbers cannot be inserted. If a transaction with the same non-blank
 * reference number already exists, insertion is rejected and returns false.
 */
class TransactionRepositoryImpl(
    private val dao: TransactionDao,
    private val payeeRuleDao: PayeeRuleDao? = null,
    private val myUpiIdDao: MyUpiIdDao? = null
) : TransactionRepository {

    override suspend fun insert(tx: Transaction): Boolean {
        val normalizedRef = if (tx.refNumber.isNullOrBlank()) null else tx.refNumber.trim()
        val normalizedUpi = if (tx.upiId.isNullOrBlank()) null else tx.upiId.trim().lowercase()

        // Explicit pre-check for duplicate reference numbers
        if (normalizedRef != null && dao.existsByRefNumber(normalizedRef)) {
            return false
        }

        val entity = tx.toEntity().copy(refNumber = normalizedRef, upiId = normalizedUpi)
        val rowId = dao.insert(entity)

        // Room returns -1 if insert was ignored due to SQLite unique constraint conflict
        return rowId > 0L
    }

    override fun observeAll(): Flow<List<Transaction>> {
        return dao.observeAll().map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override fun observeMonthTotal(monthStart: Long, monthEnd: Long): Flow<Long> {
        return dao.observeMonthTotal(monthStart = monthStart, monthEnd = monthEnd)
    }

    override fun observeCategorySummary(monthStart: Long, monthEnd: Long): Flow<Map<String, Long>> {
        return dao.observeCategorySummary(monthStart = monthStart, monthEnd = monthEnd).map { categoryList ->
            categoryList.associate { it.category to it.totalPaise }
        }
    }

    override suspend fun updateCategory(id: Long, category: String) {
        dao.updateCategory(id = id, category = category.trim())
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
        payeeRuleDao?.deleteAll()
        myUpiIdDao?.deleteAll()
    }
}
