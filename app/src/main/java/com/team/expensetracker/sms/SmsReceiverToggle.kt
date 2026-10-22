package com.team.expensetracker.sms

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Utility to dynamically enable or disable [SmsReceiver] at runtime.
 *
 * Used by F1's UI toggle ("Auto-logging on / off").
 * The receiver starts disabled in AndroidManifest.xml (android:enabled="false")
 * and is enabled only when the user explicitly grants permission and activates auto-logging.
 */
object SmsReceiverToggle {

    /**
     * Enables or disables [SmsReceiver] component using [PackageManager.DONT_KILL_APP].
     *
     * @param context Android context
     * @param enabled true to enable SMS interception; false to disable
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        val receiver = ComponentName(context, SmsReceiver::class.java)
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        context.packageManager.setComponentEnabledSetting(
            receiver,
            newState,
            PackageManager.DONT_KILL_APP
        )
    }

    /**
     * Checks whether [SmsReceiver] is currently enabled.
     *
     * @param context Android context
     * @return true if explicitly enabled; false if disabled or default
     */
    fun isEnabled(context: Context): Boolean {
        val receiver = ComponentName(context, SmsReceiver::class.java)
        val state = context.packageManager.getComponentEnabledSetting(receiver)
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }
}
