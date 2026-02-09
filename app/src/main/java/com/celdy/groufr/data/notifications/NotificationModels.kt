package com.celdy.groufr.data.notifications

import com.google.gson.annotations.SerializedName

data class NotificationsResponse(
    val notifications: List<NotificationDto>,
    val meta: NotificationMeta? = null
)

data class NotificationMeta(
    val total: Int,
    @SerializedName("unread_count")
    val unreadCount: Int,
    val limit: Int,
    val offset: Int
)

data class NotificationDto(
    val id: Long,
    @SerializedName("event_type")
    val eventType: String,
    @SerializedName("group_id")
    val groupId: Long?,
    @SerializedName("group_name")
    val groupName: String?,
    val actor: NotificationActorDto?,
    @SerializedName("entity_type")
    val entityType: String?,
    @SerializedName("entity_id")
    val entityId: Long?,
    val payload: Map<String, Any>?,
    @SerializedName("is_read")
    val isRead: Boolean,
    @SerializedName("created_at")
    val createdAt: String
)

data class NotificationActorDto(
    val id: Long,
    val name: String
)

data class MarkReadResponse(
    @SerializedName("marked_count")
    val markedCount: Int
)

data class NotificationMarkReadRequest(
    val type: String? = null,
    val scope: String? = null,
    @SerializedName("group_id")
    val groupId: Long? = null,
    @SerializedName("event_id")
    val eventId: Long? = null,
    @SerializedName("poll_id")
    val pollId: Long? = null,
    @SerializedName("invitation_id")
    val invitationId: Long? = null,
    @SerializedName("user_id")
    val userId: Long? = null
)

data class NotificationCountResponse(
    @SerializedName("unread_count")
    val unreadCount: Int,
    @SerializedName("by_event")
    val byEvent: Map<String, Int>? = null
)
