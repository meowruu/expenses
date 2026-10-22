package com.team.expensetracker.ui.util

import com.team.expensetracker.data.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats amount in paise into Indian Rupee display (e.g. ₹1,450.00).
 */
fun formatRupees(amountPaise: Long): String {
    val rupees = amountPaise / 100.0
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    return format.format(rupees)
}

/**
 * Formats transaction amount into rupee display.
 */
fun Transaction.formattedAmount(): String = formatRupees(amountPaise)

/**
 * Formats date into readable string (e.g. "22 Oct, 04:30 PM").
 */
fun Transaction.formattedDateTime(): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(date))
}

/**
 * Formats date into date-only string (e.g. "22 Oct 2026").
 */
fun Transaction.formattedDate(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(date))
}
