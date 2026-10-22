# B1 SMS Parser & OTP Rejection Handoff

**Role:** B1: SMS Parser and Brain
**Milestone:** Task 10 (SMS Parsing Engine), Task 12 (Pre-Parsing OTP Rejection Check) & Task 13 (Transaction Categorization)
**Target Git Branch:** `b1-parser`
**Base Package:** `com.team.expensetracker.parser`
**Target Audience:** **B3** (SMS Ingestion & Simulation), **B2** (Data Layer & Repository), **B4** (Security & Audit)

---

## 1. Overview & Architectural Guarantees

This document specifies the core SMS parser, pre-parsing rejection engine, and transaction categorizer built for the privacy-first on-device UPI expense tracker. The parser accepts incoming SMS metadata and converts valid Indian bank/UPI debit and credit SMS messages into structured transaction candidates, while identifying and discarding OTPs, promotional spam, login alerts, balance enquiries, and pending payment requests prior to amount extraction. The categorizer applies user-defined payee rules and an Indian merchant keyword dictionary to assign valid contract categories without database or Android framework dependencies.

### Non-Negotiable Privacy & Correctness Invariants
1. **Zero Raw SMS Retention (Strict Privacy Rule):** Under no circumstances is raw SMS body text stored, persisted, logged (no `println`, `Log.*`), or returned in any result object. The categorizer never receives or accesses raw SMS text.
2. **OTP Rejected First (Pre-Parsing Rejection):** 100% of OTP and verification code messages are detected and rejected *prior* to amount extraction or transaction processing. OTP values are never retained.
3. **Pure Kotlin / Platform-Independent:** The parser and categorizer contain zero Android framework dependencies (`android.*`). They run on pure JVM Kotlin, enabling fast, isolated unit testing.
4. **Integer Currency Arithmetic (`amountPaise: Long`):** Currency is handled strictly in paise (`1 INR = 100 paise`) using `Long` and `BigDecimal` safe parsing. `Float` and `Double` are strictly prohibited.
5. **Contract Category Compliance:** Categorizer assigns strictly from the canonical 12-category taxonomy defined in `Categories.kt` (`"Food"`, `"Groceries"`, `"Transport"`, `"Fuel"`, `"Shopping"`, `"Bills"`, `"Entertainment"`, `"Health"`, `"Education"`, `"Rent"`, `"Transfers"`, `"Uncategorized"`). User `PayeeRule` overrides always take absolute priority. This taxonomy is proposed by B1 and requires adoption by F1 (manual-entry dropdown, spending charts) and confirmation by B2.

---

## 2. Files Created

The parser and categorizer engines and test suites are contained within the following files:

| File Path | Description |
| :--- | :--- |
| `app/src/main/java/com/team/expensetracker/parser/RejectionReason.kt` | Enum of standardized rejection causes (OTP, Promo, Balance Enquiry, etc.). |
| `app/src/main/java/com/team/expensetracker/parser/ParsedTransaction.kt` | Data class representing structured candidate transaction metadata. |
| `app/src/main/java/com/team/expensetracker/parser/ParseResult.kt` | Sealed interface representing either `Success` or `Rejected`. |
| `app/src/main/java/com/team/expensetracker/parser/SmsParser.kt` | Pure Kotlin interface definition for SMS parsing. |
| `app/src/main/java/com/team/expensetracker/parser/BankSmsParser.kt` | Production implementation for Indian bank/UPI messages and OTP filtering. |
| `app/src/main/java/com/team/expensetracker/parser/Categories.kt` | Constants object defining contract category names ("Food", "Shopping", "Bills", "Uncategorized"). |
| `app/src/main/java/com/team/expensetracker/parser/TransactionCategorizer.kt` | Pure Kotlin interface and pipeline extension for transaction categorization. |
| `app/src/main/java/com/team/expensetracker/parser/DefaultTransactionCategorizer.kt` | Production categorizer with Indian merchant keyword dictionary and PayeeRule overrides. |
| `app/src/test/java/com/team/expensetracker/parser/BankSmsParserTest.kt` | JVM unit test suite covering UPI, cards, ATMs, refunds, OTPs, and edge cases. |
| `app/src/test/java/com/team/expensetracker/parser/TransactionCategorizerTest.kt` | JVM unit test suite covering merchant categories, PayeeRule precedence, and category validity. |
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

### 3.5 `Categories` Constants
```kotlin
package com.team.expensetracker.parser

object Categories {
    const val FOOD          = "Food"
    const val GROCERIES     = "Groceries"
    const val TRANSPORT     = "Transport"
    const val FUEL          = "Fuel"
    const val SHOPPING      = "Shopping"
    const val BILLS         = "Bills"
    const val ENTERTAINMENT = "Entertainment"
    const val HEALTH        = "Health"
    const val EDUCATION     = "Education"
    const val RENT          = "Rent"
    const val TRANSFERS     = "Transfers"
    const val UNCATEGORIZED = "Uncategorized"

    val ALL: Set<String> = setOf(
        FOOD, GROCERIES, TRANSPORT, FUEL, SHOPPING, BILLS,
        ENTERTAINMENT, HEALTH, EDUCATION, RENT, TRANSFERS, UNCATEGORIZED
    )
    fun isValid(category: String): Boolean = ALL.contains(category)
}
```

### 3.6 `TransactionCategorizer` Interface & Extension
```kotlin
package com.team.expensetracker.parser

import com.team.expensetracker.data.model.PayeeRule
import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType

interface TransactionCategorizer {
    fun categorize(
        transaction: ParsedTransaction,
        rules: List<PayeeRule> = emptyList()
    ): String

    fun categorize(
        merchant: String?,
        upiId: String?,
        type: TransactionType = TransactionType.DEBIT,
        rules: List<PayeeRule> = emptyList()
    ): String
}

fun ParsedTransaction.toCategorizedTransaction(
    categorizer: TransactionCategorizer,
    rules: List<PayeeRule> = emptyList(),
    id: Long = 0L
): Transaction
```

---

## 4. How to Integrate & Call the Parser & Categorizer (for B3 / Pipeline)

In B3's SMS receiver (`BroadcastReceiver`) or "Simulate SMS" pipeline:

```kotlin
import com.team.expensetracker.parser.BankSmsParser
import com.team.expensetracker.parser.DefaultTransactionCategorizer
import com.team.expensetracker.parser.ParseResult
import com.team.expensetracker.parser.toCategorizedTransaction
import com.team.expensetracker.data.model.PayeeRule
import com.team.expensetracker.data.repository.PayeeRuleRepository
import com.team.expensetracker.data.repository.TransactionRepository

class SmsProcessingPipeline(
    private val transactionRepository: TransactionRepository,
    private val payeeRuleRepository: PayeeRuleRepository,
    private val parser: BankSmsParser = BankSmsParser(),
    private val categorizer: DefaultTransactionCategorizer = DefaultTransactionCategorizer()
) {
    suspend fun processIncomingSms(body: String, sender: String, timestamp: Long) {
        when (val result = parser.parse(body, sender, timestamp)) {
            is ParseResult.Success -> {
                val candidate = result.transaction

                // 1. Fetch user-defined payee rule overrides if VPA is present
                val rules = mutableListOf<PayeeRule>()
                candidate.upiId?.let { upi ->
                    payeeRuleRepository.getCategoryFor(upi)?.let { category ->
                        rules.add(PayeeRule(upiId = upi, category = category))
                    }
                }

                // 2. Categorize and convert (Convenience Option A or B):
                // Option A:
                val tx = candidate.toTransaction(category = categorizer.categorize(candidate, rules))
                // Option B:
                // val tx = candidate.toCategorizedTransaction(categorizer, rules)

                // 3. Persist via repository (B2 handles refNumber deduplication)
                val inserted = transactionRepository.insert(tx)
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

## 5. Transaction Categorization Architecture (Task 13)

### 5.1 Category Schema

> **Note:** The following 12-category taxonomy is proposed by B1. F1 must adopt these exact strings in the manual-entry dropdown and spending charts. B2 confirmation is pending but no schema change is required (category is `String`).

| Category | Description |
|---|---|
| **`"Food"`** | Food delivery (Swiggy, Zomato, EatSure), restaurants, fast food (McDonald's, Domino's, KFC), bakeries, cafes (Starbucks, Chaayos). |
| **`"Groceries"`** | Instant grocery delivery (Blinkit, Zepto, BigBasket), supermarkets (DMart, Spencers), dairy (Mother Dairy, Country Delight). |
| **`"Transport"`** | Cabs (Uber, Ola, Rapido), metro (DMRC, Namma Metro), railways (IRCTC), buses (RedBus), flights (MakeMyTrip, Goibibo), tolls, parking, FastTag. |
| **`"Fuel"`** | Petrol, diesel, and CNG stations (HPCL, BPCL, IOCL, Indian Oil, Shell). |
| **`"Shopping"`** | E-commerce (Amazon, Flipkart, Myntra, Ajio), electronics (Croma, Vijay Sales), fashion/clothing (Zara, H&M, Decathlon), and general retail. |
| **`"Bills"`** | Electricity (BESCOM, Tata Power), water, cooking gas (LPG), mobile recharge, broadband (ACT, Hathway), DTH (Tata Play, Dish TV), BBPS/BillDesk utility payments. |
| **`"Entertainment"`** | Streaming subscriptions (Netflix, Spotify, Hotstar, Prime Video), cinema (PVR, INOX, BookMyShow), music (Gaana, Wynk). |
| **`"Health"`** | Pharmacies (Apollo, Netmeds, PharmEasy, 1mg, MedPlus), hospitals, clinics, diagnostic labs (Dr Lal, Metropolis), Practo. |
| **`"Education"`** | School/college/university fees, coaching institutes (ALLEN, Aakash, FIITJEE), ed-tech (BYJU'S, Unacademy, Coursera, Udemy). |
| **`"Rent"`** | Monthly house rent, landlord payments, society maintenance, and apartment maintenance charges. |
| **`"Transfers"`** | Person-to-person NEFT/IMPS/RTGS transfers, self-transfers, and peer UPI payments with no merchant context. |
| **`"Uncategorized"`** | Default fallback when no user rule or keyword matches. |

### 5.2 Resolution Hierarchy
1. **User `PayeeRule` Overrides (Highest Priority):** Evaluated first. If the transaction's `upiId` matches any rule in `rules` (case-insensitive, whitespace-trimmed), the rule's `category` is returned immediately. This guarantees that user customization always beats keyword heuristics.
2. **Indian Merchant Keyword Heuristics:** Matches merchant names and UPI handles against curated Indian merchant keywords and whole-word regex patterns.
3. **Credit & Refund Transactions:** Credit transactions with known merchant VPAs (such as merchant refunds) are categorized into the matching merchant category (e.g., Zomato refund -> "Food"). General credits without merchant context fall back cleanly to `"Uncategorized"`.
4. **Default Fallback:** Returns `"Uncategorized"` when neither user rules nor keyword heuristics match, or when counterparty metadata is null.

---

## 6. Scope & Status

### Implemented:
- **Task 10 (SMS Parsing Engine):** Complete pure-Kotlin parser extracting amount (in Long paise), debit/credit direction, merchant, UPI VPA, refNumber, date, bank, and masked account.
- **Task 12 (Pre-Parsing OTP Rejection Check):** Rejects OTPs, login alerts, promotional marketing, payment requests, and balance queries prior to extraction.
- **Task 13 (Transaction Categorization):** Complete pure-Kotlin categorizer with PayeeRule override precedence, Indian merchant keyword dictionary, and contract category compliance.

---

## 7. Known Limitations & Edge Cases

1. **Informal / Unstandardized SMS:** SMS notifications that do not use standard Indian bank keywords or delimiters (e.g., informal personal peer-to-peer SMS without banking headers) will be rejected with `NOT_A_TRANSACTION` or `UNPARSEABLE_AMOUNT`.
2. **Pending Mandates & Collect Requests:** Collect requests and UPI mandates that have not completed a debit or credit movement are rejected as `PAYMENT_REQUEST`. Once the user approves and the bank issues a debit confirmation SMS, the debit SMS will be parsed.
3. **Foreign Currency:** Multi-currency / international forex SMS outside INR are not currently parsed.
4. **New Unmapped Merchants:** Unknown small local merchants without recognizable food/shopping/utility keywords fall back safely to `"Uncategorized"`, and will be mapped once the user assigns a category via the Transaction Detail screen (which saves a `PayeeRule`).
