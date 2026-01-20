package com.celdy.groufr.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.notifications.NotificationDto
import com.celdy.groufr.data.notifications.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationsRepository
) : ViewModel() {
    private val _state = MutableLiveData<NotificationsState>(NotificationsState.Loading)
    val state: LiveData<NotificationsState> = _state
    private var unreadOnly: Boolean = false

    fun loadNotifications(unreadOnly: Boolean = false) {
        this.unreadOnly = unreadOnly
        _state.value = NotificationsState.Loading
        viewModelScope.launch {
            try {
                val notifications = repository.loadNotifications(unreadOnly)
                _state.value = NotificationsState.Content(notifications)
            } catch (exception: Exception) {
                _state.value = NotificationsState.Error
            }
        }
    }

    fun toggleUnreadOnly(): Boolean {
        unreadOnly = !unreadOnly
        loadNotifications(unreadOnly)
        return unreadOnly
    }

    fun currentUnreadOnly(): Boolean = unreadOnly

    fun markRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                repository.markRead(notificationId)
                loadNotifications(unreadOnly)
            } catch (exception: Exception) {
                _state.value = NotificationsState.Error
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                repository.markAllRead()
                loadNotifications(unreadOnly)
            } catch (exception: Exception) {
                _state.value = NotificationsState.Error
            }
        }
    }
}

sealed class NotificationsState {
    data object Loading : NotificationsState()
    data class Content(val notifications: List<NotificationDto>) : NotificationsState()
    data object Error : NotificationsState()
}
