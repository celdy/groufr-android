package com.celdy.groufr.data.events

import com.celdy.groufr.data.local.EventDao
import com.celdy.groufr.data.local.UserDao
import com.celdy.groufr.data.local.toDetailDto
import com.celdy.groufr.data.local.toDto
import com.celdy.groufr.data.local.toEntity
import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class EventsRepository @Inject constructor(
    private val apiService: ApiService,
    private val eventDao: EventDao,
    private val userDao: UserDao
) {
    suspend fun loadEvents(
        groupId: Long,
        filter: String = "upcoming",
        participation: String? = null
    ): List<EventDto> {
        return try {
            val events = apiService.getGroupEvents(
                groupId = groupId,
                filter = filter,
                participation = participation
            ).events
            eventDao.upsertAll(events.map { it.toEntity() })
            events
        } catch (exception: Exception) {
            val cached = eventDao.getByGroup(groupId)
            if (cached.isNotEmpty()) {
                cached.map { it.toDto() }
            } else {
                throw exception
            }
        }
    }

    suspend fun loadAllEvents(
        time: String = "upcoming",
        participation: String? = null,
        state: String? = null
    ): List<EventDto> {
        return try {
            val events = apiService.getAllEvents(
                time = time,
                participation = participation,
                state = state
            ).events
            eventDao.upsertAll(events.map { it.toEntity() })
            events
        } catch (exception: Exception) {
            val cached = eventDao.getAll()
            if (cached.isNotEmpty()) {
                cached.map { it.toDto() }
            } else {
                throw exception
            }
        }
    }

    suspend fun loadEventDetail(eventId: Long): EventDetailDto {
        return try {
            val event = apiService.getEventDetail(eventId)
            val users = event.participantsList.map { it.user.toEntity() }
            if (users.isNotEmpty()) {
                userDao.upsertAll(users)
            }
            eventDao.upsert(event.toEntity())
            event
        } catch (exception: Exception) {
            val cached = eventDao.getById(eventId)
            cached?.toDetailDto() ?: throw exception
        }
    }

    suspend fun joinEvent(eventId: Long): EventActionResponse {
        return apiService.joinEvent(eventId)
    }

    suspend fun declineEvent(eventId: Long): EventActionResponse {
        return apiService.declineEvent(eventId)
    }

    suspend fun maybeEvent(eventId: Long): EventActionResponse {
        return apiService.maybeEvent(eventId)
    }

    suspend fun updateEventState(eventId: Long, state: String): EventDetailDto {
        val event = apiService.updateEvent(eventId, UpdateEventRequest(state = state))
        val users = event.participantsList.map { it.user.toEntity() }
        if (users.isNotEmpty()) {
            userDao.upsertAll(users)
        }
        eventDao.upsert(event.toEntity())
        return event
    }

    suspend fun updateEventDetails(
        eventId: Long,
        title: String,
        description: String?,
        place: String?,
        startAt: String,
        endAt: String?,
        deadlineJoinAt: String?,
        minParticipants: Int?,
        maxParticipants: Int?,
        state: String?
    ): EventDetailDto {
        val event = apiService.updateEvent(
            eventId,
            UpdateEventRequest(
                title = title,
                description = description,
                place = place,
                startAt = startAt,
                endAt = endAt,
                deadlineJoinAt = deadlineJoinAt,
                minParticipants = minParticipants,
                maxParticipants = maxParticipants,
                state = state
            )
        )
        val users = event.participantsList.map { it.user.toEntity() }
        if (users.isNotEmpty()) {
            userDao.upsertAll(users)
        }
        eventDao.upsert(event.toEntity())
        return event
    }

    suspend fun createEvent(
        groupId: Long,
        title: String,
        description: String?,
        place: String?,
        startAt: String,
        endAt: String?,
        deadlineJoinAt: String?,
        minParticipants: Int?,
        maxParticipants: Int?,
        state: String?
    ): EventDto {
        val request = CreateEventRequest(
            title = title,
            description = description,
            place = place,
            startAt = startAt,
            endAt = endAt,
            deadlineJoinAt = deadlineJoinAt,
            minParticipants = minParticipants,
            maxParticipants = maxParticipants,
            state = state
        )
        val event = apiService.createEvent(groupId, request)
        eventDao.upsert(event.toEntity())
        return event
    }
}
