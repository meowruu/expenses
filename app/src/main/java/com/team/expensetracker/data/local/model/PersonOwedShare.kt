package com.team.expensetracker.data.local.model

/**
 * Projection POJO for aggregating unsettled split shares grouped by person.
 *
 * @property personName The name of the debtor.
 * @property totalOwedPaise The sum of unsettled paise owed by this person.
 */
data class PersonOwedShare(
    val personName: String,
    val totalOwedPaise: Long
)
