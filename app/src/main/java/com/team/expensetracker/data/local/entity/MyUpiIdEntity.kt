package com.team.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.team.expensetracker.data.model.MyUpiId

/**
 * Room persistence entity for user-owned UPI identifiers.
 *
 * Used to identify self-transfers so they can be excluded from spending totals.
 *
 * @property upiId The user's UPI identifier (primary key, e.g. "user@okhdfcbank").
 */
@Entity(tableName = "my_upi_ids")
data class MyUpiIdEntity(
    @PrimaryKey
    val upiId: String
)

/**
 * Maps this entity to the shared domain [MyUpiId] model.
 */
fun MyUpiIdEntity.toDomain(): MyUpiId {
    return MyUpiId(
        upiId = upiId
    )
}

/**
 * Maps the shared domain [MyUpiId] model to the database entity.
 */
fun MyUpiId.toEntity(): MyUpiIdEntity {
    return MyUpiIdEntity(
        upiId = upiId.trim().lowercase()
    )
}
