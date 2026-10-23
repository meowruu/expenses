package com.team.expensetracker.sms

import android.content.Context
import com.team.expensetracker.data.di.DataLayerContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Public entry point to simulate an incoming bank SMS without cellular connectivity.
 *
 * Runs the exact same end-to-end pipeline:
 *  1. Bank sender ID check ([BankSenderFilter])
 *  2. B1 SMS parsing & OTP pre-parsing rejection ([com.team.expensetracker.parser.BankSmsParser])
 *  3. Payee rule lookup & transaction categorisation ([com.team.expensetracker.parser.DefaultTransactionCategorizer])
 *  4. Room database insertion via [com.team.expensetracker.data.repository.TransactionRepository]
 *     (deduplicating on refNumber, representing amount in Long paise)
 *
 * @param context Android context used to access [DataLayerContainer].
 * @param fakeText The simulated bank SMS text.
 * @param sender Optional bank sender header (e.g. "AXISBK", "HDFCBK", "SBIINB").
 *               If omitted, automatically inferred from message text or defaults to "AXISBK".
 * @return true if the simulated transaction was parsed and persisted; false if rejected, ignored, or duplicate.
 */
fun simulateSms(
    context: Context,
    fakeText: String,
    sender: String = detectOrFallbackBankSender(fakeText)
): Boolean {
    return runBlocking(Dispatchers.IO) {
        simulateSmsAsync(context, fakeText, sender)
    }
}

/**
 * Suspending variant of [simulateSms] for use within CoroutineScope / ViewModel.
 */
suspend fun simulateSmsAsync(
    context: Context,
    fakeText: String,
    sender: String = detectOrFallbackBankSender(fakeText)
): Boolean {
    val container = DataLayerContainer.getInstance(context.applicationContext)
    val pipeline = SmsProcessingPipeline(
        transactionRepository = container.transactionRepository,
        payeeRuleRepository = container.payeeRuleRepository
    )
    return pipeline.processIncomingSms(body = fakeText, sender = sender)
}

/**
 * Inspects [text] for bank hints or common prefixes to determine an appropriate bank header.
 */
fun detectOrFallbackBankSender(text: String): String {
    val trimmed = text.trim()
    // Check if text has a prefix like "AX-AXISBK: ..." or "HDFCBK - ..."
    val colonIdx = trimmed.indexOf(':')
    if (colonIdx in 3..12) {
        val potentialHeader = trimmed.substring(0, colonIdx).trim()
        if (BankSenderFilter.isBankSender(potentialHeader)) {
            return potentialHeader
        }
    }

    val lower = text.lowercase()
    return when {
        lower.contains("hdfc") -> "HDFCBK"
        lower.contains("sbi") || lower.contains("state bank") -> "SBIINB"
        lower.contains("icici") -> "ICICIB"
        lower.contains("kotak") -> "KOTAKB"
        lower.contains("pnb") || lower.contains("punjab national") -> "PNBSMS"
        else -> "AXISBK"
    }
}
