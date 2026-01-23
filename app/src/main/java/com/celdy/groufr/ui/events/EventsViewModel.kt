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
import javax.inject.Inject

enum class TimeFilter(val apiValue: String) {
    UPCOMING("upcoming"),
    PAST("past"),
    ALL("all")
}

enum class ParticipationFilter(val apiValue: String?) {
    ALL(null),
    JOINED("joined"),
    MAYBE("maybe"),
    DECLINED("declined")
}

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val groupsRepository: GroupsRepository
) : ViewModel() {
    private val _state = MutableLiveData<EventsState>(EventsState.Loading)
    val state: LiveData<EventsState> = _state

    private val _timeFilter = MutableLiveData(TimeFilter.UPCOMING)
    val timeFilter: LiveData<TimeFilter> = _timeFilter

    private val _participationFilter = MutableLiveData(ParticipationFilter.ALL)
    val participationFilter: LiveData<ParticipationFilter> = _participationFilter

    private var currentGroupId: Long = -1L

    fun loadEvents(groupId: Long) {
        currentGroupId = groupId
        reload()
    }

    fun loadAllEvents() {
        currentGroupId = -1L
        reload()
    }

    fun setTimeFilter(filter: TimeFilter) {
        if (_timeFilter.value != filter) {
            _timeFilter.value = filter
            reload()
        }
    }

    fun setParticipationFilter(filter: ParticipationFilter) {
        if (_participationFilter.value != filter) {
            _participationFilter.value = filter
            reload()
        }
    }

    private fun reload() {
        _state.value = EventsState.Loading
        viewModelScope.launch {
            try {
                val time = _timeFilter.value ?: TimeFilter.UPCOMING
                val participation = _participationFilter.value ?: ParticipationFilter.ALL

                val events = if (currentGroupId > 0) {
                    eventsRepository.loadEvents(
                        groupId = currentGroupId,
                        filter = time.apiValue,
                        participation = participation.apiValue
                    )
                } else {
                    eventsRepository.loadAllEvents(
                        time = time.apiValue,
                        participation = participation.apiValue
                    )
                }
                _state.value = EventsState.Content(events)
            } catch (exception: Exception) {
                _state.value = EventsState.Error
            }
        }
    }

    @Deprecated("Use loadAllEvents() instead", ReplaceWith("loadAllEvents()"))
    fun loadAllFutureEvents() = loadAllEvents()
}

sealed class EventsState {
    data object Loading : EventsState()
    data class Content(val events: List<EventDto>) : EventsState()
    data object Error : EventsState()
}
