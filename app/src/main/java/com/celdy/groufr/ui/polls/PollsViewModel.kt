package com.celdy.groufr.ui.polls

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.polls.PollDto
import com.celdy.groufr.data.polls.PollsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PollsViewModel @Inject constructor(
    private val pollsRepository: PollsRepository
) : ViewModel() {
    private val _state = MutableLiveData<PollsState>(PollsState.Loading)
    val state: LiveData<PollsState> = _state

    fun loadPolls(groupId: Long) {
        _state.value = PollsState.Loading
        viewModelScope.launch {
            try {
                val polls = pollsRepository.loadPolls(groupId)
                _state.value = PollsState.Content(polls)
            } catch (exception: Exception) {
                _state.value = PollsState.Error
            }
        }
    }
}

sealed class PollsState {
    data object Loading : PollsState()
    data class Content(val polls: List<PollDto>) : PollsState()
    data object Error : PollsState()
}
