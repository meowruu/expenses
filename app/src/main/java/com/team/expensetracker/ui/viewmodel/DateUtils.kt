package com.team.expensetracker.ui.viewmodel

import java.util.Calendar

/**
 * Utility functions for month date window calculations.
 */
object DateUtils {

    /**
     * Calculates the epoch millisecond start timestamp (00:00:00.000) of the current month.
     */
    fun getCurrentMonthStart(calendar: Calendar = Calendar.getInstance()): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Calculates the epoch millisecond end timestamp (23:59:59.999) of the current month.
     */
    fun getCurrentMonthEnd(calendar: Calendar = Calendar.getInstance()): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
