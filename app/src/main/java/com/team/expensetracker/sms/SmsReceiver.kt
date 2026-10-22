package com.team.expensetracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * BroadcastReceiver listening for incoming SMS messages broadcast by the system.
 *
 * Privacy Invariants:
 *  1. Only processes messages from recognized bank senders ([BankSenderFilter.isBankSender]).
 *     Non-bank messages are immediately discarded.
 *  2. NEVER logs, prints, or persists the raw SMS body or any OTP.
 *  3. Ingests and joins multipart messages entirely in-memory before passing to [SmsPipeline.ingestor].
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        // Group messages by originating address in case multiple senders arrive simultaneously
        val messagesBySender = messages.groupBy { it.originatingAddress.orEmpty() }

        for ((sender, partList) in messagesBySender) {
            if (sender.isBlank()) continue

            // STEP 1: Check sender filter FIRST before any processing
            if (!BankSenderFilter.isBankSender(sender)) {
                // Drop immediately without examining or logging message bodies
                continue
            }

            // STEP 2: Join multipart message parts in-memory only
            val body = partList.joinToString(separator = "") { it.messageBody.orEmpty() }

            // STEP 3: Forward in-memory to the pipeline (B1 parser + B2 repository via ingestor)
            SmsPipeline.ingestor.ingest(sender, body)
        }
    }
}
