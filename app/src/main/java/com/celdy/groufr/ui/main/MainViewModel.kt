package com.celdy.groufr.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.groups.GroupDto
import com.celdy.groufr.data.groups.GroupsRepository
import com.celdy.groufr.data.notifications.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val groupsRepository: GroupsRepository,
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {
    private val _state = MutableLiveData<MainState>(MainState.Loading)
    val state: LiveData<MainState> = _state

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    fun loadGroups() {
        _state.value = MainState.Loading
        viewModelScope.launch {
            try {
                val groups = groupsRepository.loadGroups()
                _state.value = MainState.Content(groups)
                loadUnreadCount()
            } catch (exception: Exception) {
                _state.value = MainState.Error
            }
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            try {
                _unreadCount.value = notificationsRepository.loadUnreadCount()
            } catch (exception: Exception) {
                _unreadCount.value = 0
            }
        }
    }
}

sealed class MainState {
    data object Loading : MainState()
    data class Content(val groups: List<GroupDto>) : MainState()
    data object Error : MainState()
}
