package com.team.expensetracker.sms

/**
 * TEMP STUB - replace with B1 parser + B2 repository at integration.
 *
 * This class stands in for the real SMS processing pipeline until B1's parser
 * and B2's Room repository are ready. It implements [SmsIngestor] so the rest
 * of the pipeline (BroadcastReceiver, SimulatedSmsSource) can be wired up now.
 *
 * HOW TO REPLACE (see docs/handoff-b3.md for full details):
 *  1. Replace this class with a real implementation that calls B1's parser.
 *  2. Pass the parsed Transaction to B2's TransactionRepository.insert(tx).
 *  3. Keep the same [SmsIngestor] interface — no other files need to change.
 *
 * PRIVACY RULES enforced here:
 *  - The SMS body is NEVER logged (log calls only emit sender + a fixed tag).
 *  - The SMS body is NOT stored anywhere.
 *  - OTP detection placeholder returns [SmsIngestionResult.Ignored].
 *
 * Logging: uses a simple JVM-compatible logger (no android.util.Log) so this
 * class can be unit-tested on the JVM. At integration time the real handler
 * can use android.util.Log freely.
 */
class StubSmsHandler(
    /** Pluggable logger: defaults to println for JVM tests; swap for android.util.Log in integration. */
    private val logger: (tag: String, msg: String) -> Unit = { tag, msg -> println("[$tag] $msg") }
) : SmsIngestor {

    companion object {
        private const val TAG = "StubSmsHandler"

        /**
         * Simple OTP signal words — B1 will replace this with a proper regex filter.
         * OTP messages must be rejected BEFORE any other processing (RULES.md).
         */
        private val OTP_KEYWORDS = setOf("otp", "one-time", "one time password", "verification code")
    }

    /**
     * Processes an incoming SMS through the stub pipeline.
     *
     * Checks sender via [BankSenderFilter], applies a basic OTP keyword scan
     * (to be replaced by B1's proper regex), then logs a short message without
     * touching the body content.
     *
     * @param sender The SMS sender ID.
     * @param body   The raw SMS body — passed in-memory, NEVER logged or stored.
     */
    override fun ingest(sender: String, body: String): SmsIngestionResult {
        // Step 1: sender filter
        if (!BankSenderFilter.isBankSender(sender)) {
            logger(TAG, "Rejected non-bank sender: $sender")
            return SmsIngestionResult.RejectedSender(sender)
        }

        // Step 2: OTP guard — must happen BEFORE any other processing (RULES.md)
        // TEMP: simple keyword check; B1 will replace with robust regex
        val bodyLower = body.lowercase()
        if (OTP_KEYWORDS.any { bodyLower.contains(it) }) {
            logger(TAG, "Ignored OTP message from $sender [body not logged]")
            return SmsIngestionResult.Ignored
        }

        // Step 3: Forward to parser + repository (STUB — just log the handoff point)
        // TODO: replace with → val tx = B1Parser.parse(sender, body) then repo.insert(tx)
        logger(TAG, "Accepted bank SMS from $sender — stub: no parsing yet [body not logged]")
        return SmsIngestionResult.Accepted
    }
}
