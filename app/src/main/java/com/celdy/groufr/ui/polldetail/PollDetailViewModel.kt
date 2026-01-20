package com.celdy.groufr.ui.polldetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.notifications.NotificationSyncManager
import com.celdy.groufr.data.polls.PollDto
import com.celdy.groufr.data.polls.PollsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PollDetailViewModel @Inject constructor(
    private val pollsRepository: PollsRepository,
    private val notificationSyncManager: NotificationSyncManager
) : ViewModel() {
    private val _state = MutableLiveData<PollDetailState>(PollDetailState.Loading)
    val state: LiveData<PollDetailState> = _state

    private val _voteState = MutableLiveData<VoteState>(VoteState.Idle)
    val voteState: LiveData<VoteState> = _voteState

    fun loadPoll(groupId: Long, pollId: Long) {
        _state.value = PollDetailState.Loading
        viewModelScope.launch {
            try {
                val poll = pollsRepository.loadPoll(groupId, pollId)
                if (poll == null) {
                    _state.value = PollDetailState.Error
                } else {
                    _state.value = PollDetailState.Content(poll)
                }
            } catch (exception: Exception) {
                _state.value = PollDetailState.Error
            }
        }
    }

    fun vote(pollId: Long, optionIds: List<Long>) {
        if (optionIds.isEmpty()) {
            _voteState.value = VoteState.Error("Select at least one option.")
            return
        }
        _voteState.value = VoteState.Sending
        viewModelScope.launch {
            try {
                val poll = pollsRepository.vote(pollId, optionIds)
                _voteState.value = VoteState.Sent
                _state.value = PollDetailState.Content(poll)
                notificationSyncManager.onUserAction()
            } catch (exception: Exception) {
                _voteState.value = VoteState.Error("Failed to vote.")
            }
        }
    }

    fun clearVote(pollId: Long) {
        _voteState.value = VoteState.Sending
        viewModelScope.launch {
            try {
                val poll = pollsRepository.clearVote(pollId)
                _voteState.value = VoteState.Sent
                _state.value = PollDetailState.Content(poll)
                notificationSyncManager.onUserAction()
            } catch (exception: Exception) {
                _voteState.value = VoteState.Error("Failed to clear vote.")
            }
        }
    }
}

sealed class PollDetailState {
    data object Loading : PollDetailState()
    data class Content(val poll: PollDto) : PollDetailState()
    data object Error : PollDetailState()
}

sealed class VoteState {
    data object Idle : VoteState()
    data object Sending : VoteState()
    data object Sent : VoteState()
    data class Error(val message: String) : VoteState()
}
