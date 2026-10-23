package com.team.expensetracker.sms

/**
 * User-facing copy for the SMS permission explainer dialog/screen.
 *
 * NOTE FOR B4 (Security Lead):
 * Please review and verify this wording before the final demo and submission.
 * It must accurately represent our privacy and security guarantees.
 */
object ExplainerText {
    const val TITLE = "Why SpendLog Needs SMS Access"

    /** Explains that only SMS from recognized bank sender IDs are read. */
    const val WHICH_SMS_ARE_READ =
        "SpendLog ONLY inspects messages received from recognized bank sender headers " +
        "(such as HDFCBK, SBIINB, AXISBK, ICICIB). Personal text messages and non-banking " +
        "senders are completely ignored and discarded immediately."

    /** Explains what financial metadata is extracted. */
    const val WHAT_IS_EXTRACTED =
        "When an official bank transaction arrives, we extract only structured financial details: " +
        "amount spent or received, transaction type (debit/credit), merchant name or UPI ID, " +
        "bank reference/UTR number, and date."

    /** Explains what is NOT stored or logged. */
    const val WHAT_IS_NOT_KEPT =
        "Under NO circumstances is the raw SMS message body stored, saved, or logged. " +
        "OTP (One-Time Password) messages are identified and dropped immediately without any parsing."

    /** Explains that processing is 100% on-device with zero network access. */
    const val NOTHING_LEAVES_PHONE =
        "Everything runs 100% on your device. The app has zero INTERNET permissions, meaning " +
        "your financial information can never leave your phone."

    const val BUTTON_ACCEPT = "I Understand, Enable Auto-Logging"
    const val BUTTON_DISMISS = "Not Now"
}
