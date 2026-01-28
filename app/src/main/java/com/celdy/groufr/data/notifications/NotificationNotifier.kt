package com.celdy.groufr.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.celdy.groufr.R
import com.celdy.groufr.ui.notifications.NotificationsActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun showNotificationFor(notification: NotificationDto) {
        if (!canPostNotifications()) return
        ensureChannel()
        val actor = notification.actor?.name ?: context.getString(R.string.chat_system_user)
        val title = buildTitle(actor, notification.eventType)
        val contentText = buildContentText(notification)
        val intent = Intent(context, NotificationsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(resolveIcon(notification.eventType))
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun buildTitle(actor: String, eventType: String): String {
        val resId = when (eventType) {
            "event_created" -> R.string.notification_title_event_created
            "event_updated" -> R.string.notification_title_event_updated
            "new_message" -> R.string.notification_title_new_message
            "poll_created" -> R.string.notification_title_poll_created
            "poll_closed" -> R.string.notification_title_poll_closed
            "user_joined" -> R.string.notification_title_user_joined
            "participant_status_changed" -> R.string.notification_title_participant_status_changed
            else -> R.string.notification_title_generic
        }

        return if (resId == R.string.notification_title_generic) {
            context.getString(resId, actor, eventType)
        } else {
            context.getString(resId, actor)
        }
    }

    private fun buildContentText(notification: NotificationDto): String {
        val groupName = notification.groupName
        if (notification.eventType == "new_message") {
            val preview = extractPreview(notification)
            if (preview.isNotBlank()) return preview
        }
        return groupName ?: context.getString(R.string.notification_system_title)
    }

    private fun extractPreview(notification: NotificationDto): String {
        val payload = notification.payload ?: return ""
        val preview = payload["preview"] ?: return ""
        return preview as? String ?: preview.toString()
    }

    private fun resolveIcon(eventType: String): Int {
        return when (eventType) {
            "event_created", "event_updated", "participant_status_changed" -> R.drawable.ico_event
            "new_message" -> R.drawable.ico_message
            "poll_created", "poll_closed" -> R.drawable.ico_poll
            "user_joined" -> R.drawable.ico_user
            else -> R.drawable.ico_message
        }
    }

    companion object {
        const val CHANNEL_ID = "groufr_notifications"
        private const val NOTIFICATION_ID = 4201
    }
}
