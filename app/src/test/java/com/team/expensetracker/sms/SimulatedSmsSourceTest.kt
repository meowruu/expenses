package com.team.expensetracker.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [SimulatedSmsSource].
 *
 * Verifies that the simulate() function routes messages through the exact
 * same pipeline as the real BroadcastReceiver would:
 *  1. BankSenderFilter check.
 *  2. Handler (StubSmsHandler) invocation.
 *
 * Privacy note: only fake/masked bodies are used in these tests.
 * No real SMS bodies are referenced anywhere in this file.
 */
class SimulatedSmsSourceTest {

    private val source = SimulatedSmsSource(StubSmsHandler())

    @Test
    fun `SmsPipeline default ingestor is not null`() {
        assertNotNull(SmsPipeline.ingestor)
        assertTrue(SmsPipeline.ingestor is StubSmsHandler)
    }

    @Test
    fun `swapping in fake SmsIngestor makes SimulatedSmsSource call it`() {
        val original = SmsPipeline.ingestor
        var fakeCalledWithSender: String? = null
        var fakeCalledWithBody: String? = null

        val fakeIngestor = object : SmsIngestor {
            override fun ingest(sender: String, body: String): SmsIngestionResult {
                fakeCalledWithSender = sender
                fakeCalledWithBody = body
                return SmsIngestionResult.Accepted
            }
        }

        try {
            SmsPipeline.ingestor = fakeIngestor
            val pipelineSource = SimulatedSmsSource() // uses SmsPipeline.ingestor by default
            val result = pipelineSource.simulate("AX-AXISBK", "Dummy body")

            assertEquals(SmsIngestionResult.Accepted, result)
            assertEquals("AX-AXISBK", fakeCalledWithSender)
            assertEquals("Dummy body", fakeCalledWithBody)
        } finally {
            SmsPipeline.ingestor = original
        }
    }

    @Test
    fun `non-bank sender is rejected before reaching handler`() {
        val result = source.simulate(
            sender = "+919876543210",
            body = "Some message body"
        )
        assertTrue(
            "Expected RejectedSender but got $result",
            result is SmsIngestionResult.RejectedSender
        )
        assertEquals("+919876543210", (result as SmsIngestionResult.RejectedSender).sender)
    }

    @Test
    fun `bank sender with debit body is accepted`() {
        val result = source.simulate(
            sender = SimulatedSmsSource.SampleMessages.SENDER_AXIS,
            body = SimulatedSmsSource.SampleMessages.BODY_DEBIT
        )
        assertEquals(
            "Expected Accepted for valid bank debit SMS",
            SmsIngestionResult.Accepted,
            result
        )
    }

    @Test
    fun `bank sender with OTP body is ignored`() {
        val result = source.simulate(
            sender = SimulatedSmsSource.SampleMessages.SENDER_OTP,
            body = SimulatedSmsSource.SampleMessages.BODY_OTP
        )
        assertEquals(
            "Expected Ignored for OTP message from bank sender",
            SmsIngestionResult.Ignored,
            result
        )
    }

    @Test
    fun `bank sender with credit body is accepted`() {
        val result = source.simulate(
            sender = SimulatedSmsSource.SampleMessages.SENDER_HDFC,
            body = SimulatedSmsSource.SampleMessages.BODY_CREDIT
        )
        assertEquals(
            "Expected Accepted for valid bank credit SMS",
            SmsIngestionResult.Accepted,
            result
        )
    }

    @Test
    fun `unknown non-bank alpha sender is rejected`() {
        val result = source.simulate(
            sender = "AMAZON",
            body = "Your Amazon order has been placed"
        )
        assertTrue(result is SmsIngestionResult.RejectedSender)
    }
}
