package com.team.expensetracker.parser

/**
 * Result of attempting to parse an incoming SMS message.
 *
 * Privacy Invariants:
 * - Sealed interface guaranteeing exhaustive pattern matching.
 * - Under NO circumstances does [Success] or [Rejected] contain, wrap, or log the raw SMS body.
 */
sealed interface ParseResult {

    /**
     * Parsing succeeded and produced a validated [ParsedTransaction] candidate.
     *
     * @property transaction Structured financial fields extracted from the message.
     */
    data class Success(val transaction: ParsedTransaction) : ParseResult

    /**
     * Message was rejected prior to or during parsing.
     *
     * @property reason The categorized cause for rejection (e.g. [RejectionReason.OTP], [RejectionReason.PROMOTIONAL]).
     */
    data class Rejected(val reason: RejectionReason) : ParseResult
}
