package com.plugin.fcm

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.Permission
import app.tauri.annotation.PermissionCallback
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.atomic.AtomicInteger

@InvokeArg
data class CreateChannelArgs(
    val id: String,
    val name: String,
    val importance: Int
)

@InvokeArg
data class SendNotificationArgs(
    val title: String,
    val body: String?,
    val icon: String?,
    val id: Int?,
    val channelId: String?
)

@TauriPlugin(
    permissions = [
        Permission(
            strings = [Manifest.permission.POST_NOTIFICATIONS],
            alias = "notification"
        )
    ]
)
class FcmPlugin(private val activity: Activity) : Plugin(activity) {

    companion object {
        var instance: FcmPlugin? = null
        private const val PREFS_NAME = "fcm_plugin"
        private const val KEY_PERMISSION_REQUESTED = "permission_requested"
    }

    private val notificationIdCounter = AtomicInteger(0)

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
    override fun requestPermissions(invoke: Invoke) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (status == PackageManager.PERMISSION_GRANTED) {
                invoke.resolve(permissionStatus("granted"))
            } else {
                requestPermissionForAlias("notification", invoke, "permissionResultCallback")
            }
        } else {
            val enabled = NotificationManagerCompat.from(activity).areNotificationsEnabled()
            invoke.resolve(permissionStatus(if (enabled) "granted" else "denied"))
        }
    }

    @PermissionCallback
    fun permissionResultCallback(invoke: Invoke) {
        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        // Record that we asked, so checkPermissions can distinguish prompt vs denied.
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

    @Command
    override fun checkPermissions(invoke: Invoke) {
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

    @Command
    fun createChannel(invoke: Invoke) {
        val args = invoke.parseArgs(CreateChannelArgs::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(args.id, args.name, args.importance)
            NotificationManagerCompat.from(activity).createNotificationChannel(channel)
            invoke.resolve()
        } else {
            invoke.resolve()
        }
    }

    private fun ensureDefaultChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = NotificationManagerCompat.from(activity)
            if (manager.getNotificationChannel("default") == null) {
                val channel = NotificationChannel(
                    "default",
                    "Default",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(channel)
            }
        }
    }

    @Command
    fun sendNotification(invoke: Invoke) {
        val args = invoke.parseArgs(SendNotificationArgs::class.java)
        
        val resolvedIcon = if (args.icon != null) {
            val id = activity.resources.getIdentifier(args.icon, "drawable", activity.packageName)
            if (id != 0) id else activity.applicationInfo.icon
        } else {
            activity.applicationInfo.icon
        }
        
        val effectiveChannelId = args.channelId ?: "default"
        if (effectiveChannelId == "default") {
            ensureDefaultChannel()
        }
        
        val builder = NotificationCompat.Builder(activity, effectiveChannelId)
            .setContentTitle(args.title)
            .setSmallIcon(resolvedIcon)
            .setAutoCancel(true)
        
        if (args.body != null) {
            builder.setContentText(args.body)
        }
        
        val notification = builder.build()
        NotificationManagerCompat.from(activity).notify(
            args.id ?: notificationIdCounter.incrementAndGet(),
            notification
        )
        invoke.resolve()
    }
}
