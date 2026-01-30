package com.celdy.groufr.data.messages

import com.celdy.groufr.data.local.MessageDao
import com.celdy.groufr.data.local.UserDao
import com.celdy.groufr.data.local.toDto
import com.celdy.groufr.data.local.toEntity
import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class MessagesRepository @Inject constructor(
    private val apiService: ApiService,
    private val messageDao: MessageDao,
    private val userDao: UserDao
) {
    suspend fun loadMessages(groupId: Long, beforeId: Long? = null): MessagesResponse {
        return try {
            val response = apiService.getGroupMessages(groupId, beforeId = beforeId)
            val users = response.messages.flatMap { message ->
                listOfNotNull(message.user, message.refUser)
            }.map { it.toEntity() }
            if (users.isNotEmpty()) {
                userDao.upsertAll(users)
            }
            val entities = response.messages.map { it.toEntity(threadType = "group", threadId = groupId) }
            messageDao.upsertAll(entities)
            response
        } catch (exception: Exception) {
            val cached = messageDao.getByThread(threadType = "group", threadId = groupId)
            if (cached.isNotEmpty()) {
                val userIds = cached.flatMap { entity ->
                    listOfNotNull(entity.userId, entity.refUserId)
                }.distinct()
                val userMap = if (userIds.isEmpty()) {
                    emptyMap()
                } else {
                    userDao.getByIds(userIds).associateBy { it.id }
                }
                val messages = cached.map { it.toDto(userMap) }
                MessagesResponse(
                    messages = messages,
                    meta = MessagesMeta(
                        hasMore = false,
                        oldestId = messages.firstOrNull()?.id
                    )
                )
            } else {
                throw exception
            }
        }
    }

    suspend fun loadEventMessages(eventId: Long, beforeId: Long? = null): MessagesResponse {
        return try {
            val response = apiService.getEventMessages(eventId, beforeId = beforeId)
            val users = response.messages.flatMap { message ->
                listOfNotNull(message.user, message.refUser)
            }.map { it.toEntity() }
            if (users.isNotEmpty()) {
                userDao.upsertAll(users)
            }
            val entities = response.messages.map { it.toEntity(threadType = "event", threadId = eventId) }
            messageDao.upsertAll(entities)
            response
        } catch (exception: Exception) {
            val cached = messageDao.getByThread(threadType = "event", threadId = eventId)
            if (cached.isNotEmpty()) {
                val userIds = cached.flatMap { entity ->
                    listOfNotNull(entity.userId, entity.refUserId)
                }.distinct()
                val userMap = if (userIds.isEmpty()) {
                    emptyMap()
                } else {
                    userDao.getByIds(userIds).associateBy { it.id }
                }
                val messages = cached.map { it.toDto(userMap) }
                MessagesResponse(
                    messages = messages,
                    meta = MessagesMeta(
                        hasMore = false,
                        oldestId = messages.firstOrNull()?.id
                    )
                )
            } else {
                throw exception
            }
        }
    }

    suspend fun sendMessage(groupId: Long, body: String): MessageDto {
        val message = apiService.sendGroupMessage(groupId, SendMessageRequest(body = body))
        val users = listOfNotNull(message.user, message.refUser).map { it.toEntity() }
        if (users.isNotEmpty()) {
            userDao.upsertAll(users)
        }
        messageDao.upsert(message.toEntity(threadType = "group", threadId = groupId))
        return message
    }

    suspend fun sendEventMessage(eventId: Long, body: String): MessageDto {
        val message = apiService.sendEventMessage(eventId, SendMessageRequest(body = body))
        val users = listOfNotNull(message.user, message.refUser).map { it.toEntity() }
        if (users.isNotEmpty()) {
            userDao.upsertAll(users)
        }
        messageDao.upsert(message.toEntity(threadType = "event", threadId = eventId))
        return message
    }
}
