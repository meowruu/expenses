package com.team.expensetracker.data.repository

import com.team.expensetracker.data.local.dao.MyUpiIdDao
import com.team.expensetracker.data.local.entity.MyUpiIdEntity
import com.team.expensetracker.data.local.entity.toDomain
import com.team.expensetracker.data.model.MyUpiId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Production implementation of [MyUpiIdRepository] backed by Room database.
 */
class MyUpiIdRepositoryImpl(
    private val dao: MyUpiIdDao
) : MyUpiIdRepository {

    override fun observeAll(): Flow<List<MyUpiId>> {
        return dao.observeAll().map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override suspend fun add(id: String) {
        val trimmed = id.trim()
        if (trimmed.isNotEmpty()) {
            dao.add(MyUpiIdEntity(upiId = trimmed))
        }
    }

    override suspend fun remove(id: String) {
        val trimmed = id.trim()
        if (trimmed.isNotEmpty()) {
            dao.remove(trimmed)
        }
    }

    override suspend fun isMine(id: String): Boolean {
        val trimmed = id.trim()
        if (trimmed.isEmpty()) return false
        return dao.isMine(trimmed)
    }
}
