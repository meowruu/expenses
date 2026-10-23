package com.team.expensetracker.data.model

/**
 * Domain model representing a single person's share of a split expense.
 *
 * @property id Unique identifier for the split share (0L for unsaved).
 * @property transactionId The ID of the parent [Transaction] that was split.
 * @property personName The display name of the person responsible for this share.
 * @property sharePaise The owed share amount in paise (1 INR = 100 paise). Never Float/Double.
 * @property settled True if this person has settled/repaid their share; false if still owed.
 */
data class SplitShare(
    val id: Long = 0L,
    val transactionId: Long,
    val personName: String,
    val sharePaise: Long,
    val settled: Boolean = false
)
