package com.example.scraply.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.example.scraply.MainActivity
import com.example.scraply.R

/**
 * Central configuration for Scraply's FCM notifications.
 *
 * Channels follow REQUIREMENTS §8 (Social) grouping – we expose a single "Social" channel
 * because all server-driven notifications (likes, comments, follows) share the same
 * importance and behavior. Additional channels can be added here later without touching
 * callers.
 */
object ScraplyNotifications {
    const val CHANNEL_SOCIAL = "scraply_social"
    private const val CHANNEL_SOCIAL_NAME = "Social activity"
    private const val CHANNEL_SOCIAL_DESCRIPTION =
        "Likes, comments and follows on your scrapbooks."

    /** Keys matching the data payload sent by the Cloud Functions trigger. */
    const val EXTRA_TYPE = "scraply_type"
    const val EXTRA_POST_ID = "scraply_post_id"
    const val EXTRA_ROUTE = "scraply_route"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_SOCIAL) != null) return
        val channel = NotificationChannel(
            CHANNEL_SOCIAL,
            context.getString(R.string.notif_channel_social_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_social_desc)
            enableLights(true)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Builds and shows a notification for the given title/body. The tap action opens
     * [MainActivity] and forwards `data` as intent extras so the UI can deep-link to the
     * relevant screen (e.g. open the Profile → Notifications list).
     */
    fun show(
        context: Context,
        title: String?,
        body: String?,
        data: Map<String, String> = emptyMap(),
        notificationId: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
    ) {
        ensureChannels(context)
        val manager = context.getSystemService<NotificationManager>() ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data.forEach { (k, v) -> putExtra(k, v) }
        }
        val pending = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title ?: context.getString(R.string.app_name))
            .setContentText(body ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body ?: ""))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        manager.notify(notificationId, notification)
    }
}
