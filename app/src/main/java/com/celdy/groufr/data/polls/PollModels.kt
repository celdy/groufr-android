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
    val question: String,
    val description: String?,
    val multiselect: Boolean,
    val options: List<PollOptionDto>,
    @SerializedName("your_votes")
    val yourVotes: List<Long>,
    @SerializedName("total_voters")
    val totalVoters: Int,
    val status: String,
    @SerializedName("deadline_at")
    val deadlineAt: String?,
    @SerializedName("created_at")
    val createdAt: String
)

data class PollOptionDto(
    val id: Long,
    val label: String,
    @SerializedName("vote_count")
    val voteCount: Int
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
