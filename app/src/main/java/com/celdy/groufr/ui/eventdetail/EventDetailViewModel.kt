package com.celdy.groufr.ui.eventdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.events.EventDetailDto
import com.celdy.groufr.data.events.EventsRepository
import com.celdy.groufr.data.expenses.EventExpensesResponse
import com.celdy.groufr.data.expenses.ExpensesRepository
import com.celdy.groufr.data.messages.MessageDto
import com.celdy.groufr.data.messages.MessagesRepository
import com.celdy.groufr.data.notifications.NotificationsRepository
import com.celdy.groufr.data.notifications.NotificationSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val messagesRepository: MessagesRepository,
    private val notificationsRepository: NotificationsRepository,
    private val notificationSyncManager: NotificationSyncManager,
    private val expensesRepository: ExpensesRepository
) : ViewModel() {
    private val _state = MutableLiveData<EventDetailState>(EventDetailState.Loading)
    val state: LiveData<EventDetailState> = _state

    private val _actionState = MutableLiveData<ActionState>(ActionState.Idle)
    val actionState: LiveData<ActionState> = _actionState

    private val _chatState = MutableLiveData<EventChatState>(EventChatState.Loading)
    val chatState: LiveData<EventChatState> = _chatState

    private val _chatSendState = MutableLiveData<EventChatSendState>(EventChatSendState.Idle)
    val chatSendState: LiveData<EventChatSendState> = _chatSendState

    private val _chatRefreshing = MutableLiveData(false)
    val chatRefreshing: LiveData<Boolean> = _chatRefreshing

    private val _expensesState = MutableLiveData<EventExpensesState>(EventExpensesState.Loading)
    val expensesState: LiveData<EventExpensesState> = _expensesState

    private val _expenseActionState = MutableLiveData<ActionState>(ActionState.Idle)
    val expenseActionState: LiveData<ActionState> = _expenseActionState

    private var currentEventId: Long? = null
    private var currentMessages: List<MessageDto> = emptyList()
    private var oldestId: Long? = null
    private var hasMore = true
    private var isLoadingMore = false
    private var dividerBeforeMessageId: Long? = null
    private var dividerComputed = false

    fun loadEvent(eventId: Long) {
        _state.value = EventDetailState.Loading
        viewModelScope.launch {
            try {
                val event = eventsRepository.loadEventDetail(eventId)
                _state.value = EventDetailState.Content(event)
            } catch (exception: Exception) {
                _state.value = EventDetailState.Error
            }
        }
    }

    fun joinEvent(eventId: Long) {
        _actionState.value = ActionState.Sending
        viewModelScope.launch {
            try {
                eventsRepository.joinEvent(eventId)
                _actionState.value = ActionState.Sent
                loadEvent(eventId)
                notificationSyncManager.onUserAction()
            } catch (exception: Exception) {
                _actionState.value = ActionState.Error("Failed to join event.")
            }
        }
    }

    fun declineEvent(eventId: Long) {
        _actionState.value = ActionState.Sending
        viewModelScope.launch {
            try {
                eventsRepository.declineEvent(eventId)
                _actionState.value = ActionState.Sent
                loadEvent(eventId)
                notificationSyncManager.onUserAction()
            } catch (exception: Exception) {
                _actionState.value = ActionState.Error("Failed to decline event.")
            }
        }
    }

    fun maybeEvent(eventId: Long) {
        _actionState.value = ActionState.Sending
        viewModelScope.launch {
            try {
                eventsRepository.maybeEvent(eventId)
                _actionState.value = ActionState.Sent
                loadEvent(eventId)
                notificationSyncManager.onUserAction()
            } catch (exception: Exception) {
                _actionState.value = ActionState.Error("Failed to update status.")
            }
        }
    }

    fun updateEventState(eventId: Long, state: String) {
        _actionState.value = ActionState.Sending
        viewModelScope.launch {
            try {
                eventsRepository.updateEventState(eventId, state)
                _actionState.value = ActionState.Sent
                loadEvent(eventId)
            } catch (exception: Exception) {
                _actionState.value = ActionState.Error("Failed to update event state.")
            }
        }
    }

    fun loadChat(eventId: Long) {
        currentEventId = eventId
        fetchChatMessages(eventId = eventId, beforeId = null, append = false, isRefresh = true)
    }

    fun refreshChat() {
        val eventId = currentEventId ?: return
        fetchChatMessages(eventId = eventId, beforeId = null, append = false, isRefresh = true)
    }

    fun loadMoreChat() {
        val eventId = currentEventId ?: return
        if (isLoadingMore || !hasMore) return
        isLoadingMore = true
        _chatState.value = EventChatState.Content(currentMessages, dividerBeforeMessageId, isLoadingMore = true)
        fetchChatMessages(eventId = eventId, beforeId = oldestId, append = true, isRefresh = false)
    }

    fun sendChatMessage(eventId: Long, body: String) {
        if (body.isBlank()) {
            _chatSendState.value = EventChatSendState.Error("Message cannot be empty.")
            return
        }
        _chatSendState.value = EventChatSendState.Sending
        viewModelScope.launch {
            try {
                messagesRepository.sendEventMessage(eventId, body.trim())
                _chatSendState.value = EventChatSendState.Sent
                loadChat(eventId)
                notificationSyncManager.onUserAction()
            } catch (exception: Exception) {
                _chatSendState.value = EventChatSendState.Error("Failed to send message.")
            }
        }
    }

    fun loadExpenses(eventId: Long) {
        _expensesState.value = EventExpensesState.Loading
        viewModelScope.launch {
            try {
                val response = expensesRepository.loadEventExpenses(eventId)
                _expensesState.value = EventExpensesState.Content(response)
            } catch (exception: Exception) {
                _expensesState.value = EventExpensesState.Error
            }
        }
    }

    fun confirmExpense(eventId: Long, expenseId: Long) {
        _expenseActionState.value = ActionState.Sending
        viewModelScope.launch {
            try {
                expensesRepository.confirmExpense(expenseId)
                _expenseActionState.value = ActionState.Sent
                loadExpenses(eventId)
            } catch (exception: Exception) {
                _expenseActionState.value = ActionState.Error("Failed to confirm expense.")
            }
        }
    }

    fun confirmAllExpenses(eventId: Long) {
        _expenseActionState.value = ActionState.Sending
        viewModelScope.launch {
            try {
                val response = expensesRepository.confirmAllExpenses(eventId)
                _expenseActionState.value = ActionState.Sent
                loadExpenses(eventId)
            } catch (exception: Exception) {
                _expenseActionState.value = ActionState.Error("Failed to confirm expenses.")
            }
        }
    }

    fun disputeExpense(eventId: Long, expenseId: Long, reason: String) {
        _expenseActionState.value = ActionState.Sending
        viewModelScope.launch {
            try {
                expensesRepository.disputeExpense(expenseId, reason)
                _expenseActionState.value = ActionState.Sent
                loadExpenses(eventId)
            } catch (exception: Exception) {
                _expenseActionState.value = ActionState.Error("Failed to dispute expense.")
            }
        }
    }

    fun deleteExpense(eventId: Long, expenseId: Long) {
        _expenseActionState.value = ActionState.Sending
        viewModelScope.launch {
            try {
                expensesRepository.deleteExpense(expenseId)
                _expenseActionState.value = ActionState.Sent
                loadExpenses(eventId)
            } catch (exception: Exception) {
                _expenseActionState.value = ActionState.Error("Failed to delete expense.")
            }
        }
    }

    fun settleExpense(eventId: Long, expenseId: Long) {
        _expenseActionState.value = ActionState.Sending
        viewModelScope.launch {
            try {
                expensesRepository.settleExpense(expenseId)
                _expenseActionState.value = ActionState.Sent
                loadExpenses(eventId)
            } catch (exception: Exception) {
                _expenseActionState.value = ActionState.Error("Failed to settle expense.")
            }
        }
    }

    private fun fetchChatMessages(
        eventId: Long,
        beforeId: Long?,
        append: Boolean,
        isRefresh: Boolean
    ) {
        if (isRefresh) {
            _chatRefreshing.value = true
            if (currentMessages.isEmpty()) {
                _chatState.value = EventChatState.Loading
            }
        }
        viewModelScope.launch {
            try {
                val response = messagesRepository.loadEventMessages(eventId, beforeId)
                val incoming = response.messages
                val merged = mergeMessages(currentMessages, incoming, append)
                currentMessages = merged
                oldestId = response.meta?.oldestId ?: currentMessages.lastOrNull()?.id
                hasMore = response.meta?.hasMore ?: false
                isLoadingMore = false
                if (isRefresh && !dividerComputed) {
                    val lastSeenId = response.meta?.lastSeenMessageId
                    if (lastSeenId != null) {
                        dividerBeforeMessageId = currentMessages.firstOrNull { it.id > lastSeenId }?.id
                    }
                    dividerComputed = true
                }
                _chatState.value = EventChatState.Content(currentMessages, dividerBeforeMessageId, isLoadingMore = false)
                if (isRefresh) {
                    notificationsRepository.markEventDetailRead(eventId)
                    notificationSyncManager.onUserAction()
                }
            } catch (exception: Exception) {
                isLoadingMore = false
                _chatState.value = EventChatState.Error
            } finally {
                _chatRefreshing.value = false
            }
        }
    }

    private fun mergeMessages(
        current: List<MessageDto>,
        incoming: List<MessageDto>,
        append: Boolean
    ): List<MessageDto> {
        if (incoming.isEmpty()) return current
        val currentIds = current.asSequence().map { it.id }.toHashSet()
        val filtered = incoming.filterNot { currentIds.contains(it.id) }
        val merged = if (append) {
            current + filtered
        } else {
            incoming
        }
        return merged.sortedBy { it.id }
    }
}

sealed class EventDetailState {
    data object Loading : EventDetailState()
    data class Content(val event: EventDetailDto) : EventDetailState()
    data object Error : EventDetailState()
}

sealed class ActionState {
    data object Idle : ActionState()
    data object Sending : ActionState()
    data object Sent : ActionState()
    data class Error(val message: String) : ActionState()
}

sealed class EventChatState {
    data object Loading : EventChatState()
    data class Content(
        val messages: List<MessageDto>,
        val dividerBeforeMessageId: Long?,
        val isLoadingMore: Boolean
    ) : EventChatState()
    data object Error : EventChatState()
}

sealed class EventChatSendState {
    data object Idle : EventChatSendState()
    data object Sending : EventChatSendState()
    data object Sent : EventChatSendState()
    data class Error(val message: String) : EventChatSendState()
}

sealed class EventExpensesState {
    data object Loading : EventExpensesState()
    data class Content(val data: EventExpensesResponse) : EventExpensesState()
    data object Error : EventExpensesState()
}
