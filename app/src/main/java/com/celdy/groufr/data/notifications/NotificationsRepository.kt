package com.celdy.groufr.data.notifications

import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class NotificationsRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun loadNotifications(
        unreadOnly: Boolean = false,
        limit: Int = 50,
        offset: Int = 0
    ): List<NotificationDto> {
        return apiService.getNotifications(
            limit = limit,
            offset = offset,
            unreadOnly = unreadOnly
        ).notifications
    }

    suspend fun loadUnreadCount(): Int {
        return apiService.getNotificationCount().unreadCount
    }

    suspend fun loadUnreadEventCount(eventId: Long): Int {
        val response = apiService.getNotificationCount()
        return response.byEvent?.get(eventId.toString()) ?: 0
    }

    suspend fun markRead(notificationId: Long) {
        apiService.markNotificationRead(notificationId)
    }

    suspend fun markAllRead() {
        apiService.markAllNotificationsRead()
    }

    suspend fun markEventMessagesRead(eventId: Long): Int {
        val response = apiService.markNotificationsRead(
            NotificationMarkReadRequest(
                type = "event_messages",
                eventId = eventId
            )
        )
        return response.markedCount
    }
}
