# B1 SMS Parser & OTP Rejection Handoff

**Role:** B1: SMS Parser and Brain  
**Milestone:** Task 10 (SMS Parsing Engine) & Task 12 (Pre-Parsing OTP Rejection Check)  
**Target Git Branch:** `b1-parser`  
**Base Package:** `com.team.expensetracker.parser`  
**Target Audience:** **B3** (SMS Ingestion & Simulation), **B2** (Data Layer & Repository), **B4** (Security & Audit)

---

## 1. Overview & Architectural Guarantees

This document specifies the core SMS parser and pre-parsing rejection engine built for the privacy-first on-device UPI expense tracker. The parser accepts incoming SMS metadata and converts valid Indian bank/UPI debit and credit SMS messages into structured transaction candidates, while identifying and discarding OTPs, promotional spam, login alerts, balance enquiries, and pending payment requests prior to amount extraction.

### Non-Negotiable Privacy & Correctness Invariants
1. **Zero Raw SMS Retention (Strict Privacy Rule):** Under no circumstances is raw SMS body text stored, persisted, logged (no `println`, `Log.*`), or returned in any result object. The result data models strictly contain parsed, structured metadata.
2. **OTP Rejected First (Pre-Parsing Rejection):** 100% of OTP and verification code messages are detected and rejected *prior* to amount extraction or transaction processing. OTP values are never retained.
3. **Pure Kotlin / Platform-Independent:** The parser contains zero Android framework dependencies (`android.*`). It runs on pure JVM Kotlin, enabling fast, isolated unit testing.
4. **Integer Currency Arithmetic (`amountPaise: Long`):** Currency is handled strictly in paise (`1 INR = 100 paise`) using `Long` and `BigDecimal` safe parsing. `Float` and `Double` are strictly prohibited.
5. **Clean Separation of Concerns:** Categorization (Task 13) is intentionally deferred; the parser yields `ParsedTransaction`, which can be converted cleanly to `Transaction` using `toTransaction(category)`.

---

## 2. Files Created

The parser engine and test suite are contained within the following files:

| File Path | Description |
| :--- | :--- |
| `app/src/main/java/com/team/expensetracker/parser/RejectionReason.kt` | Enum of standardized rejection causes (OTP, Promo, Balance Enquiry, etc.). |
| `app/src/main/java/com/team/expensetracker/parser/ParsedTransaction.kt` | Data class representing structured candidate transaction metadata. |
| `app/src/main/java/com/team/expensetracker/parser/ParseResult.kt` | Sealed interface representing either `Success` or `Rejected`. |
| `app/src/main/java/com/team/expensetracker/parser/SmsParser.kt` | Pure Kotlin interface definition for SMS parsing. |
| `app/src/main/java/com/team/expensetracker/parser/BankSmsParser.kt` | Production implementation for Indian bank/UPI messages and OTP filtering. |
| `app/src/test/java/com/team/expensetracker/parser/BankSmsParserTest.kt` | JVM unit test suite covering UPI, cards, ATMs, refunds, OTPs, and edge cases. |
| `docs/handoff-b1.md` | This technical contract and integration guide. |

---

## 3. Public API Specification

### 3.1 `SmsParser` Interface
```kotlin
package com.team.expensetracker.parser

interface SmsParser {
    /**
     * Evaluates and parses an incoming SMS notification.
     *
     * @param body Raw SMS message text.
     * @param sender Sender address or bank header (e.g. "AXISBK", "VK-HDFCBK").
     * @param receivedTimestamp Epoch millisecond timestamp when the SMS was received.
     * @return [ParseResult.Success] containing parsed candidate, or [ParseResult.Rejected] with a reason.
     */
    fun parse(body: String, sender: String, receivedTimestamp: Long): ParseResult
}
```

### 3.2 `BankSmsParser`
```kotlin
package com.team.expensetracker.parser

/**
 * Production implementation of [SmsParser] for Indian bank & UPI SMS notifications.
 */
class BankSmsParser : SmsParser {
    override fun parse(body: String, sender: String, receivedTimestamp: Long): ParseResult
}
```

### 3.3 `ParseResult` & `RejectionReason`
```kotlin
package com.team.expensetracker.parser

sealed interface ParseResult {
    data class Success(val transaction: ParsedTransaction) : ParseResult
    data class Rejected(val reason: RejectionReason) : ParseResult
}

enum class RejectionReason {
    EMPTY_MESSAGE,
    OTP,
    SECURITY_OR_LOGIN_ALERT,
    PROMOTIONAL,
    PAYMENT_REQUEST,
    FAILED_TRANSACTION,
    BALANCE_ENQUIRY,
    NOT_A_TRANSACTION,
    UNPARSEABLE_AMOUNT,
    UNKNOWN_DIRECTION
}
```

### 3.4 `ParsedTransaction`
```kotlin
package com.team.expensetracker.parser

import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType

data class ParsedTransaction(
    val amountPaise: Long,
    val type: TransactionType,
    val merchant: String?,
    val upiId: String?,
    val refNumber: String?,
    val date: Long,
    val bank: String,
    val accountLastDigits: String? = null
) {
    /**
     * Converts this parsed transaction candidate into the core domain [Transaction] entity.
     *
     * @param category The assigned expense category (defaults to "Uncategorized").
     * @param id The persistent database ID (defaults to 0L for unpersisted entities).
     */
    fun toTransaction(category: String = "Uncategorized", id: Long = 0L): Transaction {
        return Transaction(
            id = id,
            amountPaise = amountPaise,
            type = type,
            merchant = merchant,
            upiId = upiId,
            refNumber = refNumber,
            date = date,
            bank = bank,
            category = category
        )
    }
}
```

---

## 4. How to Integrate & Call the Parser (for B3 / Pipeline)

In B3's SMS receiver (`BroadcastReceiver`) or "Simulate SMS" pipeline:

```kotlin
import com.team.expensetracker.parser.BankSmsParser
import com.team.expensetracker.parser.ParseResult
import com.team.expensetracker.data.repository.TransactionRepository

class SmsProcessingPipeline(
    private val repository: TransactionRepository,
    private val parser: BankSmsParser = BankSmsParser()
) {
    suspend fun processIncomingSms(body: String, sender: String, timestamp: Long) {
        when (val result = parser.parse(body, sender, timestamp)) {
            is ParseResult.Success -> {
                val candidate = result.transaction
                
                // Downstream categorization will hook in here (Task 13)
                // For now, default category is "Uncategorized"
                val tx = candidate.toTransaction(category = "Uncategorized")
                
                // B2's repository handles reference number deduplication
                val inserted = repository.insert(tx)
                if (!inserted) {
                    // Duplicate reference number ignored cleanly
                }
            }
            is ParseResult.Rejected -> {
                // Rejected silently without logging or persisting the SMS body
                // result.reason indicates why (e.g. OTP, PROMOTIONAL, BALANCE_ENQUIRY)
            }
        }
    }
}
```

---

## 5. Scope & Deferred Features

### Implemented in this Milestone:
- Pre-parsing rejection check for OTPs, verification codes, and security PIN warnings.
- Pre-parsing rejection for login alerts, promotional marketing, balance-only queries, and pending collect requests.
- Safe Indian currency parsing (`Rs.`, `INR`, `₹`, Indian comma grouping like `1,23,456.78` and integer amounts) into `Long` paise.
- Debit vs Credit direction resolution mapped to `TransactionType`.
- Merchant / Payee extraction and UPI VPA extraction (`name@bank`).
- Bank reference / UTR extraction for deduplication.
- Masked account / card digits extraction (`XX1234`).
- Bank name resolution from sender header (e.g. `VK-HDFCBK` -> `HDFC`, `AXISBK` -> `AXIS`) or body text.

### NOT Implemented Yet (Deferred to Task 13 Categorization):
- Automatic expense categorization (keyword dictionary, merchant guessing, user payee rule overrides via `PayeeRuleRepository`).
- Currently, candidate transactions default to `"Uncategorized"` when converted via `toTransaction()`.

---

## 6. Known Limitations & Edge Cases

1. **Informal / Unstandardized SMS:** SMS notifications that do not use standard Indian bank keywords or delimiters (e.g., informal personal peer-to-peer SMS without banking headers) will be rejected with `NOT_A_TRANSACTION` or `UNPARSEABLE_AMOUNT`.
2. **Pending Mandates & Collect Requests:** Collect requests and UPI mandates that have not completed a debit or credit movement are rejected as `PAYMENT_REQUEST`. Once the user approves and the bank issues a debit confirmation SMS, the debit SMS will be parsed.
3. **Foreign Currency:** Multi-currency / international forex SMS outside INR are not currently parsed.
