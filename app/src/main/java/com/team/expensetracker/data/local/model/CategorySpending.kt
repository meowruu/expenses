package com.team.expensetracker.data.local.model

import androidx.room.ColumnInfo

/**
 * POJO for category-wise aggregate spending query results.
 *
 * @property category The expense category name.
 * @property totalPaise The total spending in paise for this category.
 */
data class CategorySpending(
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "totalPaise")
    val totalPaise: Long
)
