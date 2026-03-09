package com.plugin.fcm

import android.content.Context
import android.util.Log
import app.tauri.plugin.JSObject
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val plugin = FcmPlugin.instance
        if (plugin != null) {
            plugin.trigger("token-refresh", JSObject().put("token", token))
        } else {
            // Cold start: buffer token in SharedPreferences for later retrieval
            getSharedPreferences("fcm_plugin", Context.MODE_PRIVATE)
                .edit().putString("fcm_token", token).apply()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Token-only plugin — no display handling
        Log.d("FcmPlugin", "Message received from: ${message.from}")
    }
}
