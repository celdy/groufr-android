package com.celdy.groufr.data.polls

import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class PollsRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun loadPolls(groupId: Long, status: String = "all"): List<PollDto> {
        return apiService.getGroupPolls(groupId, status = status).polls
    }

    suspend fun loadPoll(groupId: Long, pollId: Long): PollDto? {
        return apiService.getGroupPolls(groupId, status = "all").polls.firstOrNull { it.id == pollId }
    }

    suspend fun vote(pollId: Long, optionIds: List<Long>): PollDto {
        return apiService.voteOnPoll(pollId, VoteRequest(optionIds = optionIds))
    }

    suspend fun clearVote(pollId: Long): PollDto {
        return apiService.clearPollVote(pollId)
    }

    suspend fun createPoll(
        groupId: Long,
        question: String,
        description: String?,
        multiselect: Boolean,
        options: List<String>,
        deadlineAt: String?
    ): PollDto {
        val request = CreatePollRequest(
            question = question,
            description = description,
            multiselect = multiselect,
            options = options,
            deadlineAt = deadlineAt
        )
        return apiService.createPoll(groupId, request)
    }
}
