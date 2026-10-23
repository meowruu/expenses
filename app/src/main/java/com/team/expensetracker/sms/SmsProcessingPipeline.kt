package com.team.expensetracker.sms

import com.team.expensetracker.data.model.PayeeRule
import com.team.expensetracker.data.repository.PayeeRuleRepository
import com.team.expensetracker.data.repository.TransactionRepository
import com.team.expensetracker.parser.BankSmsParser
import com.team.expensetracker.parser.DefaultTransactionCategorizer
import com.team.expensetracker.parser.ParseResult
import com.team.expensetracker.parser.SmsParser
import com.team.expensetracker.parser.TransactionCategorizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * End-to-end processing pipeline for incoming financial SMS notifications.
 *
 * Architecture & Privacy Guarantees:
 *  1. [BankSenderFilter.isBankSender] - Discards messages from non-bank senders immediately.
 *  2. [BankSmsParser.parse] - Evaluates rejection rules; OTPs, balance queries, etc. are rejected
 *     FIRST before any amount extraction occurs.
 *  3. [PayeeRuleRepository.getCategoryFor] - Queries user-defined categorisation overrides.
 *  4. [TransactionCategorizer.categorize] - Assigns category from canonical taxonomy using heuristics.
 *  5. [TransactionRepository.insert] - Persists parsed transaction with deduplication on refNumber.
 *     Money is strictly represented as [Long] paise (zero Float/Double).
 *
 * Privacy Rule Compliance:
 *  - Raw SMS body text is processed strictly in-memory and NEVER stored or logged.
 *  - OTP messages are detected and rejected immediately; OTP values are never retained.
 */
class SmsProcessingPipeline(
    private val transactionRepository: TransactionRepository,
    private val payeeRuleRepository: PayeeRuleRepository,
    private val parser: SmsParser = BankSmsParser(),
    private val categorizer: TransactionCategorizer = DefaultTransactionCategorizer()
) : SmsIngestor {

    /**
     * Processes an incoming SMS text and persists it if it is a valid bank transaction.
     *
     * @param body Raw SMS body text (processed in-memory only).
     * @param sender SMS sender address or alphanumeric bank header.
     * @param timestamp Epoch timestamp in milliseconds.
     * @return true if successfully parsed and inserted; false if rejected, ignored, or duplicate.
     */
    suspend fun processIncomingSms(
        body: String,
        sender: String,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean {
        // Step 1: Check sender filter FIRST
        if (!BankSenderFilter.isBankSender(sender)) {
            return false
        }

        // Step 2: Parse SMS (OTP and non-transactions rejected first)
        val parseResult = parser.parse(body, sender, timestamp)
        if (parseResult !is ParseResult.Success) {
            return false
        }

        val candidate = parseResult.transaction

        // Step 3: Check user-defined payee rules
        val rules = mutableListOf<PayeeRule>()
        candidate.upiId?.let { upi ->
            payeeRuleRepository.getCategoryFor(upi)?.let { category ->
                rules.add(PayeeRule(upiId = upi, category = category))
            }
        }

        // Step 4: Categorize transaction
        val category = categorizer.categorize(candidate, rules)
        val transaction = candidate.toTransaction(category = category)

        // Step 5: Save via TransactionRepository (deduplication on refNumber)
        return transactionRepository.insert(transaction)
    }

    override fun ingest(sender: String, body: String): SmsIngestionResult {
        return runBlocking(Dispatchers.IO) {
            val inserted = processIncomingSms(body, sender)
            if (inserted) {
                SmsIngestionResult.Accepted
            } else if (!BankSenderFilter.isBankSender(sender)) {
                SmsIngestionResult.RejectedSender(sender)
            } else {
                SmsIngestionResult.Ignored
            }
        }
    }
}
