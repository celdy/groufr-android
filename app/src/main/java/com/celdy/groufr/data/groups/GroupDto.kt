package com.celdy.groufr.data.groups

import com.google.gson.annotations.SerializedName

data class GroupDto(
    val id: Long,
    val slug: String,
    val name: String,
    val description: String?,
    @SerializedName("your_role")
    val yourRole: String? = null,
    @SerializedName("billing_status")
    val billingStatus: String? = null,
    @SerializedName("paid_until")
    val paidUntil: String? = null,
    val plan: GroupPlanDto? = null,
    @SerializedName("member_count")
    val memberCount: Int = 0,
    @SerializedName("last_activity_at")
    val lastActivityAt: String?,
    @SerializedName("unread_count")
    val unreadCount: Int = 0,
    @SerializedName("created_at")
    val createdAt: String? = null
)

data class GroupPlanDto(
    val name: String,
    @SerializedName("member_limit")
    val memberLimit: Int
)
