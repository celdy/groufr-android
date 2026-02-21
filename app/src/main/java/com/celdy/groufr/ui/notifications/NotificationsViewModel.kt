package com.celdy.groufr.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.notifications.NotificationDto
import com.celdy.groufr.data.notifications.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationsRepository
) : ViewModel() {
    private val _state = MutableLiveData<NotificationsState>(NotificationsState.Loading)
    val state: LiveData<NotificationsState> = _state
    private var unreadOnly: Boolean = false

    private val _invitationResult = MutableLiveData<InvitationResult?>()
    val invitationResult: LiveData<InvitationResult?> = _invitationResult

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

    fun acceptInvitation(invitationId: Long, notificationId: Long) {
        viewModelScope.launch {
            try {
                repository.acceptInvitation(invitationId)
                repository.markRead(notificationId)
                _invitationResult.value = InvitationResult.Accepted
                loadNotifications(unreadOnly)
            } catch (exception: HttpException) {
                _invitationResult.value = parseInvitationError(exception)
            } catch (exception: Exception) {
                _invitationResult.value = InvitationResult.Error
            }
        }
    }

    fun declineInvitation(invitationId: Long, notificationId: Long) {
        viewModelScope.launch {
            try {
                repository.declineInvitation(invitationId)
                repository.markRead(notificationId)
                _invitationResult.value = InvitationResult.Declined
                loadNotifications(unreadOnly)
            } catch (exception: HttpException) {
                _invitationResult.value = parseInvitationError(exception)
            } catch (exception: Exception) {
                _invitationResult.value = InvitationResult.Error
            }
        }
    }

    private fun parseInvitationError(exception: HttpException): InvitationResult {
        if (exception.code() == 403) {
            try {
                val body = exception.response()?.errorBody()?.string()
                if (body != null) {
                    val json = JSONObject(body)
                    if (json.optString("error") == "email_mismatch") {
                        return InvitationResult.EmailMismatch
                    }
                }
            } catch (_: Exception) { }
        }
        return InvitationResult.Error
    }

    fun clearInvitationResult() {
        _invitationResult.value = null
    }
}

sealed class NotificationsState {
    data object Loading : NotificationsState()
    data class Content(val notifications: List<NotificationDto>) : NotificationsState()
    data object Error : NotificationsState()
}

sealed class InvitationResult {
    data object Accepted : InvitationResult()
    data object Declined : InvitationResult()
    data object EmailMismatch : InvitationResult()
    data object Error : InvitationResult()
}
