package com.team.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.team.expensetracker.data.model.PayeeRule

/**
 * Room persistence entity for custom payee categorization rules.
 *
 * @property upiId The UPI identifier (primary key, e.g. "swiggy@icici").
 * @property category The user-assigned expense category (e.g. "Food").
 */
@Entity(tableName = "payee_rules")
data class PayeeRuleEntity(
    @PrimaryKey
    val upiId: String,
    val category: String
)

/**
 * Maps this entity to the shared domain [PayeeRule] model.
 */
fun PayeeRuleEntity.toDomain(): PayeeRule {
    return PayeeRule(
        upiId = upiId,
        category = category
    )
}

/**
 * Maps the shared domain [PayeeRule] model to the database entity.
 */
fun PayeeRule.toEntity(): PayeeRuleEntity {
    return PayeeRuleEntity(
        upiId = upiId.trim().lowercase(),
        category = category.trim()
    )
}
