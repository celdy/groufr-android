package com.celdy.groufr.ui.groupdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.celdy.groufr.data.messages.MessageDto
import com.celdy.groufr.data.messages.MessagesRepository
import com.celdy.groufr.data.events.EventsRepository
import com.celdy.groufr.data.polls.PollsRepository
import com.celdy.groufr.data.notifications.NotificationSyncManager
import com.celdy.groufr.data.storage.ChatLastSeenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val messagesRepository: MessagesRepository,
    private val eventsRepository: EventsRepository,
    private val pollsRepository: PollsRepository,
    private val notificationSyncManager: NotificationSyncManager,
    private val chatLastSeenStore: ChatLastSeenStore
) : ViewModel() {
    private val _state = MutableLiveData<GroupDetailState>(GroupDetailState.Loading)
    val state: LiveData<GroupDetailState> = _state

    private val _sendState = MutableLiveData<SendState>(SendState.Idle)
    val sendState: LiveData<SendState> = _sendState

    private val _refreshing = MutableLiveData(false)
    val refreshing: LiveData<Boolean> = _refreshing

    private val _badgeCounts = MutableLiveData(GroupBadgeCounts())
    val badgeCounts: LiveData<GroupBadgeCounts> = _badgeCounts

    private var currentGroupId: Long? = null
    private var currentMessages: List<MessageDto> = emptyList()
    private var oldestId: Long? = null
    private var hasMore = true
    private var isLoadingMore = false
    private var dividerBeforeMessageId: Long? = null
    private var dividerComputed = false

    fun loadMessages(groupId: Long) {
        currentGroupId = groupId
        fetchMessages(groupId = groupId, beforeId = null, append = false, isRefresh = true)
    }

    fun loadBadgeCounts(groupId: Long) {
        viewModelScope.launch {
            try {
                val openPolls = pollsRepository.loadPolls(groupId, status = "open").size
                val activeEvents = eventsRepository.loadEvents(groupId, filter = "all")
                    .count { it.state == "offered" || it.state == "preparing" }
                _badgeCounts.value = GroupBadgeCounts(openPolls, activeEvents)
            } catch (exception: Exception) {
                _badgeCounts.value = GroupBadgeCounts()
            }
        }
    }

    fun refresh() {
        val groupId = currentGroupId ?: return
        fetchMessages(groupId = groupId, beforeId = null, append = false, isRefresh = true)
    }

    fun loadMore() {
        val groupId = currentGroupId ?: return
        if (isLoadingMore || !hasMore) return
        isLoadingMore = true
        _state.value = GroupDetailState.Content(currentMessages, dividerBeforeMessageId, isLoadingMore = true)
        fetchMessages(groupId = groupId, beforeId = oldestId, append = true, isRefresh = false)
    }

    fun sendMessage(groupId: Long, body: String) {
        if (body.isBlank()) {
            _sendState.value = SendState.Error("Message cannot be empty.")
            return
        }
        _sendState.value = SendState.Sending
        viewModelScope.launch {
            try {
                messagesRepository.sendMessage(groupId, body.trim())
                _sendState.value = SendState.Sent
                loadMessages(groupId)
                notificationSyncManager.onUserAction()
            } catch (exception: Exception) {
                _sendState.value = SendState.Error("Failed to send message.")
            }
        }
    }

    private fun fetchMessages(
        groupId: Long,
        beforeId: Long?,
        append: Boolean,
        isRefresh: Boolean
    ) {
        if (isRefresh) {
            _refreshing.value = true
            if (currentMessages.isEmpty()) {
                _state.value = GroupDetailState.Loading
            }
        }
        viewModelScope.launch {
            try {
                val response = messagesRepository.loadMessages(groupId, beforeId)
                val incoming = response.messages
                val merged = mergeMessages(currentMessages, incoming, append)
                currentMessages = merged
                oldestId = response.meta?.oldestId ?: currentMessages.lastOrNull()?.id
                hasMore = response.meta?.hasMore ?: false
                isLoadingMore = false
                if (isRefresh && !dividerComputed) {
                    val lastSeenId = chatLastSeenStore.getLastSeenMessageId("group", groupId)
                    if (lastSeenId != 0L) {
                        dividerBeforeMessageId = currentMessages.firstOrNull { it.id > lastSeenId }?.id
                    }
                    val maxId = currentMessages.maxOfOrNull { it.id } ?: 0L
                    chatLastSeenStore.setLastSeenMessageId("group", groupId, maxId)
                    dividerComputed = true
                }
                _state.value = GroupDetailState.Content(currentMessages, dividerBeforeMessageId, isLoadingMore = false)
            } catch (exception: Exception) {
                isLoadingMore = false
                _state.value = GroupDetailState.Error
            } finally {
                _refreshing.value = false
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

sealed class GroupDetailState {
    data object Loading : GroupDetailState()
    data class Content(
        val messages: List<MessageDto>,
        val dividerBeforeMessageId: Long?,
        val isLoadingMore: Boolean
    ) : GroupDetailState()
    data object Error : GroupDetailState()
}

sealed class SendState {
    data object Idle : SendState()
    data object Sending : SendState()
    data object Sent : SendState()
    data class Error(val message: String) : SendState()
}

data class GroupBadgeCounts(
    val openPolls: Int = 0,
    val activeEvents: Int = 0
)
