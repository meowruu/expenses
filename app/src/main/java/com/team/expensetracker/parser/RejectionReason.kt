package com.team.expensetracker.parser

/**
 * Enumerates the non-transactional or invalid reasons for which an incoming SMS
 * is rejected by the parsing engine prior to or during extraction.
 *
 * Privacy Invariant:
 * Reason identifiers represent categorized failure modes only.
 * Under NO circumstances does this enum or its usages store or expose the raw SMS text,
 * any extracted OTP numbers, or user secrets.
 */
enum class RejectionReason {
    /** The message is null, empty, or consists solely of whitespace. */
    EMPTY_MESSAGE,

    /**
     * Message contains One-Time Password (OTP) patterns, verification codes, or PIN security warnings.
     * Evaluated first before any other extraction.
     */
    OTP,

    /** Message is an authentication, login, or account security alert (e.g. password changed, new device). */
    SECURITY_OR_LOGIN_ALERT,

    /** Message is a marketing, offer, loan, cashback, or promotional advertisement. */
    PROMOTIONAL,

    /** Message is an unfulfilled payment request or collect mandate that has not completed a debit/credit. */
    PAYMENT_REQUEST,

    /** Message indicates a failed, declined, or unsuccessful transaction. */
    FAILED_TRANSACTION,

    /** Message is an account balance enquiry without a financial debit or credit movement. */
    BALANCE_ENQUIRY,

    /** Message does not represent a financial transaction. */
    NOT_A_TRANSACTION,

    /** Transaction amount could not be reliably extracted from the message text. */
    UNPARSEABLE_AMOUNT,

    /** Transaction financial direction (DEBIT vs CREDIT) could not be conclusively determined. */
    UNKNOWN_DIRECTION
}
