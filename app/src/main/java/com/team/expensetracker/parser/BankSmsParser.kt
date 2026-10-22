package com.team.expensetracker.parser

import com.team.expensetracker.data.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure Kotlin implementation of [SmsParser] for Indian bank and UPI SMS notifications.
 *
 * Privacy Invariants (Non-Negotiable):
 * - Under NO circumstances is raw SMS body text stored, persisted, logged, or returned.
 * - OTP tokens and authentication secrets are never saved or returned in results.
 * - Zero floating-point types ([Float], [Double]) are used for money; all currency is stored as [Long] paise.
 * - OTP and non-transactional messages are rejected FIRST, before any amount extraction occurs.
 * - Zero Android framework dependencies (`android.*`), ensuring 100% JVM testability.
 */
class BankSmsParser : SmsParser {

    override fun parse(body: String, sender: String, receivedTimestamp: Long): ParseResult {
        // 1. Empty / Blank Check
        if (body.isBlank()) {
            return ParseResult.Rejected(RejectionReason.EMPTY_MESSAGE)
        }

        // 2. Pre-Parsing Rejection Pipeline (MUST RUN BEFORE AMOUNT EXTRACTION)
        val rejectionReason = evaluateRejection(body)
        if (rejectionReason != null) {
            return ParseResult.Rejected(rejectionReason)
        }

        // 3. Extraction: Financial Direction (Debit vs Credit)
        val direction = determineDirection(body)
            ?: return ParseResult.Rejected(RejectionReason.NOT_A_TRANSACTION)

        // 4. Extraction: Amount in Long Paise
        val amountPaise = extractAmountPaise(body)
            ?: return ParseResult.Rejected(RejectionReason.UNPARSEABLE_AMOUNT)

        // 5. Extraction: Masked Account / Card Digits
        val accountLastDigits = extractAccountLastDigits(body)

        // 6. Extraction: Payee / Counterparty & UPI VPA
        val upiId = extractUpiId(body)
        val merchant = extractMerchant(body, upiId)

        // 7. Extraction: Bank Reference / UTR Number
        val refNumber = extractRefNumber(body)

        // 8. Extraction: Bank Identifier / Sender Normalization
        val bank = resolveBank(body, sender)

        val candidate = ParsedTransaction(
            amountPaise = amountPaise,
            type = direction,
            merchant = merchant,
            upiId = upiId,
            refNumber = refNumber,
            date = receivedTimestamp,
            bank = bank,
            accountLastDigits = accountLastDigits
        )

        return ParseResult.Success(candidate)
    }

    /**
     * Evaluates whether an incoming SMS is an OTP, promotional offer, security alert,
     * balance-only enquiry, or payment request.
     *
     * Invariant: Evaluated prior to any amount or transaction extraction.
     */
    private fun evaluateRejection(body: String): RejectionReason? {
        // A. OTP / Secret Verification Code Patterns
        for (pattern in OTP_PATTERNS) {
            if (pattern.containsMatchIn(body)) {
                return RejectionReason.OTP
            }
        }

        // B. Security & Login Alerts
        for (pattern in SECURITY_ALERT_PATTERNS) {
            if (pattern.containsMatchIn(body)) {
                return RejectionReason.SECURITY_OR_LOGIN_ALERT
            }
        }

        // C. Promotional Offers, Loans, Discounts, Marketing
        for (pattern in PROMOTIONAL_PATTERNS) {
            if (pattern.containsMatchIn(body)) {
                return RejectionReason.PROMOTIONAL
            }
        }

        // D. Payment Request / Collect Mandates (Pending / Uncompleted)
        for (pattern in PAYMENT_REQUEST_PATTERNS) {
            if (pattern.containsMatchIn(body)) {
                return RejectionReason.PAYMENT_REQUEST
            }
        }

        // E. Failed / Declined Transactions
        for (pattern in FAILED_TRANSACTION_PATTERNS) {
            if (pattern.containsMatchIn(body)) {
                return RejectionReason.FAILED_TRANSACTION
            }
        }

        // F. Balance-Only Enquiries (Enquiry without a debit/credit event)
        if (isBalanceOnlyEnquiry(body)) {
            return RejectionReason.BALANCE_ENQUIRY
        }

        return null
    }

    /**
     * Identifies messages that merely state account balances without a debit/credit transaction.
     */
    private fun isBalanceOnlyEnquiry(body: String): Boolean {
        val hasBalanceKeyword = BALANCE_ENQUIRY_PATTERNS.any { it.containsMatchIn(body) }
        if (!hasBalanceKeyword) return false

        // If it also contains transaction action verbs, it's an actual transaction reporting residual balance
        val hasDebitVerb = DEBIT_KEYWORDS_REGEX.containsMatchIn(body)
        val hasCreditVerb = CREDIT_KEYWORDS_REGEX.containsMatchIn(body)

        return !hasDebitVerb && !hasCreditVerb
    }

    /**
     * Determines whether the transaction is [TransactionType.DEBIT] or [TransactionType.CREDIT].
     */
    private fun determineDirection(body: String): TransactionType? {
        val hasDebit = DEBIT_KEYWORDS_REGEX.containsMatchIn(body)
        val hasCredit = CREDIT_KEYWORDS_REGEX.containsMatchIn(body)

        if (hasCredit && !hasDebit) {
            return TransactionType.CREDIT
        }
        if (hasDebit && !hasCredit) {
            return TransactionType.DEBIT
        }
        if (hasCredit && hasDebit) {
            // Contextual resolution: e.g. "Refund of Rs X credited"
            if (REFUND_CREDITED_REGEX.containsMatchIn(body) ||
                CREDITED_ACTION_REGEX.containsMatchIn(body)
            ) {
                return TransactionType.CREDIT
            }
            if (DEBITED_ACTION_REGEX.containsMatchIn(body)) {
                return TransactionType.DEBIT
            }
            return TransactionType.DEBIT // Default to debit for transaction security if debit was explicitly cited
        }
        return null
    }

    /**
     * Extracts the transaction amount in paise (1 INR = 100 paise) as a [Long].
     *
     * Invariants:
     * - Never uses Float or Double.
     * - Discards balance and limit numbers so available balance is not confused with transaction amount.
     */
    private fun extractAmountPaise(body: String): Long? {
        // Strip out balance and available limit clauses to avoid false matches
        var strippedText = body
        for (pattern in BALANCE_CLAUSE_PATTERNS) {
            strippedText = pattern.replace(strippedText, " ")
        }

        // Strategy 1: Look for amount directly attached to action verb in original body
        for (pattern in ACTION_LINKED_AMOUNT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val rawAmount = match.groups[1]?.value
                if (rawAmount != null) {
                    val paise = parseAmountStringToPaise(rawAmount)
                    if (paise != null && paise > 0L) {
                        return paise
                    }
                }
            }
        }

        // Strategy 2: Look for amount in the balance-stripped text
        for (pattern in GENERIC_AMOUNT_PATTERNS) {
            val match = pattern.find(strippedText)
            if (match != null) {
                val rawAmount = match.groups[1]?.value
                if (rawAmount != null) {
                    val paise = parseAmountStringToPaise(rawAmount)
                    if (paise != null && paise > 0L) {
                        return paise
                    }
                }
            }
        }

        return null
    }

    /**
     * Safely converts currency string (supporting Indian grouping like `1,23,456.78` or integer `500`)
     * to Long paise using [BigDecimal] without floating-point math.
     */
    private fun parseAmountStringToPaise(rawAmount: String): Long? {
        val clean = rawAmount.replace(",", "").trim()
        if (clean.isBlank()) return null

        return try {
            val bd = BigDecimal(clean)
            // Normalize scale to 2 decimal places (paise)
            bd.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts masked account / card digits (e.g. "XX1234").
     */
    private fun extractAccountLastDigits(body: String): String? {
        for (pattern in ACCOUNT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val digits = match.groups[1]?.value
                if (!digits.isNullOrBlank()) {
                    return "XX$digits"
                }
            }
        }
        return null
    }

    /**
     * Extracts UPI Virtual Payment Address (VPA), e.g. "swiggy@icici".
     */
    private fun extractUpiId(body: String): String? {
        val match = UPI_VPA_REGEX.find(body)
        return match?.groups[1]?.value?.trim()
    }

    /**
     * Extracts merchant, payee, or counterparty name.
     */
    private fun extractMerchant(body: String, upiId: String?): String? {
        // 1. Check for ATM withdrawal
        if (ATM_WITHDRAWAL_REGEX.containsMatchIn(body)) {
            return "ATM"
        }

        // 2. Check explicitly patterned merchant clauses
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val name = match.groups[1]?.value?.trim()
                if (!name.isNullOrBlank() && isValidMerchantName(name)) {
                    return cleanMerchantName(name)
                }
            }
        }

        // 3. Fallback: If UPI VPA is available, derive merchant name from VPA username
        if (upiId != null && upiId.contains("@")) {
            val handle = upiId.substringBefore("@").trim()
            if (handle.isNotBlank()) {
                return handle
            }
        }

        return null
    }

    private fun isValidMerchantName(name: String): Boolean {
        val lower = name.lowercase().trim()
        val invalidTokens = listOf(
            "account", "a/c", "acct", "vpa", "upi", "inr", "rs", "your",
            "the", "balance", "bal", "ref", "dated", "txn"
        )
        return invalidTokens.none { lower == it || lower.startsWith("$it ") }
    }

    private fun cleanMerchantName(name: String): String {
        return name
            .replace(Regex("^[^A-Za-z0-9]+"), "")
            .replace(Regex("[^A-Za-z0-9]+$"), "")
            .trim()
    }

    /**
     * Extracts Bank Reference Number / UTR for deduplication.
     */
    private fun extractRefNumber(body: String): String? {
        for (pattern in REF_NUMBER_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val ref = match.groups[1]?.value?.trim()
                if (!ref.isNullOrBlank()) {
                    return ref
                }
            }
        }
        return null
    }

    /**
     * Resolves the bank name from SMS body or sender header.
     */
    private fun resolveBank(body: String, sender: String): String {
        // Check for specific bank names in body or sender header
        for ((bankName, pattern) in KNOWN_BANKS) {
            if (pattern.containsMatchIn(body) || pattern.containsMatchIn(sender)) {
                return bankName
            }
        }

        // Extract clean sender without telecom header (e.g. "VK-AXISBK" -> "AXISBK")
        val cleanSender = sender.substringAfter("-").trim()
        if (cleanSender.isNotBlank()) {
            return cleanSender
        }

        return if (sender.isNotBlank()) sender.trim() else "Unknown Bank"
    }

    companion object {
        private val OTP_PATTERNS = listOf(
            Regex("(?i)\\b(?:otp|one[- ]time[- ]password|verification[- ]code|security[- ]code|login[- ]code|secret[- ]code|auth[- ]code|authorization[- ]code)\\b"),
            Regex("(?i)\\bdo\\s+not\\s+share\\b"),
            Regex("(?i)\\bnever\\s+share\\b"),
            Regex("(?i)\\bnot\\s+to\\s+be\\s+shared\\b"),
            Regex("(?i)\\bvalid\\s+for\\s+\\d+\\s*(?:mins?|minutes?|secs?|seconds?)\\b"),
            Regex("(?i)\\bvalidity\\s*:\\s*\\d+\\s*(?:mins?|minutes?)\\b"),
            Regex("(?i)\\b(?:use|enter)\\s+(?:code|otp)\\s+\\d+\\b"),
            Regex("(?i)\\bis\\s+your\\s+(?:secret\\s+)?(?:otp|code|password)\\b"),
            Regex("(?i)\\bpin\\s+to\\s+approve\\b"),
            Regex("(?i)\\bsharing\\s+otp\\b")
        )

        private val SECURITY_ALERT_PATTERNS = listOf(
            Regex("(?i)\\b(?:logged\\s+in|login\\s+alert|sign[- ]in\\s+alert|new\\s+login|login\\s+attempt)\\b"),
            Regex("(?i)\\b(?:password\\s+changed|pin\\s+changed|profile\\s+updated|device\\s+registered|suspicious\\s+login)\\b"),
            Regex("(?i)\\bsecurity\\s+alert\\b")
        )

        private val PROMOTIONAL_PATTERNS = listOf(
            Regex("(?i)\\b(?:pre[- ]approved|apply\\s+for|congratulations|flat\\s+\\d+%\\s+off)\\b"),
            Regex("(?i)\\b(?:discount\\s+(?:coupon|code)|promo\\s+code|cashback\\s+offer)\\b"),
            Regex("(?i)\\b(?:click\\s+here\\s+to|loan\\s+offer|claim\\s+your\\s+reward|earn\\s+up\\s+to)\\b"),
            Regex("(?i)\\b(?:offer\\s+valid\\s+till|exclusive\\s+offer|limited\\s+period\\s+offer|win\\s+up\\s+to)\\b")
        )

        private val PAYMENT_REQUEST_PATTERNS = listOf(
            Regex("(?i)\\b(?:has\\s+requested|request(?:ed)?\\s+money|collect\\s+request)\\b"),
            Regex("(?i)\\b(?:mandate\\s+created|approve\\s+(?:the\\s+)?(?:collect\\s+)?request|pending\\s+(?:collect\\s+)?request|request\\s+to\\s+pay)\\b")
        )

        private val FAILED_TRANSACTION_PATTERNS = listOf(
            Regex("(?i)\\b(?:transaction\\s+failed|declined|failed\\s+due\\s+to|could\\s+not\\s+be\\s+processed|unsuccessful\\s+transaction)\\b"),
            Regex("(?i)\\b(?:txn\\s+(?:has\\s+)?failed|payment\\s+.*?failed|transaction\\s+.*?failed|payment\\s+.*?declined|transaction\\s+.*?declined)\\b")
        )

        private val BALANCE_ENQUIRY_PATTERNS = listOf(
            Regex("(?i)\\b(?:avail(?:able)?\\s+bal(?:ance)?|clear\\s+bal(?:ance)?|a/c\\s+bal(?:ance)?|account\\s+balance|current\\s+balance)\\b"),
            Regex("(?i)\\bbalance\\s+enquiry\\b")
        )

        private val DEBIT_KEYWORDS_REGEX =
            Regex("(?i)\\b(?:debited|debit|spent|paid|sent|withdrawn|withdrawal|purchase|transferred\\s+to|dr)\\b")

        private val CREDIT_KEYWORDS_REGEX =
            Regex("(?i)\\b(?:credited|credit|received|deposited|refund|refunded|reversal|reversed|cr)\\b")

        private val REFUND_CREDITED_REGEX =
            Regex("(?i)\\b(?:refund\\s+(?:of\\s+.*?)?credited|credited\\s+.*refund)\\b")

        private val CREDITED_ACTION_REGEX =
            Regex("(?i)\\b(?:credited\\s+to|credited\\s+with|has\\s+been\\s+credited|is\\s+credited|deposited\\s+to)\\b")

        private val DEBITED_ACTION_REGEX =
            Regex("(?i)\\b(?:debited\\s+from|debited\\s+by|debited\\s+with|has\\s+been\\s+debited|is\\s+debited|spent\\s+on|paid\\s+to|sent\\s+to|withdrawn\\s+from)\\b")

        private val BALANCE_CLAUSE_PATTERNS = listOf(
            Regex("(?i)(?:avail(?:able)?\\s+|clear\\s+|total\\s+)?bal(?:ance)?(?:\\s+is)?\\s*[:\\-\\s]?\\s*(?:rs\\.?|inr|₹)?\\s*[0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?"),
            Regex("(?i)(?:avail(?:able)?\\s+)?limit(?:\\s+is)?\\s*[:\\-\\s]?\\s*(?:rs\\.?|inr|₹)?\\s*[0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?")
        )

        private val ACTION_LINKED_AMOUNT_PATTERNS = listOf(
            Regex("(?i)(?:debited\\s+(?:by|with)?|credited\\s+(?:by|with)?|spent|paid|sent|withdrawn|deposited|refund\\s+of)\\s*(?:rs\\.?|inr|₹)?\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?)"),
            Regex("(?i)(?:rs\\.?|inr|₹)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?)\\s*(?:has\\s+been\\s+)?(?:debited|credited|spent|paid|sent|withdrawn|deposited|received|refunded)")
        )

        private val GENERIC_AMOUNT_PATTERNS = listOf(
            Regex("(?i)(?:rs\\.?|inr|₹)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?)")
        )

        private val ACCOUNT_PATTERNS = listOf(
            Regex("(?i)\\b(?:a/c|acct|account|card)\\s*(?:no\\.?)?\\s*(?:ending\\s*)?(?:with\\s*)?(?:x+|X+|\\*+)?([0-9]{3,4})\\b"),
            Regex("\\b(?:x+|X+|\\*+)+([0-9]{3,4})\\b")
        )

        private val UPI_VPA_REGEX =
            Regex("(?i)\\b([a-zA-Z0-9.\\-_]{2,}@[a-zA-Z0-9]{2,})\\b")

        private val ATM_WITHDRAWAL_REGEX =
            Regex("(?i)\\b(?:withdrawn\\s+from\\s+atm|atm\\s+(?:cash|withdrawal))\\b")

        private val MERCHANT_PATTERNS = listOf(
            Regex("(?i)\\b(?:to\\s+vpa|from\\s+vpa)\\s+([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)"),
            Regex("(?i)\\b(?:to|at|for)\\s+([A-Za-z0-9\\s&.\\-_]{2,30}?)(?=(?:\\s+(?:on\\b|ref\\b|rrn\\b|utr\\b|txn\\b|upi\\b|val\\b|bal\\b|avail\\b|via\\b|using\\b|dated\\b)|[\\.,]|\\s*$))"),
            Regex("(?i)\\bfrom\\s+([A-Za-z0-9\\s&.\\-_]{2,30}?)(?=(?:\\s+(?:on\\b|ref\\b|rrn\\b|utr\\b|txn\\b|upi\\b|val\\b|bal\\b|avail\\b|via\\b|using\\b|dated\\b)|[\\.,]|\\s*$))"),
            Regex("(?i)\\bUPI/(?:[0-9A-Za-z]+)/([A-Za-z0-9\\s&.\\-_]{2,30}?)(?:/|\\s|\\.|\\,|$)"),
            Regex("(?i)\\binfo:\\s*([A-Za-z0-9\\s&.\\-_]{2,30}?)(?=(?:\\s+(?:on\\b|ref\\b|rrn\\b|utr\\b|txn\\b|upi\\b|val\\b|bal\\b|avail\\b)|[\\.,]|\\s*$))")
        )

        private val REF_NUMBER_PATTERNS = listOf(
            Regex("(?i)\\b(?:upi\\s*ref(?:erence)?\\s*(?:no|num|number)?|ref(?:erence)?\\s*(?:no|num|number)?|rrn|utr\\s*(?:no|num|number)?|txn\\s*(?:id|no|num|number)?)\\s*[:\\-/]?\\s*([A-Za-z0-9]+)"),
            Regex("(?i)\\bUPI/([0-9]+)/"),
            Regex("(?i)\\bUPI:([0-9]+)")
        )

        private val KNOWN_BANKS = mapOf(
            "HDFC" to Regex("(?i)\\b(?:hdfc|hdfcbk)\\b"),
            "SBI" to Regex("(?i)\\b(?:sbi|sbinb|state\\s+bank)\\b"),
            "ICICI" to Regex("(?i)\\b(?:icici|icicib)\\b"),
            "AXIS" to Regex("(?i)\\b(?:axis|axisbk)\\b"),
            "KOTAK" to Regex("(?i)\\b(?:kotak|kotakb)\\b"),
            "PNB" to Regex("(?i)\\b(?:pnb|punjab\\s+national)\\b"),
            "BOB" to Regex("(?i)\\b(?:bob|bank\\s+of\\s+baroda)\\b")
        )
    }
}
