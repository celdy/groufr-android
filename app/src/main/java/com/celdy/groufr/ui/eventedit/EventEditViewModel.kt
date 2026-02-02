package com.celdy.groufr.ui.eventedit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.events.EventDetailDto
import com.celdy.groufr.data.events.EventsRepository
import com.celdy.groufr.data.notifications.NotificationSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventEditViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val notificationSyncManager: NotificationSyncManager
) : ViewModel() {
    private val _state = MutableLiveData<EventEditState>(EventEditState.Loading)
    val state: LiveData<EventEditState> = _state

    fun loadEvent(eventId: Long) {
        _state.value = EventEditState.Loading
        viewModelScope.launch {
            try {
                val event = eventsRepository.loadEventDetail(eventId)
                _state.value = EventEditState.Content(event)
            } catch (exception: Exception) {
                _state.value = EventEditState.Error("Unable to load event.")
            }
        }
    }

    fun updateEvent(
        eventId: Long,
        title: String,
        description: String?,
        place: String?,
        startAt: String?,
        endAt: String?,
        deadlineJoinAt: String?,
        minParticipants: String?,
        maxParticipants: String?,
        state: String?
    ) {
        if (title.isBlank()) {
            _state.value = EventEditState.Error("Title is required.")
            return
        }
        val minValue = parseOptionalInt(minParticipants, "Min participants must be a number.")
        if (minParticipants?.trim()?.isNotEmpty() == true && minValue == null) return
        val maxValue = parseOptionalInt(maxParticipants, "Max participants must be a number.")
        if (maxParticipants?.trim()?.isNotEmpty() == true && maxValue == null) return
        if (minValue != null && maxValue != null && minValue > maxValue) {
            _state.value = EventEditState.Error("Min participants cannot exceed max participants.")
            return
        }
        _state.value = EventEditState.Saving
        viewModelScope.launch {
            try {
                eventsRepository.updateEventDetails(
                    eventId = eventId,
                    title = title.trim(),
                    description = description?.trim()?.ifBlank { null },
                    place = place?.trim()?.ifBlank { null },
                    startAt = startAt?.trim()?.ifBlank { null },
                    endAt = endAt?.trim()?.ifBlank { null },
                    deadlineJoinAt = deadlineJoinAt?.trim()?.ifBlank { null },
                    minParticipants = minValue,
                    maxParticipants = maxValue,
                    state = state
                )
                _state.value = EventEditState.Success
                notificationSyncManager.onUserAction()
            } catch (exception: Exception) {
                _state.value = EventEditState.Error("Failed to update event.")
            }
        }
    }

    private fun parseOptionalInt(value: String?, errorMessage: String): Int? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val parsed = trimmed.toIntOrNull()
        if (parsed == null) {
            _state.value = EventEditState.Error(errorMessage)
        }
        return parsed
    }
}

sealed class EventEditState {
    data object Loading : EventEditState()
    data class Content(val event: EventDetailDto) : EventEditState()
    data object Saving : EventEditState()
    data object Success : EventEditState()
    data class Error(val message: String) : EventEditState()
}
