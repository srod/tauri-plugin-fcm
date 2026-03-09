package com.plugin.fcm

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import com.google.firebase.messaging.FirebaseMessaging

@InvokeArg
class PermissionOptions {
    var sound: Boolean? = null
    var badge: Boolean? = null
    var alert: Boolean? = null
}

@TauriPlugin
class FcmPlugin(private val activity: Activity) : Plugin(activity) {

    companion object {
        var instance: FcmPlugin? = null
        private const val FCM_PERMISSION_REQUEST_CODE = 1001
        private const val PREFS_NAME = "fcm_plugin"
        private const val KEY_PERMISSION_REQUESTED = "permission_requested"
    }

    private var pendingPermissionInvoke: Invoke? = null

    override fun load(webView: WebView) {
        super.load(webView)
        instance = this
    }

    @Command
    fun getToken(invoke: Invoke) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val ret = JSObject()
                ret.put("token", task.result)
                invoke.resolve(ret)
            } else {
                // Fallback: check SharedPreferences for token buffered during cold start
                val prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
                val buffered = prefs.getString("fcm_token", null)
                if (buffered != null) {
                    prefs.edit().remove("fcm_token").apply()
                    val ret = JSObject()
                    ret.put("token", buffered)
                    invoke.resolve(ret)
                } else {
                    invoke.reject(task.exception?.message ?: "Failed to get FCM token")
                }
            }
        }
    }

    @Command
    fun requestPermission(invoke: Invoke) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (status == PackageManager.PERMISSION_GRANTED) {
                val ret = JSObject()
                ret.put("status", "granted")
                invoke.resolve(ret)
            } else if (pendingPermissionInvoke != null) {
                // A permission dialog is already showing — reject this call
                // rather than starting a second platform request on the same
                // request code, which would orphan or mis-route callbacks.
                invoke.reject("A permission request is already in progress")
            } else {
                activity.requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    FCM_PERMISSION_REQUEST_CODE
                )
                pendingPermissionInvoke = invoke
            }
        } else {
            // Below Android 13: notifications auto-granted
            val ret = JSObject()
            ret.put("status", "granted")
            invoke.resolve(ret)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == FCM_PERMISSION_REQUEST_CODE) {
            val invoke = pendingPermissionInvoke ?: return
            pendingPermissionInvoke = null

            // Empty grantResults means the request was canceled/interrupted
            // (e.g. another activity was launched mid-dialog). The user never
            // made a choice, so report not_determined and don't set the flag.
            if (grantResults.isEmpty()) {
                val ret = JSObject()
                ret.put("status", "not_determined")
                invoke.resolve(ret)
                return
            }

            val granted = grantResults[0] == PackageManager.PERMISSION_GRANTED

            // Only record after the OS returned a definitive answer.
            activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
                .edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply()

            val ret = JSObject()
            ret.put("status", if (granted) "granted" else "denied")
            invoke.resolve(ret)
        }
    }

    @Command
    fun checkPermissions(invoke: Invoke) {
        val ret = JSObject()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (status == PackageManager.PERMISSION_GRANTED) {
                ret.put("status", "granted")
            } else {
                val prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
                val everRequested = prefs.getBoolean(KEY_PERMISSION_REQUESTED, false)
                ret.put("status", if (everRequested) "denied" else "not_determined")
            }
        } else {
            val enabled = NotificationManagerCompat.from(activity).areNotificationsEnabled()
            ret.put("status", if (enabled) "granted" else "denied")
        }
        invoke.resolve(ret)
    }

    @Command
    fun register(invoke: Invoke) {
        // No-op on Android — FCM auto-registers
        invoke.resolve()
    }

    @Command
    fun deleteToken(invoke: Invoke) {
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                invoke.resolve()
            } else {
                invoke.reject(task.exception?.message ?: "Failed to delete FCM token")
            }
        }
    }
}
