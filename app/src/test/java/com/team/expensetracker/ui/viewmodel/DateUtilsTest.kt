package com.team.expensetracker.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for [DateUtils].
 */
class DateUtilsTest {

    @Test
    fun testCurrentMonthStartIsFirstDayAtMidnight() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 23, 14, 30, 45)
        cal.set(Calendar.MILLISECOND, 500)

        val start = DateUtils.getCurrentMonthStart(cal)

        val resultCal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(2026, resultCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, resultCal.get(Calendar.MONTH))
        assertEquals(1, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertEquals(0, resultCal.get(Calendar.SECOND))
        assertEquals(0, resultCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun testCurrentMonthEndIsLastDayAtLastMillisecond() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 15, 10, 0, 0)

        val end = DateUtils.getCurrentMonthEnd(cal)

        val resultCal = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(2026, resultCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, resultCal.get(Calendar.MONTH))
        assertEquals(31, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, resultCal.get(Calendar.MINUTE))
        assertEquals(59, resultCal.get(Calendar.SECOND))
        assertEquals(999, resultCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun testMonthStartPrecedesMonthEnd() {
        val cal = Calendar.getInstance()
        val start = DateUtils.getCurrentMonthStart(cal)
        val end = DateUtils.getCurrentMonthEnd(cal)
        assertTrue(start < end)
    }
}
