package com.team.expensetracker.data.repository

import com.team.expensetracker.data.local.dao.PayeeRuleDao
import com.team.expensetracker.data.local.entity.PayeeRuleEntity
import com.team.expensetracker.data.local.entity.toDomain
import com.team.expensetracker.data.model.PayeeRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Production implementation of [PayeeRuleRepository] backed by Room database.
 */
class PayeeRuleRepositoryImpl(
    private val dao: PayeeRuleDao
) : PayeeRuleRepository {

    override suspend fun getCategoryFor(upiId: String): String? {
        val trimmed = upiId.trim()
        if (trimmed.isEmpty()) return null
        return dao.getCategoryFor(trimmed)
    }

    override suspend fun saveRule(upiId: String, category: String) {
        val trimmedUpi = upiId.trim()
        val trimmedCategory = category.trim()
        if (trimmedUpi.isNotEmpty() && trimmedCategory.isNotEmpty()) {
            dao.saveRule(PayeeRuleEntity(upiId = trimmedUpi, category = trimmedCategory))
        }
    }

    /**
     * Observes all user-defined payee rules.
     */
    fun observeAll(): Flow<List<PayeeRule>> {
        return dao.observeAll().map { entityList ->
            entityList.map { it.toDomain() }
        }
    }
}
