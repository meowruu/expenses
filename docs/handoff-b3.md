# B3 SMS Pipeline Handoff

**Role:** B3: SMS, Permissions and Integration  
**Branch:** `b3-sms`  
**Package:** `com.team.expensetracker.sms`  
**Status:** ✅ `assembleDebug` + `testDebugUnitTest` both passing (27/27 tests)

---

## 1. Files Added / Managed

| File | Purpose |
|---|---|
| `app/src/main/java/com/team/expensetracker/sms/BankSenderFilter.kt` | Allowlist-based sender ID validator (strips operator prefixes, rejects phone numbers) |
| `app/src/main/java/com/team/expensetracker/sms/SmsIngestor.kt` | `SmsIngestor` interface + `SmsIngestionResult` sealed class |
| `app/src/main/java/com/team/expensetracker/sms/StubSmsHandler.kt` | **TEMP STUB** implementing `SmsIngestor`; logs event without logging body |
| `app/src/main/java/com/team/expensetracker/sms/SmsPipeline.kt` | Global pipeline holder with `@Volatile var ingestor: SmsIngestor = StubSmsHandler()` |
| `app/src/main/java/com/team/expensetracker/sms/SmsReceiver.kt` | System `BroadcastReceiver` joining multipart SMS in memory, checking `BankSenderFilter` first |
| `app/src/main/java/com/team/expensetracker/sms/SmsReceiverToggle.kt` | Runtime toggling of `SmsReceiver` via `PackageManager.setComponentEnabledSetting` |
| `app/src/main/java/com/team/expensetracker/sms/SimulatedSmsSource.kt` | Simulates SMS events through `SmsPipeline.ingestor` + sample messages |
| `app/src/test/java/com/team/expensetracker/sms/BankSenderFilterTest.kt` | 19 unit tests for bank header matching and edge cases |
| `app/src/test/java/com/team/expensetracker/sms/SimulatedSmsSourceTest.kt` | 7 unit tests for simulate flow, OTP rejection, and `SmsPipeline` swapping |

---

## 2. Manifest Additions (`app/src/main/AndroidManifest.xml`)

B3 added the following manifest declarations:

1. **Permission:**
   ```xml
   <uses-permission android:name="android.permission.RECEIVE_SMS" />
   ```
2. **Receiver Component:**
   ```xml
   <receiver
       android:name=".sms.SmsReceiver"
       android:enabled="false"
       android:exported="true"
       android:permission="android.permission.BROADCAST_SMS">
       <intent-filter>
           <action android:name="android.provider.Telephony.SMS_RECEIVED" />
       </intent-filter>
   </receiver>
   ```
3. **Hard Rules Maintained:**
   - `android:allowBackup="false"` strictly enforced on `<application>`.
   - **ZERO** `INTERNET` permission.
   - The receiver is disabled by default (`android:enabled="false"`) until enabled at runtime via `SmsReceiverToggle`.

*(Note for B4: B4 owns security hardening; B3 has communicated these required additions).*

---

## 3. Exact Public Signatures

### `BankSenderFilter` (object)

```kotlin
package com.team.expensetracker.sms

object BankSenderFilter {
    val BANK_SENDER_IDS: Set<String>
    fun isBankSender(senderId: String): Boolean
}
```

### `SmsIngestionResult` (sealed class) & `SmsIngestor` (interface)

```kotlin
package com.team.expensetracker.sms

sealed class SmsIngestionResult {
    object Accepted : SmsIngestionResult()
    data class RejectedSender(val sender: String) : SmsIngestionResult()
    object Ignored : SmsIngestionResult()
}

interface SmsIngestor {
    fun ingest(sender: String, body: String): SmsIngestionResult
}
```

### `SmsPipeline` (object)

```kotlin
package com.team.expensetracker.sms

object SmsPipeline {
    @Volatile
    var ingestor: SmsIngestor = StubSmsHandler()
}
```

### `SmsReceiver` (class)

```kotlin
package com.team.expensetracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent)
}
```

### `SmsReceiverToggle` (object)

```kotlin
package com.team.expensetracker.sms

import android.content.Context

object SmsReceiverToggle {
    fun setEnabled(context: Context, enabled: Boolean)
    fun isEnabled(context: Context): Boolean
}
```

### `SimulatedSmsSource` (class)

```kotlin
package com.team.expensetracker.sms

class SimulatedSmsSource(
    private val handler: SmsIngestor? = null // defaults to SmsPipeline.ingestor
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

## 4. Integration Guide: How B1 and B2 Plug In

At application startup or dependency injection initialization, B1's parser and B2's repository replace `StubSmsHandler` in one line:

```kotlin
// Example at App startup / DI module:
class ProductionSmsIngestor(
    private val parser: SmsParser,                  // From B1
    private val repository: TransactionRepository   // From B2
) : SmsIngestor {
    override fun ingest(sender: String, body: String): SmsIngestionResult {
        // Step 1: Sender check (guaranteed by receiver, but good to guard)
        if (!BankSenderFilter.isBankSender(sender)) {
            return SmsIngestionResult.RejectedSender(sender)
        }

        // Step 2: B1 parses financial details (or returns null if OTP / non-transaction)
        val transaction: Transaction? = parser.parse(sender, body)
        if (transaction == null) {
            return SmsIngestionResult.Ignored
        }

        // Step 3: B2 persists transaction locally in Room (returns false if duplicate refNumber)
        runBlocking {
            repository.insert(transaction)
        }

        return SmsIngestionResult.Accepted
    }
}

// Swapping into the pipeline:
SmsPipeline.ingestor = ProductionSmsIngestor(b1Parser, b2TransactionRepository)
```

Once `SmsPipeline.ingestor` is assigned:
- All real incoming SMS received by `SmsReceiver` will automatically flow into the production parser and repository.
- All simulated SMS sent via `SimulatedSmsSource().simulate(...)` will also automatically flow into the production parser and repository!

---

## 5. What F1 Can Call from UI

### 1. Auto-Logging Toggle Switch ("Settings" or "Explainer" screen)
```kotlin
// When user enables Auto-logging (after granting RECEIVE_SMS permission):
SmsReceiverToggle.setEnabled(context, true)

// When user disables Auto-logging:
SmsReceiverToggle.setEnabled(context, false)

// To query state:
val isAutoLoggingOn = SmsReceiverToggle.isEnabled(context)
```

### 2. "Simulate SMS" Button (Dev / Demo Flow)
```kotlin
val source = SimulatedSmsSource()

// Option A: Quick presets
source.simulate(
    sender = SimulatedSmsSource.SampleMessages.SENDER_AXIS,
    body   = SimulatedSmsSource.SampleMessages.BODY_DEBIT
)

// Option B: Custom test SMS
source.simulate(
    sender = "VM-HDFCBK",
    body   = "Rs 1500.00 debited from A/c XX1234 on 22-Oct-26 to UPI/swiggy@icici. Ref: 123456."
)
```

---

## 6. Privacy & Security Checklist

- [x] **Zero Raw SMS Storage / Logging**: The SMS body is never logged or written to persistent storage.
- [x] **Zero INTERNET Permission**: No network access declared anywhere in `AndroidManifest.xml`.
- [x] **allowBackup="false"**: Maintained in `AndroidManifest.xml`.
- [x] **Sender Allowlist First**: Non-bank senders are immediately discarded before touching message content.
- [x] **Receiver Protected & Disabled by Default**: `android:permission="android.permission.BROADCAST_SMS"` ensures only system telephony can invoke `SmsReceiver`; starts `android:enabled="false"` until user explicitly opts in.
