package com.celdy.groufr.data.messages

import com.google.gson.annotations.SerializedName

data class MessagesResponse(
    val messages: List<MessageDto>,
    val meta: MessagesMeta? = null
)

data class MessagesMeta(
    @SerializedName("has_more")
    val hasMore: Boolean,
    @SerializedName("oldest_id")
    val oldestId: Long?,
    @SerializedName("last_seen_message_id")
    val lastSeenMessageId: Long? = null
)

data class MessageDto(
    val id: Long,
    val user: MessageUserRef?,
    @SerializedName("message_type")
    val messageType: String,
    val body: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("ref_user")
    val refUser: MessageUserRef? = null,
    @SerializedName("ref_event")
    val refEvent: MessageEventRef? = null,
    @SerializedName("ref_poll")
    val refPoll: MessagePollRef? = null,
    val reactions: MessageReactions? = null
)

data class MessageReactions(
    @SerializedName("total_count")
    val totalCount: Int = 0,
    @SerializedName("top_reactions")
    val topReactions: List<MessageTopReaction> = emptyList(),
    @SerializedName("user_reaction")
    val userReaction: String? = null
)

data class MessageTopReaction(
    val type: String,
    val emoji: String,
    val count: Int
)

data class MessageUserRef(
    val id: Long,
    val name: String
)

data class MessageEventRef(
    val id: Long,
    val title: String?
)

data class MessagePollRef(
    val id: Long,
    val question: String?,
    @SerializedName("deadline_at")
    val deadlineAt: String? = null
)

data class SendMessageRequest(
    val body: String
)
