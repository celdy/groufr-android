package com.celdy.groufr.data.groups

import com.google.gson.annotations.SerializedName

data class GroupDetailDto(
    val id: Long,
    val slug: String,
    val name: String,
    val description: String?,
    @SerializedName("your_role")
    val yourRole: String?,
    @SerializedName("member_count")
    val memberCount: Int,
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
