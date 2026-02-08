package com.celdy.groufr.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface GroupDao {
    @Query("SELECT * FROM `groups` ORDER BY name")
    suspend fun getAll(): List<GroupEntity>

    @Upsert
    suspend fun upsertAll(groups: List<GroupEntity>)

    @Query("DELETE FROM `groups`")
    suspend fun clear()

    @Query("DELETE FROM `groups` WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<Long>)

    @Transaction
    suspend fun replaceAll(groups: List<GroupEntity>) {
        if (groups.isEmpty()) {
            clear()
            return
        }
        upsertAll(groups)
        deleteNotIn(groups.map { it.id })
    }
}

@Dao
interface UserDao {
    @Upsert
    suspend fun upsertAll(users: List<UserEntity>)

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM users WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE groupId = :groupId ORDER BY startAt")
    suspend fun getByGroup(groupId: Long): List<EventEntity>

    @Query("SELECT * FROM events ORDER BY startAt")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    suspend fun getById(eventId: Long): EventEntity?

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Upsert
    suspend fun upsert(event: EventEntity)

    @Query("DELETE FROM events")
    suspend fun clear()

    @Query("DELETE FROM events WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<Long>)

    @Query("DELETE FROM events WHERE groupId = :groupId AND id NOT IN (:ids)")
    suspend fun deleteByGroupNotIn(groupId: Long, ids: List<Long>)

    @Query("DELETE FROM events WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: Long)

    @Transaction
    suspend fun replaceAll(events: List<EventEntity>) {
        if (events.isEmpty()) {
            clear()
            return
        }
        upsertAll(events)
        deleteNotIn(events.map { it.id })
    }

    @Transaction
    suspend fun replaceAllByGroup(groupId: Long, events: List<EventEntity>) {
        if (events.isEmpty()) {
            deleteByGroup(groupId)
            return
        }
        upsertAll(events)
        deleteByGroupNotIn(groupId, events.map { it.id })
    }
}

@Dao
interface PollDao {
    @Query("SELECT * FROM polls WHERE groupId = :groupId ORDER BY createdAt DESC")
    suspend fun getByGroup(groupId: Long): List<PollEntity>

    @Query("SELECT * FROM polls WHERE groupId = :groupId AND status = :status ORDER BY createdAt DESC")
    suspend fun getByGroupAndStatus(groupId: Long, status: String): List<PollEntity>

    @Query("SELECT * FROM polls WHERE id = :pollId LIMIT 1")
    suspend fun getById(pollId: Long): PollEntity?

    @Upsert
    suspend fun upsertAll(polls: List<PollEntity>)

    @Upsert
    suspend fun upsert(poll: PollEntity)
}

@Dao
interface MessageDao {
    @Query(
        "SELECT * FROM messages " +
            "WHERE threadType = :threadType AND threadId = :threadId " +
            "ORDER BY id"
    )
    suspend fun getByThread(threadType: String, threadId: Long): List<MessageEntity>

    @Upsert
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Upsert
    suspend fun upsert(message: MessageEntity)
}
