package com.team.expensetracker.parser

/**
 * Pure Kotlin parser interface for evaluating and extracting transactional financial records from SMS messages.
 *
 * Architecture & Platform Independence:
 * - Free of Android framework dependencies (`android.*`), enabling fast JVM unit testing.
 * - Enforces zero raw SMS persistence across all implementations.
 */
interface SmsParser {

    /**
     * Parses an incoming SMS message.
     *
     * @param body The text content of the SMS.
     * @param sender The sender identifier or address (e.g. "AXISBK", "VK-HDFCBK").
     * @param receivedTimestamp Epoch millisecond timestamp when the SMS was received on-device.
     * @return [ParseResult.Success] containing structured transaction data, or [ParseResult.Rejected] with the rejection reason.
     */
    fun parse(body: String, sender: String, receivedTimestamp: Long): ParseResult
}
