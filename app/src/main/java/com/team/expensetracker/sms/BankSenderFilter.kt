package com.team.expensetracker.sms

/**
 * Decides whether an incoming SMS sender ID belongs to a known Indian bank.
 *
 * Accepted formats:
 *  - Plain 6-letter bank header:   "AXISBK", "HDFCBK", "SBIINB"
 *  - Operator-prefixed header:     "AX-AXISBK", "VM-HDFCBK", "JD-ICICIB"
 *    (two-letter operator code + hyphen + 6-letter header)
 *
 * Rejected:
 *  - Personal phone numbers (+91XXXXXXXXXX, 10-digit, etc.)
 *  - Any sender not in the allowlist.
 *
 * To extend: add the 6-letter header (upper-case) to [BANK_SENDER_IDS].
 */
object BankSenderFilter {

    /**
     * Allowlist of known Indian bank 6-letter SMS sender headers (upper-case).
     * Extend this set as more banks are onboarded.
     */
    val BANK_SENDER_IDS: Set<String> = setOf(
        "AXISBK",  // Axis Bank
        "HDFCBK",  // HDFC Bank
        "SBIINB",  // State Bank of India
        "ICICIB",  // ICICI Bank
        "KOTAKB",  // Kotak Mahindra Bank
        "PNBSMS",  // Punjab National Bank
        "BOIIND",  // Bank of India
        "CANBNK",  // Canara Bank
        "CENTBK",  // Central Bank of India
        "INDUSB",  // IndusInd Bank
        "YESBNK",  // Yes Bank
        "IDBIBS",  // IDBI Bank
        "PAYTMB",  // Paytm Payments Bank
        "FINOBL",  // Fino Payments Bank
        "SCBNKB",  // Standard Chartered Bank
        "CSBBNK",  // Catholic Syrian Bank
        "FEDBKM",  // Federal Bank
        "AUSFIN",  // AU Small Finance Bank
    )

    /**
     * Returns true if [senderId] is a recognised bank SMS sender.
     *
     * Normalisation applied before matching:
     *  1. Trim whitespace and convert to upper-case.
     *  2. If the result matches "XX-XXXXXX" (operator-prefixed), strip the first 3 characters.
     *  3. Look up the remaining string in [BANK_SENDER_IDS].
     *
     * Personal phone numbers (containing only digits or starting with '+') are always rejected
     * without a allowlist lookup.
     */
    fun isBankSender(senderId: String): Boolean {
        if (senderId.isBlank()) return false

        val trimmed = senderId.trim()

        // Reject personal phone numbers: pure digits or starts with '+'
        if (trimmed.startsWith("+") || trimmed.all { it.isDigit() }) return false
        // Reject if it looks like a short-code number (e.g. "54321")
        if (trimmed.length <= 7 && trimmed.all { it.isDigit() }) return false

        val upper = trimmed.uppercase()

        // Strip "XX-" operator prefix if present (e.g. "AX-AXISBK" → "AXISBK")
        val header = if (upper.length > 3 && upper[2] == '-') {
            upper.substring(3)
        } else {
            upper
        }

        return header in BANK_SENDER_IDS
    }
}
