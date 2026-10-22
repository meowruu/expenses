package com.team.expensetracker.sms

/**
 * Sends fake bank SMS messages through the exact same processing path
 * that the real [android.content.BroadcastReceiver] will use at runtime.
 *
 * Pipeline order (matches the real receiver):
 *  1. [BankSenderFilter.isBankSender] — sender allowlist check.
 *  2. [handler].ingest(sender, body) — OTP detection, then parser + repo.
 *
 * F1 integration: call [simulate] with one of the [SampleMessages] constants
 * (or any custom fake message) when the user taps the "Simulate SMS" button.
 *
 * Privacy rules:
 *  - Fake bodies are only held in memory for the duration of the call.
 *  - NEVER pass real SMS bodies or OTPs through this class.
 *
 * @param handler The [SmsIngestor] implementation to forward accepted messages to.
 *                During development, pass [StubSmsHandler]. At integration, pass
 *                the real handler that calls B1's parser and B2's repository.
 */
class SimulatedSmsSource(
    private val handler: SmsIngestor = StubSmsHandler()
) {

    /**
     * Pre-built fake sample messages for testing.
     * All data is fictional (masked account numbers, fake names).
     * NEVER use real account numbers, names, or OTPs here.
     */
    object SampleMessages {

        /** A fake debit transaction SMS from Axis Bank. */
        const val SENDER_AXIS = "AX-AXISBK"
        const val BODY_DEBIT =
            "Dear Customer, INR 450.00 debited from A/c XXXX1234 on 22-Oct-26. " +
            "UPI Ref: 123456789012. Avail Bal: INR 12,000.00. -AxisBank"

        /** A fake credit transaction SMS from HDFC Bank. */
        const val SENDER_HDFC = "VM-HDFCBK"
        const val BODY_CREDIT =
            "Rs.2000.00 credited to A/c XXXX5678 on 22-Oct-26 by UPI. " +
            "UTR: 987654321098. Info: FAKENAME-9876543210@ybl. -HDFC Bank"

        /**
         * A fake OTP SMS — used to verify OTP rejection.
         * No real OTP value is ever stored here.
         */
        const val SENDER_OTP = "VM-SBIINB"
        const val BODY_OTP =
            "Your OTP for SBI transaction is XXXXXX. Valid for 5 minutes. Do not share."
    }

    /**
     * Simulates an incoming SMS through the full pipeline.
     *
     * @param sender The simulated sender ID (e.g. [SampleMessages.SENDER_AXIS]).
     * @param body   The simulated SMS body (use fake/masked data only).
     * @return       The [SmsIngestionResult] produced by the pipeline.
     */
    fun simulate(sender: String, body: String): SmsIngestionResult {
        return handler.ingest(sender, body)
    }
}
