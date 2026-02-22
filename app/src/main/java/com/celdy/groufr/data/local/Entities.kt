package com.celdy.groufr.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.celdy.groufr.data.events.EventParticipantDto
import com.celdy.groufr.data.expenses.ExpenseShareDto
import com.celdy.groufr.data.messages.MessageEventRef
import com.celdy.groufr.data.messages.MessagePollRef
import com.celdy.groufr.data.polls.PollOptionDto

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: Long,
    val slug: String,
    val name: String,
    val description: String?,
    val lastActivityAt: String?,
    val unreadCount: Int = 0
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val email: String?
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: Long,
    val groupId: Long,
    val groupName: String?,
    val groupSlug: String?,
    val title: String,
    val description: String?,
    val place: String?,
    val state: String,
    val startAt: String?,
    val endAt: String?,
    val deadlineJoinAt: String?,
    val minParticipants: Int?,
    val maxParticipants: Int?,
    val yourStatus: String,
    val yourRole: String?,
    val participants: Map<String, Int>,
    val participantsList: List<EventParticipantDto>,
    val createdAt: String,
    val updatedAt: String?
)

@Entity(tableName = "polls")
data class PollEntity(
    @PrimaryKey val id: Long,
    val groupId: Long,
    val question: String,
    val description: String?,
    val multiselect: Boolean,
    val options: List<PollOptionDto>,
    val yourVotes: List<Long>,
    val totalVoters: Int,
    val status: String,
    val deadlineAt: String?,
    val createdAt: String
)

@Entity(
    tableName = "messages",
    primaryKeys = ["id", "threadType", "threadId"]
)
data class MessageEntity(
    val id: Long,
    val threadType: String,
    val threadId: Long,
    val userId: Long?,
    val messageType: String,
    val body: String?,
    val createdAt: String,
    val refUserId: Long?,
    val refEvent: MessageEventRef?,
    val refPoll: MessagePollRef?
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: Long,
    val eventId: Long,
    val payerId: Long,
    val payerName: String,
    val createdById: Long?,
    val createdByName: String?,
    val label: String,
    val amountCents: Long,
    val currency: String,
    val splitType: String,
    val status: String,
    val shares: List<ExpenseShareDto>,
    val createdAt: String
)

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val id: Long,
    val groupId: Long,
    val payerId: Long,
    val payerName: String,
    val recipientId: Long,
    val recipientName: String,
    val amountCents: Long,
    val currency: String,
    val note: String?,
    val status: String,
    val createdAt: String,
    val confirmedAt: String?,
    val rejectedAt: String?
)
