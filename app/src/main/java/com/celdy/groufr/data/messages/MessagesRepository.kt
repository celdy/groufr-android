package com.celdy.groufr.data.messages

import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class MessagesRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun loadMessages(groupId: Long, beforeId: Long? = null): MessagesResponse {
        return apiService.getGroupMessages(groupId, beforeId = beforeId)
    }

    suspend fun loadEventMessages(eventId: Long, beforeId: Long? = null): MessagesResponse {
        return apiService.getEventMessages(eventId, beforeId = beforeId)
    }

    suspend fun sendMessage(groupId: Long, body: String): MessageDto {
        return apiService.sendGroupMessage(groupId, SendMessageRequest(body = body))
    }

    suspend fun sendEventMessage(eventId: Long, body: String): MessageDto {
        return apiService.sendEventMessage(eventId, SendMessageRequest(body = body))
    }
}
