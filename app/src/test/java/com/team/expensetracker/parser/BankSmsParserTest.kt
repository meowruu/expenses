package com.team.expensetracker.parser

import com.team.expensetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM Unit Tests for [BankSmsParser].
 *
 * All test data strictly uses fake names, fake account digits (e.g. XX0000, XX1234),
 * and fake UPI IDs (e.g. fakeuser@fakebank).
 */
class BankSmsParserTest {

    private val parser = BankSmsParser()
    private val testTimestamp = 1774200000000L // Fake epoch millis

    // =========================================================================
    // 1. Core Transaction Types & Flows
    // =========================================================================

    @Test
    fun parse_upiDebit_successfullyExtractsAllFields() {
        val sms = "Dear Customer, Rs.500.00 has been debited from account XX1234 being a UPI transaction to fake-store@fakebank (UPI Ref no 123456789012). Avail Bal: Rs 12,345.00."
        val result = parser.parse(sms, "VK-HDFCBK", testTimestamp)

        assertTrue("Expected ParseResult.Success but was $result", result is ParseResult.Success)
        val tx = (result as ParseResult.Success).transaction

        assertEquals(50000L, tx.amountPaise)
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("fake-store@fakebank", tx.upiId)
        assertEquals("fake-store", tx.merchant)
        assertEquals("123456789012", tx.refNumber)
        assertEquals("XX1234", tx.accountLastDigits)
        assertEquals("HDFC", tx.bank)
        assertEquals(testTimestamp, tx.date)
    }

    @Test
    fun parse_upiCredit_successfullyExtractsAllFields() {
        val sms = "INR 2,500.00 credited to A/c XX5678 on 22-Oct-26 by transfer from friend@fakebank. UPI Ref: 334455667788. Avail Bal: INR 15,000.00"
        val result = parser.parse(sms, "AXISBK", testTimestamp)

        assertTrue("Expected ParseResult.Success", result is ParseResult.Success)
        val tx = (result as ParseResult.Success).transaction

        assertEquals(250000L, tx.amountPaise)
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals("friend@fakebank", tx.upiId)
        assertEquals("334455667788", tx.refNumber)
        assertEquals("XX5678", tx.accountLastDigits)
        assertEquals("AXIS", tx.bank)
    }

    @Test
    fun parse_cardDebitPurchase_extractsMerchantAndAccount() {
        val sms = "Spent Rs.1,500 on your Credit Card ending 1122 at FAKE SUPERMARKET on 22-Oct-26. Available limit: Rs. 45,000. Txn: 87654321."
        val result = parser.parse(sms, "SBINB", testTimestamp)

        assertTrue(result is ParseResult.Success)
        val tx = (result as ParseResult.Success).transaction

        assertEquals(150000L, tx.amountPaise)
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("FAKE SUPERMARKET", tx.merchant)
        assertEquals("XX1122", tx.accountLastDigits)
        assertEquals("87654321", tx.refNumber)
        assertEquals("SBI", tx.bank)
    }

    @Test
    fun parse_atmCashWithdrawal_extractsAmountAndAtmLabel() {
        val sms = "Rs. 2,000.00 withdrawn from ATM using Debit Card XX3344 on 22-Oct-26. Available Bal: Rs. 9,000.00. Txn ID: 998877."
        val result = parser.parse(sms, "ICICIB", testTimestamp)

        assertTrue(result is ParseResult.Success)
        val tx = (result as ParseResult.Success).transaction

        assertEquals(200000L, tx.amountPaise)
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals("ATM", tx.merchant)
        assertEquals("XX3344", tx.accountLastDigits)
        assertEquals("998877", tx.refNumber)
        assertEquals("ICICI", tx.bank)
    }

    @Test
    fun parse_refundCredit_correctlyIdentifiedAsCredit() {
        val sms = "Dear Customer, Refund of Rs 350.00 has been credited to your Fake Bank Account XX4321 on 22-Oct-26. Info: FAKE CAFE REFUND. Available balance is Rs 8,450.00."
        val result = parser.parse(sms, "VK-HDFCBK", testTimestamp)

        assertTrue(result is ParseResult.Success)
        val tx = (result as ParseResult.Success).transaction

        assertEquals(35000L, tx.amountPaise)
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals("FAKE CAFE REFUND", tx.merchant)
        assertEquals("XX4321", tx.accountLastDigits)
    }

    // =========================================================================
    // 2. Currency Formats, Comma Grouping & Exact Long Paise Precision
    // =========================================================================

    @Test
    fun parse_amountWithPaise_convertsExactlyToPaise() {
        val sms = "Rs.1,234.50 debited from A/c XX0000 on 22-Oct-26. Ref: 101."
        val result = parser.parse(sms, "HDFCBK", testTimestamp)

        assertTrue(result is ParseResult.Success)
        assertEquals(123450L, (result as ParseResult.Success).transaction.amountPaise)
    }

    @Test
    fun parse_amountInrWithoutPaise_convertsExactlyToPaise() {
        val sms = "INR 500 debited from A/c XX0000 for order at FAKE STORE. Ref: 102."
        val result = parser.parse(sms, "HDFCBK", testTimestamp)

        assertTrue(result is ParseResult.Success)
        assertEquals(50000L, (result as ParseResult.Success).transaction.amountPaise)
    }

    @Test
    fun parse_rupeeSymbolWithIndianCommaGrouping_convertsExactly() {
        val sms = "₹1,23,456.00 credited to A/c XX0000. Ref: 103."
        val result = parser.parse(sms, "AXISBK", testTimestamp)

        assertTrue(result is ParseResult.Success)
        assertEquals(12345600L, (result as ParseResult.Success).transaction.amountPaise)
    }

    @Test
    fun parse_rupeeSymbolWithoutPaise_convertsExactly() {
        val sms = "₹50 debited from A/c XX0000 at FAKE CHAI. Ref: 104."
        val result = parser.parse(sms, "SBINB", testTimestamp)

        assertTrue(result is ParseResult.Success)
        assertEquals(5000L, (result as ParseResult.Success).transaction.amountPaise)
    }

    @Test
    fun parse_rsWithSinglePaiseDigit_normalizesToHundredths() {
        val sms = "Rs. 10.5 debited from A/c XX0000. Ref: 105."
        val result = parser.parse(sms, "SBINB", testTimestamp)

        assertTrue(result is ParseResult.Success)
        assertEquals(1050L, (result as ParseResult.Success).transaction.amountPaise)
    }

    @Test
    fun parse_availableBalanceNotConfusedWithTransactionAmount() {
        val sms = "Rs 150.00 debited from A/c XX0000. Total Bal: Rs 85,432.10. Ref: 106."
        val result = parser.parse(sms, "HDFCBK", testTimestamp)

        assertTrue(result is ParseResult.Success)
        val tx = (result as ParseResult.Success).transaction
        // Must be the 150.00 transaction amount, NOT the 85,432.10 balance!
        assertEquals(15000L, tx.amountPaise)
    }

    // =========================================================================
    // 3. Pre-Parsing Rejection: OTP, Promo, Balance, Payment Requests, Failed Txn
    // =========================================================================

    @Test
    fun parse_otpMessagesWithDifferentWordings_areRejectedPriorToParsing() {
        val otpSamples = listOf(
            "123456 is the OTP for your online transaction of Rs. 500.00 at Swiggy. OTP is valid for 5 mins. Do not share this OTP with anyone.",
            "Your one-time-password for logging into Fake Bank is 987654. Never share your password with anyone.",
            "Verification code: 456789 for payment of INR 1,200.00. Valid for 10 minutes. Do not share.",
            "Secret code 112233 to approve payment of Rs 200. Validity: 3 mins.",
            "Security code: 778899. Do not disclose this code to anyone including bank staff.",
            "Use code 334455 to complete your net banking login. Do not share OTP."
        )

        for (otpMessage in otpSamples) {
            val result = parser.parse(otpMessage, "HDFCBK", testTimestamp)
            assertTrue("Expected OTP rejection for: $otpMessage", result is ParseResult.Rejected)
            assertEquals(RejectionReason.OTP, (result as ParseResult.Rejected).reason)
        }
    }

    @Test
    fun parse_promotionalAndOfferMessages_areRejected() {
        val promoSamples = listOf(
            "Congratulations! You are pre-approved for a personal loan of Rs 5,00,000 with zero processing fee. Click here to apply: https://fake.bank/loan",
            "Flat 50% off on all pizza orders today! Use promo code PIZZA50 on the app. Offer valid till midnight.",
            "Exclusive cashback offer: Earn up to Rs 100 on your next mobile recharge of INR 500 or more.",
            "Claim your reward of Rs 250! Apply for Fake Bank Platinum Credit Card today with free lounge access."
        )

        for (promoMessage in promoSamples) {
            val result = parser.parse(promoMessage, "BANKOFFER", testTimestamp)
            assertTrue("Expected PROMOTIONAL rejection", result is ParseResult.Rejected)
            assertEquals(RejectionReason.PROMOTIONAL, (result as ParseResult.Rejected).reason)
        }
    }

    @Test
    fun parse_balanceOnlyEnquiries_areRejected() {
        val balanceSamples = listOf(
            "Dear Customer, Available Balance in your A/C ending XX1234 as of 22-Oct-26 is Rs. 14,250.75. Thank you for banking with us.",
            "Clear Bal for A/c XX9999 is INR 3,200.00.",
            "Your account balance for A/c XX5555 is Rs. 1,050.00. Balance enquiry successful."
        )

        for (balMessage in balanceSamples) {
            val result = parser.parse(balMessage, "SBINB", testTimestamp)
            assertTrue("Expected BALANCE_ENQUIRY rejection", result is ParseResult.Rejected)
            assertEquals(RejectionReason.BALANCE_ENQUIRY, (result as ParseResult.Rejected).reason)
        }
    }

    @Test
    fun parse_paymentRequestsAndCollectMandates_areRejected() {
        val collectSamples = listOf(
            "User test@fakebank has requested Rs. 450.00 from you on UPI. Open your UPI app to approve or reject the collect request.",
            "Collect request of Rs 300 received from merchant@fakebank. Approve request to pay."
        )

        for (collectMessage in collectSamples) {
            val result = parser.parse(collectMessage, "UPIBANK", testTimestamp)
            assertTrue("Expected PAYMENT_REQUEST rejection", result is ParseResult.Rejected)
            assertEquals(RejectionReason.PAYMENT_REQUEST, (result as ParseResult.Rejected).reason)
        }
    }

    @Test
    fun parse_failedOrDeclinedTransactions_areRejected() {
        val failedSamples = listOf(
            "Your transaction of Rs. 1,000.00 at FAKE STORE on A/c XX1234 has failed due to technical issues. Ref No: 11223344.",
            "Payment of Rs 250 declined at FAKE CAFE. Insufficient balance in your account."
        )

        for (failedMessage in failedSamples) {
            val result = parser.parse(failedMessage, "AXISBK", testTimestamp)
            assertTrue("Expected FAILED_TRANSACTION rejection", result is ParseResult.Rejected)
            assertEquals(RejectionReason.FAILED_TRANSACTION, (result as ParseResult.Rejected).reason)
        }
    }

    // =========================================================================
    // 4. Malformed, Blank & Non-Transactional Messages
    // =========================================================================

    @Test
    fun parse_emptyOrBlankMessage_returnsEmptyMessageRejection() {
        val emptyResult = parser.parse("", "HDFCBK", testTimestamp)
        assertTrue(emptyResult is ParseResult.Rejected)
        assertEquals(RejectionReason.EMPTY_MESSAGE, (emptyResult as ParseResult.Rejected).reason)

        val whitespaceResult = parser.parse("   \n\t  ", "HDFCBK", testTimestamp)
        assertTrue(whitespaceResult is ParseResult.Rejected)
        assertEquals(RejectionReason.EMPTY_MESSAGE, (whitespaceResult as ParseResult.Rejected).reason)
    }

    @Test
    fun parse_nonTransactionalMessageWithoutAmount_returnsRejectionWithoutCrashing() {
        val nonTxnMessage = "Thank you for visiting your Fake Bank branch today. Please rate your customer service experience."
        val result = parser.parse(nonTxnMessage, "FAKEBK", testTimestamp)

        assertTrue(result is ParseResult.Rejected)
        assertEquals(RejectionReason.NOT_A_TRANSACTION, (result as ParseResult.Rejected).reason)
    }

    @Test
    fun parse_transactionActionWithoutAmount_returnsUnparseableAmountRejection() {
        val missingAmount = "Your account XX1234 was debited today at FAKE STORE but no digits here. Ref: 999."
        val result = parser.parse(missingAmount, "FAKEBK", testTimestamp)

        assertTrue(result is ParseResult.Rejected)
        assertEquals(RejectionReason.UNPARSEABLE_AMOUNT, (result as ParseResult.Rejected).reason)
    }

    // =========================================================================
    // 5. Strict Privacy Guarantee: Zero Raw Body in Results
    // =========================================================================

    @Test
    fun parseResult_neverContainsRawSmsBodyOrOtpSecret() {
        val rawSecretBody = "Dear Customer, Rs.500.00 has been debited from account XX1234 being a UPI transaction to secret-merchant@fakebank Ref 123456789012."
        val successResult = parser.parse(rawSecretBody, "HDFCBK", testTimestamp)

        assertTrue(successResult is ParseResult.Success)
        val tx = (successResult as ParseResult.Success).transaction

        // Verify none of the fields equal the full raw SMS body
        assertFalse(tx.merchant?.contains(rawSecretBody) == true)
        assertFalse(tx.upiId?.contains(rawSecretBody) == true)
        assertFalse(tx.refNumber?.contains(rawSecretBody) == true)
        assertFalse(tx.bank.contains(rawSecretBody))

        // Verify Rejection results also do not store the body
        val rawOtpBody = "123456 is your secret OTP for transaction. Do not share."
        val rejectedResult = parser.parse(rawOtpBody, "HDFCBK", testTimestamp)

        assertTrue(rejectedResult is ParseResult.Rejected)
        val rejection = rejectedResult as ParseResult.Rejected
        assertEquals(RejectionReason.OTP, rejection.reason)
        // Rejection only holds the enum reason, no body reference
    }

    // =========================================================================
    // 6. Mapping to Existing Domain Transaction
    // =========================================================================

    @Test
    fun parsedTransaction_toTransaction_mapsCleanlyToCoreModel() {
        val parsed = ParsedTransaction(
            amountPaise = 75000L,
            type = TransactionType.DEBIT,
            merchant = "FAKE STORE",
            upiId = "fakestore@fakebank",
            refNumber = "REF998877",
            date = testTimestamp,
            bank = "HDFC",
            accountLastDigits = "XX1234"
        )

        val domainTx = parsed.toTransaction(category = "Shopping", id = 12L)

        assertEquals(12L, domainTx.id)
        assertEquals(75000L, domainTx.amountPaise)
        assertEquals(TransactionType.DEBIT, domainTx.type)
        assertEquals("FAKE STORE", domainTx.merchant)
        assertEquals("fakestore@fakebank", domainTx.upiId)
        assertEquals("REF998877", domainTx.refNumber)
        assertEquals(testTimestamp, domainTx.date)
        assertEquals("HDFC", domainTx.bank)
        assertEquals("Shopping", domainTx.category)
    }
}
