package com.celdy.groufr.data.notifications

import com.celdy.groufr.data.device.PushTokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GroufrMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushTokenManager: PushTokenManager
    @Inject lateinit var notificationNotifier: NotificationNotifier

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch {
            pushTokenManager.onNewToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data.isEmpty()) return

        val notification = parseNotification(data) ?: return
        notificationNotifier.showNotificationFor(notification)
    }

    private fun parseNotification(data: Map<String, String>): NotificationDto? {
        val eventType = data["event_type"] ?: return null

        val payload = mutableMapOf<String, Any>()
        data["preview"]?.let { payload["preview"] = it }
        data["event_id"]?.let { payload["event_id"] = it }
        data["group_name"]?.let { payload["group_name"] = it }
        data["invitation_token"]?.let { payload["invitation_token"] = it }

        return NotificationDto(
            id = data["notification_id"]?.toLongOrNull() ?: System.currentTimeMillis(),
            eventType = eventType,
            groupId = data["group_id"]?.toLongOrNull(),
            groupName = data["group_name"],
            actor = data["actor_name"]?.let { name ->
                NotificationActorDto(
                    id = data["actor_id"]?.toLongOrNull() ?: 0L,
                    name = name
                )
            },
            entityType = data["entity_type"],
            entityId = data["entity_id"]?.toLongOrNull(),
            payload = payload.ifEmpty { null },
            isRead = false,
            createdAt = data["created_at"] ?: ""
        )
    }
}
