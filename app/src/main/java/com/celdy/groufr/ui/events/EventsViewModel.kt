package com.celdy.groufr.ui.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.events.EventDto
import com.celdy.groufr.data.events.EventsRepository
import com.celdy.groufr.data.groups.GroupsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val groupsRepository: GroupsRepository
) : ViewModel() {
    private val _state = MutableLiveData<EventsState>(EventsState.Loading)
    val state: LiveData<EventsState> = _state

    fun loadEvents(groupId: Long) {
        _state.value = EventsState.Loading
        viewModelScope.launch {
            try {
                val events = eventsRepository.loadEvents(groupId, filter = "all")
                _state.value = EventsState.Content(filterFutureEvents(events))
            } catch (exception: Exception) {
                _state.value = EventsState.Error
            }
        }
    }

    fun loadAllFutureEvents() {
        _state.value = EventsState.Loading
        viewModelScope.launch {
            try {
                val events = eventsRepository.loadAllEvents(time = "upcoming")
                _state.value = EventsState.Content(events)
            } catch (exception: Exception) {
                _state.value = EventsState.Error
            }
        }
    }

    private fun filterFutureEvents(events: List<EventDto>): List<EventDto> {
        val now = OffsetDateTime.now()
        return events.filter { isFutureEvent(it, now) }
    }

    private fun isFutureEvent(event: EventDto, now: OffsetDateTime): Boolean {
        if (event.state != "offered" && event.state != "preparing") {
            return false
        }
        return try {
            if (event.startAt.isBlank()) {
                true
            } else {
                OffsetDateTime.parse(event.startAt).isAfter(now)
            }
        } catch (exception: DateTimeParseException) {
            true
        }
    }
}

sealed class EventsState {
    data object Loading : EventsState()
    data class Content(val events: List<EventDto>) : EventsState()
    data object Error : EventsState()
}
