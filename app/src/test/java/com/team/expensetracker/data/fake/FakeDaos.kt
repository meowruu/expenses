package com.team.expensetracker.data.fake

import com.team.expensetracker.data.local.dao.MyUpiIdDao
import com.team.expensetracker.data.local.dao.PayeeRuleDao
import com.team.expensetracker.data.local.dao.TransactionDao
import com.team.expensetracker.data.local.entity.MyUpiIdEntity
import com.team.expensetracker.data.local.entity.PayeeRuleEntity
import com.team.expensetracker.data.local.entity.TransactionEntity
import com.team.expensetracker.data.local.model.CategorySpending
import com.team.expensetracker.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory test double for [MyUpiIdDao].
 */
class FakeMyUpiIdDao : MyUpiIdDao {
    val items = MutableStateFlow<Map<String, MyUpiIdEntity>>(emptyMap())

    override fun observeAll(): Flow<List<MyUpiIdEntity>> {
        return items.map { map -> map.values.sortedBy { it.upiId } }
    }

    override suspend fun add(myUpiId: MyUpiIdEntity): Long {
        val key = myUpiId.upiId.trim().lowercase()
        val current = items.value.toMutableMap()
        if (current.containsKey(key)) {
            return -1L
        }
        current[key] = myUpiId.copy(upiId = key)
        items.value = current
        return 1L
    }

    override suspend fun remove(id: String): Int {
        val key = id.trim().lowercase()
        val current = items.value.toMutableMap()
        val removed = current.remove(key)
        items.value = current
        return if (removed != null) 1 else 0
    }

    override suspend fun isMine(id: String): Boolean {
        val key = id.trim().lowercase()
        return items.value.containsKey(key)
    }

    override suspend fun deleteAll(): Int {
        val count = items.value.size
        items.value = emptyMap()
        return count
    }
}

/**
 * In-memory test double for [PayeeRuleDao].
 */
class FakePayeeRuleDao : PayeeRuleDao {
    val items = MutableStateFlow<Map<String, PayeeRuleEntity>>(emptyMap())

    override suspend fun getCategoryFor(upiId: String): String? {
        val key = upiId.trim().lowercase()
        return items.value[key]?.category
    }

    override suspend fun saveRule(rule: PayeeRuleEntity): Long {
        val key = rule.upiId.trim().lowercase()
        val current = items.value.toMutableMap()
        current[key] = rule.copy(upiId = key)
        items.value = current
        return 1L
    }

    override fun observeAll(): Flow<List<PayeeRuleEntity>> {
        return items.map { it.values.sortedBy { rule -> rule.upiId } }
    }

    override suspend fun delete(upiId: String): Int {
        val key = upiId.trim().lowercase()
        val current = items.value.toMutableMap()
        val removed = current.remove(key)
        items.value = current
        return if (removed != null) 1 else 0
    }

    override suspend fun deleteAll(): Int {
        val count = items.value.size
        items.value = emptyMap()
        return count
    }
}

/**
 * In-memory test double for [TransactionDao] that accurately mimics Room SQLite behavior.
 */
class FakeTransactionDao(
    private val myUpiIdDao: FakeMyUpiIdDao = FakeMyUpiIdDao()
) : TransactionDao {

    private val idCounter = AtomicLong(1L)
    val transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())

    override suspend fun insert(transaction: TransactionEntity): Long {
        val current = transactions.value.toMutableList()

        // Check SQLite unique constraint on non-null, non-blank refNumber
        if (!transaction.refNumber.isNullOrBlank()) {
            if (current.any { it.refNumber == transaction.refNumber }) {
                return -1L
            }
        }

        val assignedId = if (transaction.id == 0L) idCounter.getAndIncrement() else transaction.id
        val newEntity = transaction.copy(id = assignedId)
        current.add(newEntity)
        transactions.value = current
        return assignedId
    }

    override suspend fun existsByRefNumber(refNumber: String): Boolean {
        if (refNumber.isBlank()) return false
        return transactions.value.any { it.refNumber == refNumber }
    }

    override fun observeAll(): Flow<List<TransactionEntity>> {
        return transactions.map { list -> list.sortedByDescending { it.date } }
    }

    override suspend fun getById(id: Long): TransactionEntity? {
        return transactions.value.find { it.id == id }
    }

    override fun observeMonthTotal(monthStart: Long, monthEnd: Long): Flow<Long> {
        return combine(transactions, myUpiIdDao.items) { txList, ownUpiMap ->
            val ownUpiKeys = ownUpiMap.keys.map { it.trim().lowercase() }.toSet()
            txList.filter { tx ->
                tx.type == TransactionType.DEBIT &&
                        tx.date in monthStart..monthEnd &&
                        (tx.upiId == null || tx.upiId.trim().isEmpty() || !ownUpiKeys.contains(tx.upiId.trim().lowercase()))
            }.sumOf { it.amountPaise }
        }
    }

    override fun observeCategorySummary(monthStart: Long, monthEnd: Long): Flow<List<CategorySpending>> {
        return combine(transactions, myUpiIdDao.items) { txList, ownUpiMap ->
            val ownUpiKeys = ownUpiMap.keys.map { it.trim().lowercase() }.toSet()
            txList.filter { tx ->
                tx.type == TransactionType.DEBIT &&
                        tx.date in monthStart..monthEnd &&
                        (tx.upiId == null || tx.upiId.trim().isEmpty() || !ownUpiKeys.contains(tx.upiId.trim().lowercase()))
            }.groupBy { it.category }
                .map { (cat, list) -> CategorySpending(category = cat, totalPaise = list.sumOf { it.amountPaise }) }
                .sortedByDescending { it.totalPaise }
        }
    }

    override suspend fun updateCategory(id: Long, category: String): Int {
        val current = transactions.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(category = category)
            transactions.value = current
            return 1
        }
        return 0
    }

    override suspend fun deleteAll(): Int {
        val count = transactions.value.size
        transactions.value = emptyList()
        return count
    }
}

/**
 * In-memory test double for [com.team.expensetracker.data.local.dao.SplitShareDao].
 */
class FakeSplitShareDao : com.team.expensetracker.data.local.dao.SplitShareDao {
    private val idCounter = AtomicLong(1L)
    val shares = MutableStateFlow<List<com.team.expensetracker.data.local.entity.SplitShareEntity>>(emptyList())

    override suspend fun insertShares(shares: List<com.team.expensetracker.data.local.entity.SplitShareEntity>): List<Long> {
        val current = this.shares.value.toMutableList()
        val ids = mutableListOf<Long>()
        for (share in shares) {
            val assignedId = if (share.id == 0L) idCounter.getAndIncrement() else share.id
            val newEntity = share.copy(id = assignedId)
            current.add(newEntity)
            ids.add(assignedId)
        }
        this.shares.value = current
        return ids
    }

    override suspend fun insertShare(share: com.team.expensetracker.data.local.entity.SplitShareEntity): Long {
        return insertShares(listOf(share)).first()
    }

    override fun observeSharesByTransaction(transactionId: Long): Flow<List<com.team.expensetracker.data.local.entity.SplitShareEntity>> {
        return shares.map { list -> list.filter { it.transactionId == transactionId }.sortedBy { it.id } }
    }

    override suspend fun getSharesByTransaction(transactionId: Long): List<com.team.expensetracker.data.local.entity.SplitShareEntity> {
        return shares.value.filter { it.transactionId == transactionId }.sortedBy { it.id }
    }

    override fun observeTotalsOwedPerPerson(): Flow<List<com.team.expensetracker.data.local.model.PersonOwedShare>> {
        return shares.map { list ->
            list.filter { !it.settled }
                .groupBy { it.personName }
                .map { (name, personShares) ->
                    com.team.expensetracker.data.local.model.PersonOwedShare(
                        personName = name,
                        totalOwedPaise = personShares.sumOf { it.sharePaise }
                    )
                }
                .sortedByDescending { it.totalOwedPaise }
        }
    }

    override suspend fun markSettled(id: Long, settled: Boolean): Int {
        val current = shares.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(settled = settled)
            shares.value = current
            return 1
        }
        return 0
    }

    override suspend fun deleteByTransaction(transactionId: Long): Int {
        val current = shares.value.toMutableList()
        val before = current.size
        current.removeAll { it.transactionId == transactionId }
        val count = before - current.size
        shares.value = current
        return count
    }

    override suspend fun deleteAll(): Int {
        val count = shares.value.size
        shares.value = emptyList()
        return count
    }
}
