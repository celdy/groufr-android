package com.celdy.groufr.data.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.celdy.groufr.R
import com.celdy.groufr.data.storage.SettingsStore
import com.celdy.groufr.ui.eventdetail.EventDetailActivity
import com.celdy.groufr.ui.notifications.NotificationsActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore
) {
    fun showNotificationsFor(notifications: List<NotificationDto>) {
        if (!canPostNotifications()) return
        if (notifications.isEmpty()) return
        ensureChannel()

        val manager = NotificationManagerCompat.from(context)

        if (notifications.size == 1) {
            val notification = notifications.first()
            val built = buildSingleNotification(notification)
            safeNotify(manager, NOTIFICATION_ID, built)
        } else {
            notifications.forEach { notification ->
                val built = buildGroupedNotification(notification)
                safeNotify(manager, notification.id.toInt(), built)
            }
            val summaryNotification = buildSummaryNotification(notifications)
            safeNotify(manager, SUMMARY_ID, summaryNotification)
        }

        playNotificationSound()
    }

    fun showNotificationFor(notification: NotificationDto) {
        showNotificationsFor(listOf(notification))
    }

    private fun buildSingleNotification(notification: NotificationDto): Notification {
        val actor = notification.actor?.name ?: context.getString(R.string.chat_system_user)
        val title = buildTitle(actor, notification.eventType, notification)
        val contentText = buildContentText(notification)
        val pendingIntent = buildPendingIntentFor(notification)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(resolveIcon(notification.eventType))
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .build()
    }

    private fun buildGroupedNotification(notification: NotificationDto): Notification {
        val actor = notification.actor?.name ?: context.getString(R.string.chat_system_user)
        val title = buildTitle(actor, notification.eventType, notification)
        val contentText = buildContentText(notification)
        val pendingIntent = buildPendingIntentFor(notification)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(resolveIcon(notification.eventType))
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setGroup(GROUP_KEY)
            .build()
    }

    private fun buildSummaryNotification(notifications: List<NotificationDto>): Notification {
        val pendingIntent = buildPendingIntent(SUMMARY_ID)
        val summaryText = context.resources.getQuantityString(
            R.plurals.notification_summary,
            notifications.size,
            notifications.size
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(summaryText)

        notifications.take(MAX_INBOX_LINES).forEach { notification ->
            val actor = notification.actor?.name ?: context.getString(R.string.chat_system_user)
            val line = buildTitle(actor, notification.eventType, notification)
            inboxStyle.addLine(line)
        }

        if (notifications.size > MAX_INBOX_LINES) {
            val moreCount = notifications.size - MAX_INBOX_LINES
            inboxStyle.setSummaryText(
                context.resources.getQuantityString(R.plurals.notification_more, moreCount, moreCount)
            )
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ico_message)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(summaryText)
            .setStyle(inboxStyle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .build()
    }

    private fun buildPendingIntentFor(notification: NotificationDto): PendingIntent {
        val intent = buildIntentFor(notification)
        return PendingIntent.getActivity(
            context,
            notification.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildIntentFor(notification: NotificationDto): Intent {
        if (notification.eventType == "new_message") {
            val eventId = notification.eventIdFromPayload()
            if (eventId != null && eventId > 0) {
                return Intent(context, EventDetailActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId)
                    putExtra(EventDetailActivity.EXTRA_GROUP_NAME, notification.groupName.orEmpty())
                    putExtra(EventDetailActivity.EXTRA_SHOW_CHAT, true)
                }
            }
        }
        if (notification.eventType in EVENT_DETAIL_TYPES) {
            val eventId = notification.eventIdFromPayload() ?: notification.entityId
            if (eventId != null && eventId > 0) {
                return Intent(context, EventDetailActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId)
                    putExtra(EventDetailActivity.EXTRA_GROUP_NAME, notification.groupName.orEmpty())
                }
            }
        }
        return Intent(context, NotificationsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    private fun buildPendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(context, NotificationsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) {
            if (existing.sound != null) {
                manager.deleteNotificationChannel(CHANNEL_ID)
            } else {
                return
            }
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun playNotificationSound() {
        val resId = SettingsStore.getResourceIdForKey(settingsStore.getNotificationSoundKey()) ?: return
        try {
            MediaPlayer.create(context, resId)?.apply {
                setOnCompletionListener { it.release() }
                start()
            }
        } catch (_: Exception) {
            // Ignore playback errors
        }
    }

    private fun safeNotify(
        manager: NotificationManagerCompat,
        notificationId: Int,
        notification: Notification
    ) {
        if (!canPostNotifications()) return
        try {
            manager.notify(notificationId, notification)
        } catch (securityException: SecurityException) {
            // Ignore; user may have revoked notification permission mid-session.
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun buildTitle(actor: String, eventType: String, notification: NotificationDto? = null): String {
        return when (eventType) {
            "event_created" -> context.getString(R.string.notification_title_event_created, actor)
            "event_updated" -> context.getString(R.string.notification_title_event_updated, actor)
            "new_message" -> context.getString(R.string.notification_title_new_message, actor)
            "poll_created" -> context.getString(R.string.notification_title_poll_created, actor)
            "poll_closed" -> context.getString(R.string.notification_title_poll_closed, actor)
            "user_joined" -> context.getString(R.string.notification_title_user_joined, actor)
            "participant_status_changed" -> context.getString(R.string.notification_title_participant_status_changed, actor)
            "invitation_received" -> context.getString(R.string.notification_title_invitation_received, actor)
            "event_invitation_received" -> context.getString(R.string.notification_title_event_invitation_received, actor)
            "event_poll_created" -> context.getString(R.string.notification_title_event_poll_created, actor)
            "event_poll_closed" -> context.getString(R.string.notification_title_event_poll_closed, actor)
            "reaction_message", "reaction_event", "reaction_poll" -> {
                val emoji = extractReactionEmoji(notification)
                val resId = when (eventType) {
                    "reaction_message" -> R.string.notification_title_reaction_message
                    "reaction_event" -> R.string.notification_title_reaction_event
                    else -> R.string.notification_title_reaction_poll
                }
                context.getString(resId, actor, emoji)
            }
            "expense_created" -> context.getString(R.string.notification_title_expense_created, actor)
            "expense_updated" -> context.getString(R.string.notification_title_expense_updated, actor)
            "expense_confirmed" -> context.getString(R.string.notification_title_expense_confirmed, actor)
            "expense_disputed" -> context.getString(R.string.notification_title_expense_disputed, actor)
            "expense_due_reminder" -> context.getString(R.string.notification_title_expense_due_reminder)
            "expense_overdue" -> context.getString(R.string.notification_title_expense_overdue)
            "expense_mediation_needed" -> context.getString(R.string.notification_title_expense_mediation_needed)
            "settlement_payment_created" -> context.getString(R.string.notification_title_settlement_payment_created, actor)
            "settlement_payment_confirmed" -> context.getString(R.string.notification_title_settlement_payment_confirmed, actor)
            "settlement_payment_rejected" -> context.getString(R.string.notification_title_settlement_payment_rejected, actor)
            else -> context.getString(R.string.notification_title_generic, actor, eventType)
        }
    }

    private fun buildContentText(notification: NotificationDto): String {
        val payload = notification.payload
        when (notification.eventType) {
            "new_message" -> {
                val preview = extractStringPayload(payload, "preview")
                if (preview.isNotBlank()) return preview
            }
            "invitation_received" -> {
                val invitedGroupName = notification.invitedGroupNameFromPayload()
                if (!invitedGroupName.isNullOrBlank()) return invitedGroupName
            }
            "event_invitation_received" -> {
                val eventTitle = extractStringPayload(payload, "event_title")
                if (eventTitle.isNotBlank()) return eventTitle
            }
            "event_poll_created", "event_poll_closed" -> {
                val question = extractStringPayload(payload, "question")
                if (question.isNotBlank()) return question
            }
            "reaction_message", "reaction_event", "reaction_poll" -> {
                val preview = extractStringPayload(payload, "preview")
                if (preview.isNotBlank()) return preview
            }
            "expense_created", "expense_updated", "expense_confirmed",
            "expense_disputed", "expense_due_reminder", "expense_overdue",
            "expense_mediation_needed" -> {
                val label = extractStringPayload(payload, "label")
                if (label.isNotBlank()) return label
            }
            "settlement_payment_created", "settlement_payment_confirmed",
            "settlement_payment_rejected" -> {
                val amountCents = payload?.get("amount_cents")
                val currency = extractStringPayload(payload, "currency")
                if (amountCents != null && currency.isNotBlank()) {
                    val amount = when (amountCents) {
                        is Number -> amountCents.toLong()
                        is String -> amountCents.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    return "${amount / 100}.${"%02d".format(amount % 100)} $currency"
                }
            }
        }
        return notification.groupName ?: context.getString(R.string.notification_system_title)
    }

    private fun extractStringPayload(payload: Map<String, Any>?, key: String): String {
        val value = payload?.get(key) ?: return ""
        return value as? String ?: value.toString()
    }

    private fun extractReactionEmoji(notification: NotificationDto?): String {
        val payload = notification?.payload ?: return ""
        val reactions = payload["reactions"] as? List<*> ?: return ""
        return reactions.filterIsInstance<Map<*, *>>()
            .mapNotNull { it["emoji"] as? String }
            .joinToString("")
    }

    private fun resolveIcon(eventType: String): Int {
        return when (eventType) {
            "event_created", "event_updated", "participant_status_changed",
            "event_invitation_received" -> R.drawable.ico_event
            "new_message" -> R.drawable.ico_message
            "poll_created", "poll_closed",
            "event_poll_created", "event_poll_closed" -> R.drawable.ico_poll
            "user_joined", "invitation_received" -> R.drawable.ico_user
            "reaction_message", "reaction_event", "reaction_poll" -> R.drawable.ico_reaction
            "expense_created", "expense_updated", "expense_confirmed",
            "expense_disputed", "expense_due_reminder", "expense_overdue",
            "expense_mediation_needed",
            "settlement_payment_created", "settlement_payment_confirmed",
            "settlement_payment_rejected" -> R.drawable.ico_event
            else -> R.drawable.ico_message
        }
    }

    companion object {
        const val CHANNEL_ID = "groufr_notifications"
        private const val NOTIFICATION_ID = 4201
        private const val SUMMARY_ID = 4200
        private const val GROUP_KEY = "com.celdy.groufr.NOTIFICATION_GROUP"
        private const val MAX_INBOX_LINES = 5
        private val EVENT_DETAIL_TYPES = setOf(
            "event_invitation_received",
            "event_updated",
            "participant_status_changed",
            "reaction_event",
            "expense_created",
            "expense_updated",
            "expense_confirmed",
            "expense_disputed",
            "expense_due_reminder",
            "expense_overdue",
            "expense_mediation_needed"
        )
    }
}
