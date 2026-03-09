package com.plugin.fcm

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.tauri.annotation.Command
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import com.google.firebase.messaging.FirebaseMessaging

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

    private fun permissionStatus(value: String): JSObject {
        val ret = JSObject()
        ret.put("notification", value)
        return ret
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
    fun requestPermissions(invoke: Invoke) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (status == PackageManager.PERMISSION_GRANTED) {
                invoke.resolve(permissionStatus("granted"))
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
            val enabled = NotificationManagerCompat.from(activity).areNotificationsEnabled()
            invoke.resolve(permissionStatus(if (enabled) "granted" else "denied"))
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
            // made a choice, so report prompt and don't set the flag.
            if (grantResults.isEmpty()) {
                invoke.resolve(permissionStatus("prompt"))
                return
            }

            val granted = grantResults[0] == PackageManager.PERMISSION_GRANTED

            // Only record after the OS returned a definitive answer.
            activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
                .edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply()

            val deniedState =
                if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    "prompt-with-rationale"
                } else {
                    "denied"
                }

            invoke.resolve(permissionStatus(if (granted) "granted" else deniedState))
        }
    }

    @Command
    fun checkPermissions(invoke: Invoke) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (status == PackageManager.PERMISSION_GRANTED) {
                invoke.resolve(permissionStatus("granted"))
            } else {
                val prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
                val everRequested = prefs.getBoolean(KEY_PERMISSION_REQUESTED, false)
                val deniedState =
                    if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        "prompt-with-rationale"
                    } else {
                        "denied"
                    }
                invoke.resolve(permissionStatus(if (everRequested) deniedState else "prompt"))
            }
        } else {
            val enabled = NotificationManagerCompat.from(activity).areNotificationsEnabled()
            invoke.resolve(permissionStatus(if (enabled) "granted" else "denied"))
        }
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
