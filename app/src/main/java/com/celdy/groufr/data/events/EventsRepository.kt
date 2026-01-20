package com.celdy.groufr.data.events

import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class EventsRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun loadEvents(groupId: Long, filter: String = "upcoming"): List<EventDto> {
        return apiService.getGroupEvents(groupId, filter = filter).events
    }

    suspend fun loadAllEvents(time: String = "upcoming", state: String? = null): List<EventDto> {
        return apiService.getAllEvents(time = time, state = state).events
    }

    suspend fun loadEventDetail(eventId: Long): EventDetailDto {
        return apiService.getEventDetail(eventId)
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
        return apiService.updateEvent(eventId, UpdateEventRequest(state = state))
    }

    suspend fun createEvent(
        groupId: Long,
        title: String,
        description: String?,
        startAt: String,
        endAt: String?,
        deadlineJoinAt: String?
    ): EventDto {
        val request = CreateEventRequest(
            title = title,
            description = description,
            startAt = startAt,
            endAt = endAt,
            deadlineJoinAt = deadlineJoinAt
        )
        return apiService.createEvent(groupId, request)
    }
}
