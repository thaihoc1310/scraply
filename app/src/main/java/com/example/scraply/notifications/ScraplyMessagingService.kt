package com.example.scraply.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives pushes from Firebase Cloud Messaging.
 *
 * Two incoming shapes are handled:
 *  - `notification` payload: Firebase auto-displays the notification when the app is in the
 *    background. We only surface it ourselves when the app is in the foreground (Android's
 *    default behaviour would otherwise drop the notification silently).
 *  - `data` payload: always delivered to [onMessageReceived]. We build the notification from
 *    `title`/`body` fields and forward other entries as intent extras so tapping the
 *    notification deep-links to the right route (e.g. the notifications list).
 */
class ScraplyMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken length=${token.length}")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch {
            runCatching { NotificationTokenManager.saveToken(uid, token) }
                .onFailure { Log.w(TAG, "Failed to save refreshed token", it) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = message.notification?.title ?: data["title"]
        val body = message.notification?.body ?: data["body"]
        Log.d(TAG, "onMessageReceived title=$title body=$body dataKeys=${data.keys}")

        val extras = buildMap {
            data["type"]?.let { put(ScraplyNotifications.EXTRA_TYPE, it) }
            data["postId"]?.let { put(ScraplyNotifications.EXTRA_POST_ID, it) }
            data["route"]?.let { put(ScraplyNotifications.EXTRA_ROUTE, it) }
        }
        ScraplyNotifications.show(
            context = applicationContext,
            title = title,
            body = body,
            data = extras,
        )
    }

    companion object {
        private const val TAG = "ScraplyMessaging"
    }
}
