package com.celdy.groufr.ui.eventcreate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.events.EventsRepository
import com.celdy.groufr.data.notifications.NotificationSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventCreateViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val notificationSyncManager: NotificationSyncManager
) : ViewModel() {
    private val _state = MutableLiveData<CreateState>(CreateState.Idle)
    val state: LiveData<CreateState> = _state

    fun createEvent(
        groupId: Long,
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
            _state.value = CreateState.Error("Title is required.")
            return
        }
        val minValue = parseOptionalInt(minParticipants, "Min participants must be a number.")
        if (minParticipants?.trim()?.isNotEmpty() == true && minValue == null) return
        val maxValue = parseOptionalInt(maxParticipants, "Max participants must be a number.")
        if (maxParticipants?.trim()?.isNotEmpty() == true && maxValue == null) return
        if (minValue != null && maxValue != null && minValue > maxValue) {
            _state.value = CreateState.Error("Min participants cannot exceed max participants.")
            return
        }
        _state.value = CreateState.Sending
        viewModelScope.launch {
            try {
                eventsRepository.createEvent(
                    groupId = groupId,
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
                _state.value = CreateState.Success
                notificationSyncManager.onUserAction()
            } catch (exception: Exception) {
                _state.value = CreateState.Error("Failed to create event.")
            }
        }
    }

    private fun parseOptionalInt(value: String?, errorMessage: String): Int? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val parsed = trimmed.toIntOrNull()
        if (parsed == null) {
            _state.value = CreateState.Error(errorMessage)
        }
        return parsed
    }
}

sealed class CreateState {
    data object Idle : CreateState()
    data object Sending : CreateState()
    data object Success : CreateState()
    data class Error(val message: String) : CreateState()
}
