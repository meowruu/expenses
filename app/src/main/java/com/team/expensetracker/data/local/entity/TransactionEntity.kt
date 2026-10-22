package com.team.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType

/**
 * Room persistence entity for financial transactions.
 *
 * Privacy Invariant:
 * Strictly stores only parsed financial fields.
 * NEVER stores raw SMS body, OTP strings, or full message content.
 *
 * Deduplication Invariant:
 * A unique database index is enforced on [refNumber]. Nullable reference numbers
 * are supported (multiple NULLs are permitted in SQLite unique indexes, while
 * non-null reference numbers are guaranteed unique).
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["refNumber"], unique = true),
        Index(value = ["date"]),
        Index(value = ["type", "date"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amountPaise: Long,
    val type: TransactionType,
    val merchant: String?,
    val upiId: String?,
    val refNumber: String?,
    val date: Long,
    val bank: String,
    val category: String
)

/**
 * Maps this database entity to the shared domain [Transaction] model.
 */
fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        amountPaise = amountPaise,
        type = type,
        merchant = merchant,
        upiId = upiId,
        refNumber = refNumber,
        date = date,
        bank = bank,
        category = category
    )
}

/**
 * Maps the shared domain [Transaction] model to the database entity.
 * Reference numbers are normalized: blank or whitespace-only strings are converted to null
 * so SQLite's UNIQUE index does not collide on empty strings.
 */
fun Transaction.toEntity(): TransactionEntity {
    val normalizedRef = if (refNumber.isNullOrBlank()) null else refNumber.trim()
    val normalizedUpi = if (upiId.isNullOrBlank()) null else upiId.trim().lowercase()
    val normalizedMerchant = if (merchant.isNullOrBlank()) null else merchant.trim()
    return TransactionEntity(
        id = id,
        amountPaise = amountPaise,
        type = type,
        merchant = normalizedMerchant,
        upiId = normalizedUpi,
        refNumber = normalizedRef,
        date = date,
        bank = bank.trim(),
        category = category.trim()
    )
}
