package com.team.expensetracker.sms

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Android framework implementation of [SmsPermissionChecker] using [ContextCompat].
 */
class AndroidSmsPermissionChecker(private val context: Context) : SmsPermissionChecker {
    override fun isGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Android framework implementation of [ReceiverSwitch] delegating to [SmsReceiverToggle].
 */
class AndroidReceiverSwitch(private val context: Context) : ReceiverSwitch {
    override fun setEnabled(enabled: Boolean) {
        SmsReceiverToggle.setEnabled(context, enabled)
    }
}

/**
 * Android framework implementation of [AutoLoggingStore] backed by [SharedPreferences].
 * Only stores a single boolean preference for auto-logging enabled state. Zero SMS content.
 */
class SharedPreferencesAutoLoggingStore(
    context: Context,
    prefsName: String = "spendlog_auto_logging_prefs"
) : AutoLoggingStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOGGING_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_LOGGING_ENABLED, value).apply()
        }

    companion object {
        const val KEY_AUTO_LOGGING_ENABLED = "key_auto_logging_enabled"
    }
}

/**
 * Top-level factory function providing a configured [AutoLoggingController] for F1.
 */
fun createAutoLoggingController(context: Context): AutoLoggingController {
    val appContext = context.applicationContext
    return AutoLoggingController(
        permissionChecker = AndroidSmsPermissionChecker(appContext),
        receiverSwitch = AndroidReceiverSwitch(appContext),
        store = SharedPreferencesAutoLoggingStore(appContext)
    )
}
