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
        startAt: String,
        endAt: String?,
        deadlineJoinAt: String?
    ) {
        if (title.isBlank() || startAt.isBlank()) {
            _state.value = CreateState.Error("Title and start date are required.")
            return
        }
        _state.value = CreateState.Sending
        viewModelScope.launch {
            try {
                eventsRepository.createEvent(
                    groupId = groupId,
                    title = title.trim(),
                    description = description?.trim()?.ifBlank { null },
                    startAt = startAt.trim(),
                    endAt = endAt?.trim()?.ifBlank { null },
                    deadlineJoinAt = deadlineJoinAt?.trim()?.ifBlank { null }
                )
                _state.value = CreateState.Success
                notificationSyncManager.onUserAction()
            } catch (exception: Exception) {
                _state.value = CreateState.Error("Failed to create event.")
            }
        }
    }
}

sealed class CreateState {
    data object Idle : CreateState()
    data object Sending : CreateState()
    data object Success : CreateState()
    data class Error(val message: String) : CreateState()
}
