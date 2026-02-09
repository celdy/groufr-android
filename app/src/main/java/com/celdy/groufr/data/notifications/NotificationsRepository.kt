package com.celdy.groufr.data.notifications

import com.celdy.groufr.data.local.UserDao
import com.celdy.groufr.data.local.toEntity
import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class NotificationsRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao
) {
    suspend fun loadNotifications(
        unreadOnly: Boolean = false,
        limit: Int = 50,
        offset: Int = 0
    ): List<NotificationDto> {
        val response = apiService.getNotifications(
            limit = limit,
            offset = offset,
            unreadOnly = unreadOnly
        )
        val users = response.notifications.mapNotNull { it.actor?.toEntity() }
        if (users.isNotEmpty()) {
            userDao.upsertAll(users)
        }
        return response.notifications
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

    suspend fun markGroupChatRead(groupId: Long): Int {
        val response = apiService.markNotificationsRead(
            NotificationMarkReadRequest(scope = "group_chat", groupId = groupId)
        )
        return response.markedCount
    }

    suspend fun markEventDetailRead(eventId: Long): Int {
        val response = apiService.markNotificationsRead(
            NotificationMarkReadRequest(scope = "event_detail", eventId = eventId)
        )
        return response.markedCount
    }

    suspend fun markPollDetailRead(pollId: Long): Int {
        val response = apiService.markNotificationsRead(
            NotificationMarkReadRequest(scope = "poll_detail", pollId = pollId)
        )
        return response.markedCount
    }

    suspend fun acceptInvitation(token: String) {
        apiService.acceptInvitation(token)
    }

    suspend fun declineInvitation(token: String) {
        apiService.declineInvitation(token)
    }
}
