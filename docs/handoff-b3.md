# B3 SMS Pipeline & Simulation Handoff

**Role:** B3: SMS Receiver, Ingestion Pipeline & Permission Helper  
**Target Git Branch:** `b3-sms`  
**Base Package:** `com.team.expensetracker.sms`  
**Verification:** ✅ `assembleDebug` + `testDebugUnitTest` passing (112/112 tests passed across the codebase, 42/42 in `sms`)

---

## 1. Overview & Architectural Guarantees

B3 delivers the complete live SMS reception pipeline, the simulated SMS pipeline, and the privacy-first permission state machine for the SpendLog application.

### Strict Privacy & Security Invariants
1. **Zero Raw SMS Retention:** Under NO circumstances is raw SMS body text stored in the database, logged via `Log.*` / `println`, or included in domain models.
2. **Pre-Parsing OTP Rejection:** All OTP / authentication messages are rejected *prior* to amount extraction and never saved.
3. **Zero INTERNET Permission:** The app has zero network access. No internet permission is declared in `AndroidManifest.xml`.
4. **Integer Currency Arithmetic (`amountPaise: Long`):** Currency is strictly handled in paise (`1 INR = 100 paise`) using `Long`.
5. **Bank Sender Filtering FIRST:** Messages from non-bank senders (personal numbers, marketing) are immediately discarded without processing.

---

## 2. Public API Specifications for F1 (Frontend)

### 2.1 `simulateSms` (Simulated Ingestion Pipeline)

Located in `com.team.expensetracker.sms.SimulateSms.kt`:

```kotlin
package com.team.expensetracker.sms

import android.content.Context

/**
 * Simulates incoming bank SMS processing without cellular connectivity.
 * Runs the exact same end-to-end pipeline:
 *  1. Bank sender ID validation ([BankSenderFilter])
 *  2. B1 SMS parsing & pre-parsing OTP rejection ([BankSmsParser])
 *  3. Payee rule lookup & transaction categorisation ([DefaultTransactionCategorizer])
 *  4. Room database insertion via [TransactionRepository] (deduplicating on refNumber)
 *
 * @param context Android Context used to access [DataLayerContainer].
 * @param fakeText The simulated bank SMS text.
 * @param sender Optional bank sender header (e.g. "AXISBK", "HDFCBK", "SBIINB").
 *               If omitted, automatically detected from text keywords or defaults to "AXISBK".
 * @return true if successfully parsed and inserted into Room; false if rejected, ignored, or duplicate.
 */
fun simulateSms(
    context: Context,
    fakeText: String,
    sender: String = detectOrFallbackBankSender(fakeText)
): Boolean

/**
 * Suspending variant of [simulateSms] for use within a CoroutineScope / ViewModel.
 */
suspend fun simulateSmsAsync(
    context: Context,
    fakeText: String,
    sender: String = detectOrFallbackBankSender(fakeText)
): Boolean
```

#### How F1 wires the "Simulate SMS" Dialog / Button:
```kotlin
// In HomeScreen.kt or AppNavHost.kt when user submits a test SMS:
val context = LocalContext.current
val coroutineScope = rememberCoroutineScope()

SimulateSmsDialog(
    onDismiss = { showDialog = false },
    onSend = { smsText ->
        coroutineScope.launch(Dispatchers.IO) {
            val inserted = simulateSmsAsync(context, smsText)
            // inserted == true -> Entry added to Room; reactive dashboard updates automatically!
            // inserted == false -> Message was duplicate, OTP, or non-transaction.
        }
        showDialog = false
    }
)
```

---

### 2.2 Permission Helper (`AutoLoggingController` & `AutoLoggingState`)

Located in `com.team.expensetracker.sms.AutoLoggingController.kt` and `AutoLoggingState.kt`:

```kotlin
package com.team.expensetracker.sms

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

sealed class AutoLoggingState {
    object Off : AutoLoggingState()
    object ShowingExplainer : AutoLoggingState()
    object RequestingPermission : AutoLoggingState()
    object PermissionDenied : AutoLoggingState()
    object On : AutoLoggingState()
}

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

#### How F1 wires Auto-Logging & Privacy Explainer in Compose:
```kotlin
// 1. Instantiate the controller (in ViewModel or remembered in Composable):
val context = LocalContext.current
val controller = remember { AutoLoggingController.create(context) }
val autoLoggingState by controller.state.collectAsState()

// 2. Set up the Android system permission request launcher:
val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted ->
    controller.onPermissionResult(isGranted)
}

// 3. React to state changes:
LaunchedEffect(autoLoggingState) {
    if (autoLoggingState is AutoLoggingState.RequestingPermission) {
        permissionLauncher.launch(android.Manifest.permission.RECEIVE_SMS)
    }
}

// 4. Privacy Explainer Dialog:
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

// 5. Settings Screen Toggle Switch:
Switch(
    checked = autoLoggingState is AutoLoggingState.On,
    onCheckedChange = { isChecked ->
        if (isChecked) {
            controller.onEnableRequested()
        } else {
            controller.disable()
        }
    }
)

// 6. Lifecycle Sync in onResume:
LifecycleResumeEffect(Unit) {
    controller.refresh()
    onPauseOrDispose { }
}
```

---

## 3. Live BroadcastReceiver & Manifest Declarations

- **Receiver:** `com.team.expensetracker.sms.SmsReceiver`
- **Dynamic Toggle:** `com.team.expensetracker.sms.SmsReceiverToggle`

### Manifest Configuration (`AndroidManifest.xml`)
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.RECEIVE_SMS" />

    <application
        android:allowBackup="false" ...>

        <receiver
            android:name=".sms.SmsReceiver"
            android:enabled="false"
            android:exported="true"
            android:permission="android.permission.BROADCAST_SMS">
            <intent-filter>
                <action android:name="android.provider.Telephony.SMS_RECEIVED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

- Protected by `android.permission.BROADCAST_SMS` so only system telephony can invoke it.
- Initial state is disabled (`android:enabled="false"`).
- Enabled dynamically at runtime solely upon explicit user consent via `SmsReceiverToggle.setEnabled(context, true)`.

---

## 4. Android 13+ Note: "Allow Restricted Settings" for Sideloaded APKs

> [!IMPORTANT]
> When testing on a real device with Android 13+ via sideloaded APK (`adb install`):
> Android security policy marks `RECEIVE_SMS` as **"Restricted settings"**.
> If the permission dialog does not appear or says permission is restricted:
> 1. Open device **Settings -> Apps -> SpendLog**.
> 2. Tap the **three dots menu (⋮)** in the top right corner.
> 3. Select **"Allow restricted settings"** and authenticate with device PIN.
> 4. Return to SpendLog and enable Auto-logging.

---

## 5. Summary of Files in `com.team.expensetracker.sms`

| File | Purpose |
|---|---|
| `BankSenderFilter.kt` | Validates 6-letter bank headers (AXISBK, HDFCBK, SBIINB, etc.) and strips operator prefixes |
| `SmsProcessingPipeline.kt` | End-to-end pipeline: sender filter -> B1 parser -> categorizer -> B2 Room repository |
| `SimulateSms.kt` | `simulateSms(context, fakeText)` function for non-cellular testing & UI button |
| `SmsReceiver.kt` | System `BroadcastReceiver` joining multipart SMS and running the pipeline in background |
| `SmsReceiverToggle.kt` | Runtime enabling/disabling of `SmsReceiver` via `PackageManager` |
| `AutoLoggingState.kt` | Permission lifecycle state machine and decoupled interface abstractions |
| `AutoLoggingController.kt` | StateFlow controller managing explainer -> permission -> active state |
| `AndroidAutoLoggingAdapters.kt` | Framework implementations (`ContextCompat`, `SharedPreferences`) and factory |
| `ExplainerText.kt` | User-facing copy constants for privacy explainer dialog |
| `SimulatedSmsSource.kt` | Simulated SMS event holder with sample bank SMS messages |
| `StubSmsHandler.kt` | Safe stub implementation of `SmsIngestor` |
| `SmsPipeline.kt` | Global pipeline holder |
