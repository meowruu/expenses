package com.team.expensetracker.sms

/**
 * Result type returned by [SmsIngestor.ingest].
 *
 * Consumers should handle all three cases:
 *  - [Accepted]        – the SMS passed all checks and was forwarded for parsing.
 *  - [RejectedSender]  – the sender is not a known bank; message discarded.
 *  - [Ignored]         – the sender is a bank but the message was detected as an
 *                        OTP / non-financial message and was discarded without processing.
 *
 * Privacy note: none of these types carry the raw SMS body.
 */
sealed class SmsIngestionResult {
    /** SMS was accepted and forwarded to the processing pipeline. */
    object Accepted : SmsIngestionResult()

    /**
     * SMS was rejected because [sender] is not a recognised bank sender.
     * @param sender The original sender ID, for logging purposes only (not the body).
     */
    data class RejectedSender(val sender: String) : SmsIngestionResult()

    /**
     * SMS came from a known bank but was ignored (e.g. OTP, balance alert without a transaction).
     * The body is never stored in this result.
     */
    object Ignored : SmsIngestionResult()
}

/**
 * Entry-point interface for the SMS ingestion pipeline.
 *
 * Implementations must:
 *  1. Check the sender against [BankSenderFilter] first.
 *  2. Pass accepted messages to the parser (B1) in-memory only.
 *  3. NEVER persist, log, or leak the raw [body] string.
 */
interface SmsIngestor {

    /**
     * Attempts to ingest an incoming SMS.
     *
     * @param sender The SMS sender ID (e.g. "AXISBK" or "AX-AXISBK").
     * @param body   The raw SMS body text. Passed in-memory only; NEVER stored or logged.
     * @return       A [SmsIngestionResult] indicating the outcome.
     */
    fun ingest(sender: String, body: String): SmsIngestionResult
}
