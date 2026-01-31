package com.celdy.groufr.data.polls

import com.celdy.groufr.data.local.PollDao
import com.celdy.groufr.data.local.toDto
import com.celdy.groufr.data.local.toEntity
import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class PollsRepository @Inject constructor(
    private val apiService: ApiService,
    private val pollDao: PollDao
) {
    suspend fun loadPolls(groupId: Long, status: String = "all"): List<PollDto> {
        return try {
            val polls = apiService.getGroupPolls(groupId, status = status).polls
            pollDao.upsertAll(polls.map { it.toEntity() })
            polls
        } catch (exception: Exception) {
            val cached = if (status == "all") {
                pollDao.getByGroup(groupId)
            } else {
                pollDao.getByGroupAndStatus(groupId, status)
            }
            if (cached.isNotEmpty()) {
                cached.map { it.toDto() }
            } else {
                throw exception
            }
        }
    }

    suspend fun loadPoll(groupId: Long, pollId: Long): PollDto? {
        return try {
            val poll = apiService.getPollDetail(pollId)
            pollDao.upsert(poll.toEntity())
            poll
        } catch (exception: Exception) {
            pollDao.getById(pollId)?.toDto()
        }
    }

    suspend fun vote(pollId: Long, optionIds: List<Long>): PollDto {
        val poll = apiService.voteOnPoll(pollId, VoteRequest(optionIds = optionIds))
        pollDao.upsert(poll.toEntity())
        return poll
    }

    suspend fun clearVote(pollId: Long): PollDto {
        val poll = apiService.clearPollVote(pollId)
        pollDao.upsert(poll.toEntity())
        return poll
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
        val poll = apiService.createPoll(groupId, request)
        pollDao.upsert(poll.toEntity())
        return poll
    }
}
