package com.celdy.groufr.data.groups

import com.google.gson.annotations.SerializedName

data class GroupDetailDto(
    val id: Long,
    val slug: String,
    val name: String,
    val description: String?,
    val locale: String? = null,
    @SerializedName("your_role")
    val yourRole: String?,
    @SerializedName("billing_status")
    val billingStatus: String? = null,
    @SerializedName("paid_until")
    val paidUntil: String? = null,
    val plan: GroupPlanDto? = null,
    @SerializedName("digest_frequency")
    val digestFrequency: String? = null,
    @SerializedName("member_count")
    val memberCount: Int,
    @SerializedName("chat_id")
    val chatId: Long? = null,
    @SerializedName("last_activity_at")
    val lastActivityAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String?,
    val owner: GroupOwnerDto?
)

data class GroupOwnerDto(
    val id: Long,
    val name: String
)

data class GroupMembersResponse(
    val members: List<GroupMemberDto>
)

data class GroupMemberDto(
    val user: GroupMemberUserDto,
    val role: String,
    @SerializedName("joined_at")
    val joinedAt: String?
)

data class GroupMemberUserDto(
    val id: Long,
    val name: String,
    val email: String?
)

data class GroupActionResponse(
    val message: String?
)

data class GroupsListResponse(
    val groups: List<GroupDto>
)

data class DigestResponse(
    @SerializedName("digest_frequency")
    val digestFrequency: String
)

data class UpdateDigestRequest(
    @SerializedName("digest_frequency")
    val digestFrequency: String
)
