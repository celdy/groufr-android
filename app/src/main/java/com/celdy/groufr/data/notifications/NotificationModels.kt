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
    val text: String? = null,
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
    @SerializedName("message_id")
    val messageId: Long? = null,
    @SerializedName("invitation_id")
    val invitationId: Long? = null,
    @SerializedName("user_id")
    val userId: Long? = null
)

data class NotificationCountResponse(
    @SerializedName("unread_count")
    val unreadCount: Int,
    @SerializedName("by_group")
    val byGroup: Map<String, GroupNotificationCount>? = null,
    @SerializedName("by_event")
    val byEvent: Map<String, Int>? = null,
    val invitations: Int = 0,
    val timestamp: String? = null
)

data class GroupNotificationCount(
    val total: Int = 0,
    val messages: Int = 0,
    val events: Int = 0,
    val polls: Int = 0,
    val reactions: Int = 0,
    val users: Int = 0
)

data class NotificationPreferencesResponse(
    val global: Map<String, NotificationPreference>,
    @SerializedName("by_group")
    val byGroup: Map<String, Map<String, NotificationPreference>>? = null
)

data class NotificationPreference(
    @SerializedName("email_enabled")
    val emailEnabled: Boolean,
    @SerializedName("push_enabled")
    val pushEnabled: Boolean,
    @SerializedName("in_app_enabled")
    val inAppEnabled: Boolean,
    val digest: String? = null
)

data class UpdateNotificationPreferenceRequest(
    @SerializedName("event_type")
    val eventType: String,
    @SerializedName("group_id")
    val groupId: Long? = null,
    @SerializedName("email_enabled")
    val emailEnabled: Boolean? = null,
    @SerializedName("push_enabled")
    val pushEnabled: Boolean? = null,
    @SerializedName("in_app_enabled")
    val inAppEnabled: Boolean? = null,
    val digest: String? = null
)
