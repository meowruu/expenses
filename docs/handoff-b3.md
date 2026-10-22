# B3 SMS Pipeline Handoff

**Role:** B3: SMS, Permissions and Integration  
**Branch:** `b3-sms`  
**Package:** `com.team.expensetracker.sms`  
**Status:** ✅ `assembleDebug` + `testDebugUnitTest` both passing (37/37 tests)

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
| `app/src/main/java/com/team/expensetracker/sms/AutoLoggingState.kt` | `AutoLoggingState` sealed class + `SmsPermissionChecker`, `ReceiverSwitch`, `AutoLoggingStore` interfaces |
| `app/src/main/java/com/team/expensetracker/sms/AutoLoggingController.kt` | UI-free state machine for the privacy-first permission & auto-logging flow |
| `app/src/main/java/com/team/expensetracker/sms/AndroidAutoLoggingAdapters.kt` | Framework adapters (`AndroidSmsPermissionChecker`, `AndroidReceiverSwitch`, `SharedPreferencesAutoLoggingStore`, factory) |
| `app/src/main/java/com/team/expensetracker/sms/ExplainerText.kt` | User-facing privacy explainer copy constants (review requested from B4) |
| `app/src/test/java/com/team/expensetracker/sms/BankSenderFilterTest.kt` | 19 unit tests for bank header matching and edge cases |
| `app/src/test/java/com/team/expensetracker/sms/SimulatedSmsSourceTest.kt` | 7 unit tests for simulate flow, OTP rejection, and `SmsPipeline` swapping |
| `app/src/test/java/com/team/expensetracker/sms/AutoLoggingControllerTest.kt` | 10 unit tests covering all state machine transitions and self-healing |

---

## 2. Manifest Declarations (`app/src/main/AndroidManifest.xml`)

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
   - The receiver is disabled by default (`android:enabled="false"`) until enabled at runtime via `SmsReceiverToggle` upon explicit user consent.

---

## 3. Exact Public Signatures

### `AutoLoggingState` (sealed class) & Core Interfaces

```kotlin
package com.team.expensetracker.sms

sealed class AutoLoggingState {
    object Off : AutoLoggingState()
    object ShowingExplainer : AutoLoggingState()
    object RequestingPermission : AutoLoggingState()
    object PermissionDenied : AutoLoggingState()
    object On : AutoLoggingState()
}

interface SmsPermissionChecker {
    fun isGranted(): Boolean
}

interface ReceiverSwitch {
    fun setEnabled(enabled: Boolean)
}

interface AutoLoggingStore {
    var isEnabled: Boolean
}
```

### `AutoLoggingController` (class)

```kotlin
package com.team.expensetracker.sms

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

class AutoLoggingController(
    permissionChecker: SmsPermissionChecker,
    receiverSwitch: ReceiverSwitch,
    store: AutoLoggingStore
) {
    val state: StateFlow<AutoLoggingState>

    fun onEnableRequested()
    fun onExplainerAccepted()
    fun onExplainerDismissed()
    fun onPermissionResult(granted: Boolean)
    fun disable()
    fun refresh()

    companion object {
        fun create(context: Context): AutoLoggingController
    }
}

fun createAutoLoggingController(context: Context): AutoLoggingController
```

### Android Adapters (`AndroidAutoLoggingAdapters.kt`)

```kotlin
package com.team.expensetracker.sms

import android.content.Context

class AndroidSmsPermissionChecker(context: Context) : SmsPermissionChecker
class AndroidReceiverSwitch(context: Context) : ReceiverSwitch
class SharedPreferencesAutoLoggingStore(context: Context, prefsName: String = ...) : AutoLoggingStore
```

### `ExplainerText` (object)

```kotlin
package com.team.expensetracker.sms

object ExplainerText {
    const val TITLE: String
    const val WHICH_SMS_ARE_READ: String
    const val WHAT_IS_EXTRACTED: String
    const val WHAT_IS_NOT_KEPT: String
    const val NOTHING_LEAVES_PHONE: String
    const val BUTTON_ACCEPT: String
    const val BUTTON_DISMISS: String
}
```

### `BankSenderFilter` (object)

```kotlin
package com.team.expensetracker.sms

object BankSenderFilter {
    val BANK_SENDER_IDS: Set<String>
    fun isBankSender(senderId: String): Boolean
}
```

### `SmsPipeline` (object) & `SmsIngestor`

```kotlin
package com.team.expensetracker.sms

object SmsPipeline {
    @Volatile
    var ingestor: SmsIngestor = StubSmsHandler()
}

sealed class SmsIngestionResult {
    object Accepted : SmsIngestionResult()
    data class RejectedSender(val sender: String) : SmsIngestionResult()
    object Ignored : SmsIngestionResult()
}

interface SmsIngestor {
    fun ingest(sender: String, body: String): SmsIngestionResult
}
```

### `SmsReceiverToggle` (object) & `SimulatedSmsSource` (class)

```kotlin
package com.team.expensetracker.sms

import android.content.Context

object SmsReceiverToggle {
    fun setEnabled(context: Context, enabled: Boolean)
    fun isEnabled(context: Context): Boolean
}

class SimulatedSmsSource(
    handler: SmsIngestor? = null
) {
    fun simulate(sender: String, body: String): SmsIngestionResult

    object SampleMessages {
        const val SENDER_AXIS: String
        const val BODY_DEBIT: String
        const val SENDER_HDFC: String
        const val BODY_CREDIT: String
        const val SENDER_OTP: String
        const val BODY_OTP: String
    }
}
```

---

## 4. How F1 Uses This in Jetpack Compose

F1 can easily integrate the full privacy-first permission flow into the UI:

### 1. Initialize the Controller
```kotlin
// Inside ViewModel or remembered in Composable:
val controller = remember { AutoLoggingController.create(context) }
val autoLoggingState by controller.state.collectAsState()
```

### 2. Handle System Permission Launcher
```kotlin
val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted ->
    controller.onPermissionResult(isGranted)
}

// When state transitions to RequestingPermission, trigger system popup:
LaunchedEffect(autoLoggingState) {
    if (autoLoggingState is AutoLoggingState.RequestingPermission) {
        permissionLauncher.launch(android.Manifest.permission.RECEIVE_SMS)
    }
}
```

### 3. Show Explainer Dialog (Privacy-First Flow)
```kotlin
if (autoLoggingState is AutoLoggingState.ShowingExplainer) {
    AlertDialog(
        onDismissRequest = { controller.onExplainerDismissed() },
        title = { Text(ExplainerText.TITLE) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ExplainerText.WHICH_SMS_ARE_READ)
                Text(ExplainerText.WHAT_IS_EXTRACTED)
                Text(ExplainerText.WHAT_IS_NOT_KEPT)
                Text(ExplainerText.NOTHING_LEAVES_PHONE)
            }
        },
        confirmButton = {
            Button(onClick = { controller.onExplainerAccepted() }) {
                Text(ExplainerText.BUTTON_ACCEPT)
            }
        },
        dismissButton = {
            TextButton(onClick = { controller.onExplainerDismissed() }) {
                Text(ExplainerText.BUTTON_DISMISS)
            }
        }
    )
}
```

### 4. Toggle Switch Binding
```kotlin
Switch(
    checked = autoLoggingState is AutoLoggingState.On,
    onCheckedChange = { checked ->
        if (checked) {
            controller.onEnableRequested()
        } else {
            controller.disable()
        }
    }
)
```

### 5. Resyncing on Resume
```kotlin
// In MainActivity or screen lifecycle observer:
LifecycleResumeEffect(Unit) {
    controller.refresh()
    onPauseOrDispose { }
}
```

---

## 5. Android 13+ Note: "Allow Restricted Settings" for Sideloaded Installs

> [!IMPORTANT]
> **Android 13+ Sideloading Quirk:**
> When the app APK is sideloaded (e.g. via `adb install` or downloaded APK during a hackathon demo), Android 13+ marks sensitive permissions (`RECEIVE_SMS`) as **"Restricted settings"**.
> If the permission popup does not appear or appears greyed out / denied on a real device:
> 1. Long-press the App icon on the home screen -> tap **App info** (or go to **Settings -> Apps -> SpendLog**).
> 2. Tap the **three vertical dots** (⋮) in the top-right corner.
> 3. Tap **"Allow restricted settings"** and authenticate with device PIN/fingerprint.
> 4. Return to the app and toggle Auto-logging on again.
>
> *(Note: Do NOT build UI for this; keep this as an operational reminder for the golden demo device).*

---

## 6. How B1 and B2 Plug In

Replace `StubSmsHandler` in one step at application init:
```kotlin
SmsPipeline.ingestor = ProductionSmsIngestor(b1Parser, b2TransactionRepository)
```
Both live system SMS via `SmsReceiver` and fake SMS via `SimulatedSmsSource` immediately route through the production parser and Room database.
