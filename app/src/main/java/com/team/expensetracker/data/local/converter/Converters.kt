package com.team.expensetracker.data.local.converter

import androidx.room.TypeConverter
import com.team.expensetracker.data.model.TransactionType

/**
 * Room type converters for database persistence.
 */
class Converters {

    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String {
        return type?.name ?: TransactionType.DEBIT.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType {
        return value?.let {
            runCatching { TransactionType.valueOf(it) }.getOrDefault(TransactionType.DEBIT)
        } ?: TransactionType.DEBIT
    }
}
