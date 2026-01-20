package com.celdy.groufr.ui.pollcreate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.notifications.NotificationSyncManager
import com.celdy.groufr.data.polls.PollsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PollCreateViewModel @Inject constructor(
    private val pollsRepository: PollsRepository,
    private val notificationSyncManager: NotificationSyncManager
) : ViewModel() {
    private val _state = MutableLiveData<PollCreateState>(PollCreateState.Idle)
    val state: LiveData<PollCreateState> = _state

    fun createPoll(
        groupId: Long,
        question: String,
        description: String?,
        multiselect: Boolean,
        options: List<String>,
        deadlineAt: String?
    ) {
        val cleanedOptions = options.map { it.trim() }.filter { it.isNotBlank() }
        if (question.isBlank() || cleanedOptions.size < 2) {
            _state.value = PollCreateState.Error("Question and at least two options are required.")
            return
        }
        _state.value = PollCreateState.Sending
        viewModelScope.launch {
            try {
                pollsRepository.createPoll(
                    groupId = groupId,
                    question = question.trim(),
                    description = description?.trim()?.ifBlank { null },
                    multiselect = multiselect,
                    options = cleanedOptions,
                    deadlineAt = deadlineAt?.trim()?.ifBlank { null }
                )
                _state.value = PollCreateState.Success
                notificationSyncManager.onUserAction()
            } catch (exception: Exception) {
                _state.value = PollCreateState.Error("Failed to create poll.")
            }
        }
    }
}

sealed class PollCreateState {
    data object Idle : PollCreateState()
    data object Sending : PollCreateState()
    data object Success : PollCreateState()
    data class Error(val message: String) : PollCreateState()
}
