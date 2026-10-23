package com.team.expensetracker.sms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [BankSenderFilter].
 *
 * These run on the JVM (no Android framework needed) so they are placed
 * under src/test, not src/androidTest.
 */
class BankSenderFilterTest {

    // -------------------------------------------------------------------------
    // Plain 6-letter bank header (no operator prefix)
    // -------------------------------------------------------------------------

    @Test
    fun `plain uppercase AXISBK is accepted`() {
        assertTrue(BankSenderFilter.isBankSender("AXISBK"))
    }

    @Test
    fun `plain uppercase HDFCBK is accepted`() {
        assertTrue(BankSenderFilter.isBankSender("HDFCBK"))
    }

    @Test
    fun `plain uppercase SBIINB is accepted`() {
        assertTrue(BankSenderFilter.isBankSender("SBIINB"))
    }

    @Test
    fun `plain uppercase ICICIB is accepted`() {
        assertTrue(BankSenderFilter.isBankSender("ICICIB"))
    }

    // -------------------------------------------------------------------------
    // Operator-prefixed format (e.g. "AX-AXISBK")
    // -------------------------------------------------------------------------

    @Test
    fun `operator-prefixed AX-AXISBK is accepted`() {
        assertTrue(BankSenderFilter.isBankSender("AX-AXISBK"))
    }

    @Test
    fun `operator-prefixed VM-HDFCBK is accepted`() {
        assertTrue(BankSenderFilter.isBankSender("VM-HDFCBK"))
    }

    @Test
    fun `operator-prefixed JD-ICICIB is accepted`() {
        assertTrue(BankSenderFilter.isBankSender("JD-ICICIB"))
    }

    @Test
    fun `operator-prefixed TF-SBIINB is accepted`() {
        assertTrue(BankSenderFilter.isBankSender("TF-SBIINB"))
    }

    // -------------------------------------------------------------------------
    // Case normalisation
    // -------------------------------------------------------------------------

    @Test
    fun `lowercase axisbk is accepted (case-insensitive)`() {
        assertTrue(BankSenderFilter.isBankSender("axisbk"))
    }

    @Test
    fun `mixed-case Ax-AxIsBk is accepted`() {
        assertTrue(BankSenderFilter.isBankSender("Ax-AxIsBk"))
    }

    @Test
    fun `lowercase vm-hdfcbk is accepted`() {
        assertTrue(BankSenderFilter.isBankSender("vm-hdfcbk"))
    }

    // -------------------------------------------------------------------------
    // Personal phone numbers — must be rejected
    // -------------------------------------------------------------------------

    @Test
    fun `E164 personal number +919876543210 is rejected`() {
        assertFalse(BankSenderFilter.isBankSender("+919876543210"))
    }

    @Test
    fun `10-digit mobile number 9876543210 is rejected`() {
        assertFalse(BankSenderFilter.isBankSender("9876543210"))
    }

    @Test
    fun `11-digit mobile number 09876543210 is rejected`() {
        assertFalse(BankSenderFilter.isBankSender("09876543210"))
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun `empty string is rejected`() {
        assertFalse(BankSenderFilter.isBankSender(""))
    }

    @Test
    fun `blank whitespace-only string is rejected`() {
        assertFalse(BankSenderFilter.isBankSender("   "))
    }

    @Test
    fun `unknown sender XYZABC is rejected`() {
        assertFalse(BankSenderFilter.isBankSender("XYZABC"))
    }

    @Test
    fun `operator-prefixed unknown VM-XYZABC is rejected`() {
        assertFalse(BankSenderFilter.isBankSender("VM-XYZABC"))
    }

    @Test
    fun `random word AMAZON is rejected`() {
        assertFalse(BankSenderFilter.isBankSender("AMAZON"))
    }
}
