package com.team.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.team.expensetracker.data.model.SplitShare

/**
 * Room persistence entity representing a person's share of a split expense.
 *
 * Foreign Key:
 * References [TransactionEntity.id]. When the parent transaction is deleted,
 * its associated split shares are cascade-deleted automatically.
 */
@Entity(
    tableName = "split_shares",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["personName"]),
        Index(value = ["settled"])
    ]
)
data class SplitShareEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val transactionId: Long,
    val personName: String,
    val sharePaise: Long,
    val settled: Boolean = false
)

/**
 * Converts this entity to the domain [SplitShare] model.
 */
fun SplitShareEntity.toDomain(): SplitShare = SplitShare(
    id = id,
    transactionId = transactionId,
    personName = personName,
    sharePaise = sharePaise,
    settled = settled
)

/**
 * Converts a domain [SplitShare] model to this entity.
 */
fun SplitShare.toEntity(): SplitShareEntity = SplitShareEntity(
    id = id,
    transactionId = transactionId,
    personName = personName.trim(),
    sharePaise = sharePaise,
    settled = settled
)
