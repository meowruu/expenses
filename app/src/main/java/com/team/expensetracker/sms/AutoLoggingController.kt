package com.team.expensetracker.sms

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI-free state holder and controller for the privacy-first SMS auto-logging flow.
 *
 * Guarantees:
 *  1. Explains what is read and why FIRST ([AutoLoggingState.ShowingExplainer]).
 *  2. Requests system permission only after user accepts the explainer ([AutoLoggingState.RequestingPermission]).
 *  3. Enables the receiver component ONLY if the user grants the permission ([AutoLoggingState.On]).
 *  4. Keeps receiver disabled and state [AutoLoggingState.Off] or [AutoLoggingState.PermissionDenied] otherwise.
 *
 * F1 UI observes [state] to drive the UI.
 */
class AutoLoggingController(
    private val permissionChecker: SmsPermissionChecker,
    private val receiverSwitch: ReceiverSwitch,
    private val store: AutoLoggingStore
) {

    private val _state = MutableStateFlow<AutoLoggingState>(
        if (store.isEnabled && permissionChecker.isGranted()) {
            AutoLoggingState.On
        } else {
            if (store.isEnabled) {
                // Self-heal: stored state was On, but system permission was revoked
                store.isEnabled = false
                receiverSwitch.setEnabled(false)
            }
            AutoLoggingState.Off
        }
    )

    /** Observable state of auto-logging to bind with Jetpack Compose. */
    val state: StateFlow<AutoLoggingState> = _state.asStateFlow()

    /**
     * User requested to turn auto-logging on (e.g. tapped switch).
     * If permission is already granted, enables immediately.
     * Otherwise, displays privacy explainer first.
     */
    fun onEnableRequested() {
        if (permissionChecker.isGranted()) {
            receiverSwitch.setEnabled(true)
            store.isEnabled = true
            _state.value = AutoLoggingState.On
        } else {
            _state.value = AutoLoggingState.ShowingExplainer
        }
    }

    /**
     * User read and accepted the privacy explainer dialog.
     * Transitions to [AutoLoggingState.RequestingPermission] so F1 can launch the system permission prompt.
     */
    fun onExplainerAccepted() {
        _state.value = AutoLoggingState.RequestingPermission
    }

    /**
     * User dismissed or cancelled the privacy explainer dialog.
     * Reverts to [AutoLoggingState.Off].
     */
    fun onExplainerDismissed() {
        _state.value = AutoLoggingState.Off
    }

    /**
     * Callback invoked after F1 receives the system permission prompt result.
     *
     * @param granted True if the user granted RECEIVE_SMS; false if denied.
     */
    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            receiverSwitch.setEnabled(true)
            store.isEnabled = true
            _state.value = AutoLoggingState.On
        } else {
            receiverSwitch.setEnabled(false)
            store.isEnabled = false
            _state.value = AutoLoggingState.PermissionDenied
        }
    }

    /**
     * Disables auto-logging immediately, turning off the receiver and clearing the flag.
     */
    fun disable() {
        receiverSwitch.setEnabled(false)
        store.isEnabled = false
        _state.value = AutoLoggingState.Off
    }

    /**
     * Re-syncs internal state with actual system permission status.
     * Call this in Activity onResume() to handle cases where the user revoked permissions
     * outside the app in system settings.
     */
    fun refresh() {
        if (store.isEnabled) {
            if (!permissionChecker.isGranted()) {
                receiverSwitch.setEnabled(false)
                store.isEnabled = false
                _state.value = AutoLoggingState.Off
            } else {
                receiverSwitch.setEnabled(true)
                _state.value = AutoLoggingState.On
            }
        } else {
            if (_state.value is AutoLoggingState.On) {
                receiverSwitch.setEnabled(false)
                _state.value = AutoLoggingState.Off
            }
        }
    }

    companion object {
        /**
         * Factory function to create a ready-to-use controller for Android runtime.
         */
        fun create(context: Context): AutoLoggingController = createAutoLoggingController(context)
    }
}
