package com.team.expensetracker.sms

/**
 * State of the Auto-Logging permission and interception lifecycle.
 *
 * Flow:
 *  - [Off]: Auto-logging is disabled.
 *  - [ShowingExplainer]: User requested enabling auto-logging, but permission is missing;
 *    F1 UI displays privacy explainer.
 *  - [RequestingPermission]: User accepted explainer; F1 UI requests system RECEIVE_SMS permission.
 *  - [PermissionDenied]: System permission request was denied; receiver stays disabled.
 *  - [On]: Permission granted and auto-logging active; incoming bank SMS are intercepted.
 */
sealed class AutoLoggingState {
    /** Auto-logging is off. */
    object Off : AutoLoggingState()

    /** Showing the privacy explainer dialog/screen to the user before requesting system permission. */
    object ShowingExplainer : AutoLoggingState()

    /** Triggered after user accepts explainer; signals F1 UI to invoke system permission popup. */
    object RequestingPermission : AutoLoggingState()

    /** The user denied the system permission request; receiver remains disabled. */
    object PermissionDenied : AutoLoggingState()

    /** Auto-logging is enabled and active. */
    object On : AutoLoggingState()
}

/**
 * Abstraction for querying the system SMS permission status.
 * Decoupled from Android Context to enable JVM unit testing.
 */
interface SmsPermissionChecker {
    /** Returns true if android.permission.RECEIVE_SMS is granted. */
    fun isGranted(): Boolean
}

/**
 * Abstraction for enabling/disabling the SMS receiver component.
 * Decoupled from Android Context to enable JVM unit testing.
 */
interface ReceiverSwitch {
    /** Enables or disables the SMS broadcast receiver component. */
    fun setEnabled(enabled: Boolean)
}

/**
 * Abstraction for persisting the user's auto-logging preference.
 * Only stores a single boolean flag — NEVER stores any SMS data.
 */
interface AutoLoggingStore {
    /** True if the user has enabled auto-logging; false otherwise. */
    var isEnabled: Boolean
}
