package com.celdy.groufr.data.polls

import com.google.gson.annotations.SerializedName

data class PollsResponse(
    val polls: List<PollDto>,
    val meta: PollsMeta? = null
)

data class PollsMeta(
    val total: Int,
    val limit: Int,
    val offset: Int
)

data class PollDto(
    val id: Long,
    @SerializedName("group_id")
    val groupId: Long,
    @SerializedName("event_id")
    val eventId: Long? = null,
    @SerializedName("created_by")
    val createdBy: PollUserDto? = null,
    val question: String,
    val description: String?,
    val multiselect: Boolean,
    val options: List<PollOptionDto>,
    @SerializedName("your_votes")
    val yourVotes: List<Long>,
    @SerializedName("total_voters")
    val totalVoters: Int,
    @SerializedName("total_votes")
    val totalVotes: Int = 0,
    val status: String,
    @SerializedName("can_change_status")
    val canChangeStatus: Boolean = false,
    @SerializedName("deadline_at")
    val deadlineAt: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class PollOptionDto(
    val id: Long,
    val label: String,
    @SerializedName("vote_count")
    val voteCount: Int,
    @SerializedName("is_voted")
    val isVoted: Boolean = false,
    val voters: List<PollUserDto> = emptyList()
)

data class PollUserDto(
    val id: Long,
    val name: String
)

data class VoteRequest(
    @SerializedName("option_ids")
    val optionIds: List<Long>
)

data class CreatePollRequest(
    val question: String,
    val description: String?,
    val multiselect: Boolean,
    val options: List<String>,
    @SerializedName("deadline_at")
    val deadlineAt: String?
)

data class UpdatePollStatusRequest(
    val status: String
)
