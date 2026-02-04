package com.celdy.groufr.data.groups

import com.google.gson.annotations.SerializedName

data class GroupDto(
    val id: Long,
    val slug: String,
    val name: String,
    val description: String?,
    @SerializedName("last_activity_at")
    val lastActivityAt: String?,
    @SerializedName("unread_count")
    val unreadCount: Int = 0
)
