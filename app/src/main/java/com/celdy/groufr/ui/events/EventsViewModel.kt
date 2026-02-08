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
    GOING_AND_MAYBE("joined,maybe"),
    UNRESPONDED("unresponded"),
    GOING("joined"),
    MAYBE("maybe"),
    DECLINED("declined"),
    ALL(null)
}

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val groupsRepository: GroupsRepository
) : ViewModel() {
    private val _state = MutableLiveData<EventsState>(EventsState.Loading)
    val state: LiveData<EventsState> = _state

    private val _refreshing = MutableLiveData(false)
    val refreshing: LiveData<Boolean> = _refreshing

    private val _timeFilter = MutableLiveData(TimeFilter.UPCOMING)
    val timeFilter: LiveData<TimeFilter> = _timeFilter

    private val _participationFilter = MutableLiveData<ParticipationFilter>()
    val participationFilter: LiveData<ParticipationFilter> = _participationFilter

    private var currentGroupId: Long = -1L
    private var filtersInitialized = false

    fun loadEvents(groupId: Long) {
        currentGroupId = groupId
        if (!filtersInitialized) {
            // From group detail: default to All participation
            _participationFilter.value = ParticipationFilter.ALL
            filtersInitialized = true
        }
        reload()
    }

    fun loadAllEvents() {
        currentGroupId = -1L
        if (!filtersInitialized) {
            // From main activity: default to Going & Maybe
            _participationFilter.value = ParticipationFilter.GOING_AND_MAYBE
            filtersInitialized = true
        }
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

    fun refresh() {
        reload(isRefresh = true)
    }

    private fun reload(isRefresh: Boolean = false) {
        if (isRefresh) {
            _refreshing.value = true
        } else {
            _state.value = EventsState.Loading
        }
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
                val sortedEvents = sortEvents(events, time)
                _state.value = EventsState.Content(sortedEvents)
            } catch (exception: Exception) {
                if (!isRefresh) {
                    _state.value = EventsState.Error
                }
            } finally {
                _refreshing.value = false
            }
        }
    }

    /**
     * Sort events based on the time filter:
     * - UPCOMING: ascending by startAt, then endAt; NULLs at the end
     * - PAST: descending by startAt, then endAt
     * - ALL: descending by startAt, then endAt; NULLs at the end
     */
    private fun sortEvents(events: List<EventDto>, timeFilter: TimeFilter): List<EventDto> {
        return when (timeFilter) {
            TimeFilter.UPCOMING -> {
                events.sortedWith(
                    compareBy<EventDto, String?>(nullsLast()) { it.startAt }
                        .thenBy(nullsLast()) { it.endAt }
                )
            }
            TimeFilter.PAST -> {
                events.sortedWith(
                    compareByDescending<EventDto> { it.startAt }
                        .thenByDescending { it.endAt }
                )
            }
            TimeFilter.ALL -> {
                events.sortedWith(
                    compareByDescending<EventDto, String?>(nullsLast()) { it.startAt }
                        .thenByDescending(nullsLast()) { it.endAt }
                )
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
