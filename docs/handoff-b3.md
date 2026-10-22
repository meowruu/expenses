# B3 SMS Pipeline Handoff

**Role:** B3: SMS, Permissions and Integration
**Branch:** `b3-sms`
**Package:** `com.team.expensetracker.sms`
**Status:** ✅ assembleDebug + testDebugUnitTest both passing (25/25 tests)

---

## 1. Files Added

| File | Purpose |
|---|---|
| `app/src/main/java/com/team/expensetracker/sms/BankSenderFilter.kt` | Allowlist-based sender ID validator |
| `app/src/main/java/com/team/expensetracker/sms/SmsIngestor.kt` | `SmsIngestor` interface + `SmsIngestionResult` sealed class |
| `app/src/main/java/com/team/expensetracker/sms/StubSmsHandler.kt` | **TEMP STUB** implementing `SmsIngestor`; replace at integration |
| `app/src/main/java/com/team/expensetracker/sms/SimulatedSmsSource.kt` | Simulate SMS flow + 3 fake sample messages |
| `app/src/test/java/com/team/expensetracker/sms/BankSenderFilterTest.kt` | 20 unit tests for the sender filter |
| `app/src/test/java/com/team/expensetracker/sms/SimulatedSmsSourceTest.kt` | 5 unit tests for the simulate flow |

No files outside `sms/` were modified. No `AndroidManifest.xml` changes. No INTERNET permission added.

---

## 2. Exact Public Signatures

### `BankSenderFilter` (object)

```kotlin
package com.team.expensetracker.sms

object BankSenderFilter {
    /** Allowlist of 6-letter bank sender headers (upper-case). Add more here. */
    val BANK_SENDER_IDS: Set<String>

    /**
     * Returns true if senderId is a recognised bank sender.
     * Strips "XX-" operator prefix before matching. Case-insensitive.
     * Rejects personal phone numbers (+91..., pure-digit strings).
     */
    fun isBankSender(senderId: String): Boolean
}
```

### `SmsIngestionResult` (sealed class)

```kotlin
package com.team.expensetracker.sms

sealed class SmsIngestionResult {
    object Accepted : SmsIngestionResult()
    data class RejectedSender(val sender: String) : SmsIngestionResult()
    object Ignored : SmsIngestionResult()
}
```

### `SmsIngestor` (interface)

```kotlin
package com.team.expensetracker.sms

interface SmsIngestor {
    fun ingest(sender: String, body: String): SmsIngestionResult
}
```

### `StubSmsHandler` (class — TEMP STUB)

```kotlin
package com.team.expensetracker.sms

class StubSmsHandler(
    logger: (tag: String, msg: String) -> Unit = { tag, msg -> println("[$tag] $msg") }
) : SmsIngestor {
    override fun ingest(sender: String, body: String): SmsIngestionResult
}
```

### `SimulatedSmsSource` (class)

```kotlin
package com.team.expensetracker.sms

class SimulatedSmsSource(
    private val handler: SmsIngestor = StubSmsHandler()
) {
    fun simulate(sender: String, body: String): SmsIngestionResult

    object SampleMessages {
        const val SENDER_AXIS: String   // "AX-AXISBK"
        const val BODY_DEBIT: String    // fake Axis Bank debit SMS
        const val SENDER_HDFC: String   // "VM-HDFCBK"
        const val BODY_CREDIT: String   // fake HDFC credit SMS
        const val SENDER_OTP: String    // "VM-SBIINB"
        const val BODY_OTP: String      // fake OTP SMS (no real OTP value)
    }
}
```

---

## 3. How B1 (Parser) and B2 (Repository) Plug In

### What to replace

Open [`StubSmsHandler.kt`](../app/src/main/java/com/team/expensetracker/sms/StubSmsHandler.kt) and find the comment:

```
// TODO: replace with → val tx = B1Parser.parse(sender, body) then repo.insert(tx)
```

Replace `StubSmsHandler` with a real implementation (e.g. `LiveSmsHandler`) that:

1. **Calls B1's parser** with the raw `body` string in-memory:
   ```kotlin
   val tx: Transaction? = B1Parser.parse(sender, body)
   ```
   - If `tx` is `null` (B1 couldn't parse it), return `SmsIngestionResult.Ignored`.

2. **Calls B2's repository** with the parsed transaction:
   ```kotlin
   val inserted = transactionRepository.insert(tx)
   // inserted == false means duplicate refNumber → safe to ignore
   ```

3. Keep `BankSenderFilter.isBankSender()` as Step 1 (do NOT remove it).

4. Keep the OTP guard as Step 2, or replace it with B1's proper regex filter.

### Constructor to use

```kotlin
class LiveSmsHandler(
    private val repository: TransactionRepository,        // from B2
    private val payeeRuleRepo: PayeeRuleRepository        // from B2 (optional, for categorisation)
) : SmsIngestor { ... }
```

Both `TransactionRepository` and `PayeeRuleRepository` are defined in
`com.team.expensetracker.data.repository` — see [`docs/handoff-b2.md`](handoff-b2.md).

### Wire up in BroadcastReceiver (B3's next task)

```kotlin
class BankSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages?.forEach { sms ->
            val result = handler.ingest(sms.originatingAddress ?: "", sms.messageBody)
            // handle result
        }
    }
}
```

The `handler` should be injected (DI or passed from Application).

---

## 4. What F1 Can Call for the "Simulate SMS" Button

F1 needs a reference to a `SimulatedSmsSource` instance (injected via ViewModel or passed from the Activity):

```kotlin
// In F1's ViewModel or composable button handler:
val source = SimulatedSmsSource(handler = /* LiveSmsHandler or StubSmsHandler */)

// User taps "Simulate Debit":
source.simulate(
    sender = SimulatedSmsSource.SampleMessages.SENDER_AXIS,
    body   = SimulatedSmsSource.SampleMessages.BODY_DEBIT
)

// User taps "Simulate Credit":
source.simulate(
    sender = SimulatedSmsSource.SampleMessages.SENDER_HDFC,
    body   = SimulatedSmsSource.SampleMessages.BODY_CREDIT
)
```

F1 can also provide a custom sender/body text field and call `source.simulate(sender, body)` with user-entered fake data.

**Important:** F1 must never pass real SMS bodies through this function — only fake/masked test data.

---

## 5. Open Questions / Blockers

| # | Question | Owner | Priority |
|---|---|---|---|
| 1 | `AndroidManifest.xml` needs `RECEIVE_SMS` and `READ_SMS` permissions + the `BankSmsReceiver` `<receiver>` entry. Awaiting B4 to add manifest entries (Task 23). | **B4** | High |
| 2 | Android 13+ requires `RECEIVE_SMS` under "restricted settings" — user must allow via Settings. B3 must test on a real device (Task 21). | **B3** | High |
| 3 | B1 hasn't defined their parser interface yet. The `TODO` in `StubSmsHandler` is the integration point — B1 should define a `parse(sender, body): Transaction?` function and tell B3. | **B1** | High |
| 4 | `SimulatedSmsSource` currently uses `StubSmsHandler` by default. Once B1+B2 are ready, the default should be swapped to `LiveSmsHandler`. This is a one-line change in the constructor default. | **B3 at integration** | Medium |
| 5 | `BankSenderFilter.BANK_SENDER_IDS` should be reviewed/extended by the whole team when real bank SMS samples (Task 5) are collected. | **All** | Medium |
| 6 | OTP detection in `StubSmsHandler` is a simple keyword check. B1 should provide a robust regex-based `isOtp(body): Boolean` function to replace it. | **B1** | Medium |
| 7 | F1 needs the `SimulatedSmsSource` wired into a ViewModel for the Simulate SMS button. B3 should coordinate with F1 on the ViewModel interface. | **B3 + F1** | Medium |

---

## 6. Privacy Compliance Checklist

- [x] Raw SMS body is never stored, persisted, or logged anywhere in `sms/`
- [x] OTP messages are detected and rejected **before** any other processing
- [x] No `INTERNET` permission added
- [x] `android:allowBackup="false"` not touched (B4's domain)
- [x] No `Float` or `Double` used for currency (money not handled in this task)
- [x] `AndroidManifest.xml` not modified in this task
