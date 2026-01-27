package com.celdy.groufr.ui.eventdetail

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.data.events.EventDetailDto
import com.celdy.groufr.data.storage.TokenStore
import com.celdy.groufr.databinding.ActivityEventDetailBinding
import com.celdy.groufr.ui.common.ChatDateFormatter
import com.celdy.groufr.ui.common.MarkdownRenderer
import com.celdy.groufr.ui.eventedit.EventEditActivity
import com.celdy.groufr.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class EventDetailActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var tokenStore: TokenStore
    private lateinit var binding: ActivityEventDetailBinding
    private val viewModel: EventDetailViewModel by viewModels()
    private val adapter = EventParticipantAdapter()
    private lateinit var chatAdapter: EventChatAdapter
    private var eventId: Long = -1L
    private var currentStatus: String = ""
    private var groupName: String = ""
    private var activeSection = EventSection.INFO
    private var chatLoaded = false
    private var chatFirstLoad = true
    private var currentEvent: EventDetailDto? = null
    private var shouldRefreshOnResume = false
    private var contentPaddingBottom = 0
    private var systemBarsBottom = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        groupName = intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty()
        setSupportActionBar(binding.eventDetailToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.eventDetailToolbar.setNavigationOnClickListener { finish() }

        val chatLayoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        chatAdapter = EventChatAdapter(tokenStore.getUserId())
        binding.eventMessagesList.layoutManager = chatLayoutManager
        binding.eventMessagesList.adapter = chatAdapter

        binding.eventParticipants.layoutManager = LinearLayoutManager(this)
        binding.eventParticipants.adapter = adapter
        contentPaddingBottom = binding.eventDetailContent.paddingBottom
        updateSectionVisibility()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = max(systemBars.bottom, imeInsets.bottom)
            systemBarsBottom = systemBars.bottom
            binding.eventDetailToolbar.updatePadding(top = systemBars.top)
            updateSectionPadding()
            binding.eventMessageComposer.updatePadding(bottom = bottomInset)
            binding.root.updatePadding(left = systemBars.left, right = systemBars.right)
            insets
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                EventDetailState.Loading -> {
                    binding.eventDetailLoading.isVisible = true
                    binding.eventDetailContent.isVisible = false
                    binding.eventDetailError.isVisible = false
                }
                is EventDetailState.Content -> {
                    val event = state.event
                    val locale = currentLocale()
                    currentEvent = event
                    binding.eventDetailLoading.isVisible = false
                    binding.eventDetailContent.isVisible = true
                    binding.eventDetailError.isVisible = false
                    binding.eventTitle.text = event.title
                    val resolvedGroupName = if (groupName.isNotBlank()) groupName else event.groupName.orEmpty()
                    groupName = resolvedGroupName
                    binding.eventGroupName.text = resolvedGroupName
                    binding.eventGroupName.isVisible = resolvedGroupName.isNotBlank()
                    binding.eventDate.text = formatEventDate(event.startAt, event.endAt, locale)
                    val hasPlace = !event.place.isNullOrBlank()
                    binding.eventPlace.isVisible = hasPlace
                    binding.eventPlaceIcon.isVisible = hasPlace
                    binding.eventPlace.text = event.place.orEmpty()
                    binding.eventDescription.text = MarkdownRenderer.render(event.description.orEmpty())
                    binding.eventDescription.movementMethod = LinkMovementMethod.getInstance()
                    currentStatus = event.state
                    binding.eventState.text = formatEventStatus(event.state)
                    styleStateBadge(event.state)
                    binding.eventUserStatus.text = formatParticipantStatus(event.yourStatus)
                    styleUserStatusBadge(event.yourStatus)
                    val deadline = formatTime(event.deadlineJoinAt, getString(com.celdy.groufr.R.string.event_deadline_prefix))
                    binding.eventDeadline.text = deadline
                    binding.eventDeadline.isVisible = deadline.isNotBlank()
                    bindParticipantSummary(event.participants, event.participantsList)
                    adapter.submitList(sortParticipants(event.participantsList))
                    updateStatusBadgeAccess(event)
                    updateUserStatusBadgeAccess(event)
                    invalidateOptionsMenu()
                }
                EventDetailState.Error -> {
                    binding.eventDetailLoading.isVisible = false
                    binding.eventDetailContent.isVisible = false
                    binding.eventDetailError.isVisible = true
                    currentEvent = null
                    invalidateOptionsMenu()
                    if (!authRepository.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }

        viewModel.actionState.observe(this) { state ->
            when (state) {
                ActionState.Idle -> Unit
                ActionState.Sending -> {
                    Unit
                }
                ActionState.Sent -> {
                    Unit
                }
                is ActionState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.chatState.observe(this) { state ->
            when (state) {
                EventChatState.Loading -> {
                    binding.eventMessagesLoading.isVisible = true
                    binding.eventMessagesEmpty.isVisible = false
                }
                is EventChatState.Content -> {
                    binding.eventMessagesLoading.isVisible = false
                    val items = buildChatItems(state.messages, state.unreadCount)
                    val shouldScrollToBottom = chatFirstLoad || !binding.eventMessagesList.canScrollVertically(1)
                    chatAdapter.submitList(items)
                    binding.eventMessagesEmpty.isVisible = state.messages.isEmpty()
                    if (shouldScrollToBottom && items.isNotEmpty()) {
                        binding.eventMessagesList.scrollToPosition(items.size - 1)
                    }
                    chatFirstLoad = false
                }
                EventChatState.Error -> {
                    binding.eventMessagesLoading.isVisible = false
                    binding.eventMessagesEmpty.isVisible = true
                    binding.eventMessagesEmpty.text = getString(com.celdy.groufr.R.string.messages_error)
                }
            }
        }

        viewModel.chatSendState.observe(this) { state ->
            when (state) {
                EventChatSendState.Idle -> Unit
                EventChatSendState.Sending -> binding.eventMessageSend.isEnabled = false
                EventChatSendState.Sent -> {
                    binding.eventMessageSend.isEnabled = true
                    binding.eventMessageInput.text?.clear()
                }
                is EventChatSendState.Error -> {
                    binding.eventMessageSend.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.chatRefreshing.observe(this) { refreshing ->
            binding.eventMessagesRefresh.isRefreshing = refreshing
        }

        if (eventId > 0) {
            viewModel.loadEvent(eventId)
        } else {
            binding.eventDetailLoading.isVisible = false
            binding.eventDetailContent.isVisible = false
            binding.eventDetailError.isVisible = true
        }

        binding.eventMessagesRefresh.setOnRefreshListener {
            viewModel.refreshChat()
        }

        binding.eventMessageSend.setOnClickListener {
            if (eventId > 0) {
                viewModel.sendChatMessage(eventId, binding.eventMessageInput.text?.toString().orEmpty())
            }
        }

        binding.eventMessagesList.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lastVisible = (binding.eventMessagesList.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                if (lastVisible >= chatAdapter.itemCount - 5) {
                    viewModel.loadMoreChat()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (shouldRefreshOnResume && eventId > 0) {
            shouldRefreshOnResume = false
            viewModel.loadEvent(eventId)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(com.celdy.groufr.R.menu.menu_event_detail, menu)
        updateMenuVisibility(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        updateMenuVisibility(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            com.celdy.groufr.R.id.action_event_info -> {
                setActiveSection(EventSection.INFO)
                true
            }
            com.celdy.groufr.R.id.action_event_chat -> {
                setActiveSection(EventSection.CHAT)
                true
            }
            com.celdy.groufr.R.id.action_event_participants -> {
                setActiveSection(EventSection.PARTICIPANTS)
                true
            }
            com.celdy.groufr.R.id.action_event_edit -> {
                val intent = Intent(this, EventEditActivity::class.java)
                    .putExtra(EventEditActivity.EXTRA_EVENT_ID, eventId)
                    .putExtra(EventEditActivity.EXTRA_GROUP_NAME, groupName)
                shouldRefreshOnResume = true
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setActiveSection(section: EventSection) {
        if (activeSection == section) return
        activeSection = section
        updateSectionVisibility()
        invalidateOptionsMenu()
    }

    private fun updateSectionVisibility() {
        binding.eventInfoContainer.isVisible = activeSection == EventSection.INFO
        binding.eventChatContainer.isVisible = activeSection == EventSection.CHAT
        binding.eventParticipantsContainer.isVisible = activeSection == EventSection.PARTICIPANTS
        updateSectionPadding()
        if (activeSection == EventSection.CHAT && !chatLoaded && eventId > 0) {
            chatLoaded = true
            chatFirstLoad = true
            viewModel.loadChat(eventId)
        }
    }

    private fun updateSectionPadding() {
        val bottomPadding = if (activeSection == EventSection.CHAT) 0 else contentPaddingBottom + systemBarsBottom
        binding.eventDetailContent.updatePadding(bottom = bottomPadding)
    }

    private fun updateMenuVisibility(menu: Menu) {
        menu.findItem(com.celdy.groufr.R.id.action_event_info)?.isVisible = activeSection != EventSection.INFO
        menu.findItem(com.celdy.groufr.R.id.action_event_chat)?.isVisible = activeSection != EventSection.CHAT
        menu.findItem(com.celdy.groufr.R.id.action_event_participants)?.isVisible =
            activeSection != EventSection.PARTICIPANTS
        val canEdit = currentEvent?.let { canEditEvent(it) } ?: false
        menu.findItem(com.celdy.groufr.R.id.action_event_edit)?.isVisible = canEdit
    }

    private fun bindParticipantSummary(
        participants: Map<String, Int>,
        participantsList: List<com.celdy.groufr.data.events.EventParticipantDto>
    ) {
        val countsFromList = participantsList.groupingBy { it.status }.eachCount()
        val joined = participants["joined"] ?: countsFromList["joined"] ?: 0
        val maybe = participants["maybe"] ?: countsFromList["maybe"] ?: 0
        val declined = participants["declined"] ?: countsFromList["declined"] ?: 0
        val invited = participants["invited"] ?: countsFromList["invited"] ?: 0
        binding.eventSummaryJoined.text = joined.toString()
        binding.eventSummaryMaybe.text = maybe.toString()
        binding.eventSummaryDeclined.text = declined.toString()
        binding.eventSummaryInvited.text = invited.toString()
    }

    private fun updateStatusBadgeAccess(event: EventDetailDto) {
        val canEdit = canEditEvent(event)
        binding.eventStateContainer.isClickable = canEdit
        binding.eventStateContainer.isFocusable = canEdit
        binding.eventStateDropdown.isVisible = canEdit
        if (canEdit) {
            binding.eventStateContainer.setOnClickListener { showStatusDialog() }
        } else {
            binding.eventStateContainer.setOnClickListener(null)
        }
    }

    private fun updateUserStatusBadgeAccess(event: EventDetailDto) {
        val canEdit = event.yourStatus != "not_invited"
        binding.eventUserStatusContainer.isClickable = canEdit
        binding.eventUserStatusContainer.isFocusable = canEdit
        binding.eventUserStatusDropdown.isVisible = canEdit
        if (canEdit) {
            binding.eventUserStatusContainer.setOnClickListener { showUserStatusDialog(event.yourStatus) }
        } else {
            binding.eventUserStatusContainer.setOnClickListener(null)
        }
    }

    private fun canEditStatus(event: EventDetailDto): Boolean {
        return canEditEvent(event)
    }

    private fun canEditEvent(event: EventDetailDto): Boolean {
        val userId = tokenStore.getUserId()
        val role = event.participantsList.firstOrNull { it.user.id == userId }?.role
        val elevatedRoles = setOf("admin", "owner")
        return role in elevatedRoles || event.yourRole in elevatedRoles
    }

    private fun showStatusDialog() {
        val entries = STATUS_OPTIONS.map { getString(it.second) }.toTypedArray()
        val currentIndex = STATUS_OPTIONS.indexOfFirst { it.first == currentStatus }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(com.celdy.groufr.R.string.event_status_dialog_title)
            .setSingleChoiceItems(entries, currentIndex) { dialog, which ->
                val selectedState = STATUS_OPTIONS[which].first
                if (selectedState != currentStatus) {
                    currentStatus = selectedState
                    binding.eventState.text = getString(STATUS_OPTIONS[which].second)
                    styleStateBadge(selectedState)
                    viewModel.updateEventState(eventId, selectedState)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showUserStatusDialog(current: String) {
        val entries = USER_STATUS_OPTIONS.map { getString(it.second) }.toTypedArray()
        val currentIndex = USER_STATUS_OPTIONS.indexOfFirst { it.first == current }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(com.celdy.groufr.R.string.event_your_status_label)
            .setSingleChoiceItems(entries, currentIndex) { dialog, which ->
                val selected = USER_STATUS_OPTIONS[which].first
                when (selected) {
                    "joined" -> viewModel.joinEvent(eventId)
                    "maybe" -> viewModel.maybeEvent(eventId)
                    "declined" -> viewModel.declineEvent(eventId)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun formatEventStatus(status: String): String {
        return when (status) {
            "offered" -> getString(com.celdy.groufr.R.string.event_status_offered)
            "preparing" -> getString(com.celdy.groufr.R.string.event_status_preparing)
            "closed" -> getString(com.celdy.groufr.R.string.event_status_closed)
            "cancelled" -> getString(com.celdy.groufr.R.string.event_status_cancelled)
            else -> status
        }
    }

    private fun styleStateBadge(state: String) {
        when (state) {
            "offered" -> {
                binding.eventStateContainer.setBackgroundResource(com.celdy.groufr.R.drawable.bg_event_status_offered)
                binding.eventState.setTextColor(ContextCompat.getColor(this, com.celdy.groufr.R.color.event_state_offered_text))
                binding.eventStateDropdown.imageTintList = ContextCompat.getColorStateList(this, com.celdy.groufr.R.color.event_state_offered_text)
            }
            "preparing" -> {
                binding.eventStateContainer.setBackgroundResource(com.celdy.groufr.R.drawable.bg_event_status_preparing)
                binding.eventState.setTextColor(ContextCompat.getColor(this, com.celdy.groufr.R.color.event_state_preparing_text))
                binding.eventStateDropdown.imageTintList = ContextCompat.getColorStateList(this, com.celdy.groufr.R.color.event_state_preparing_text)
            }
            "closed" -> {
                binding.eventStateContainer.setBackgroundResource(com.celdy.groufr.R.drawable.bg_event_status_closed)
                binding.eventState.setTextColor(ContextCompat.getColor(this, com.celdy.groufr.R.color.event_state_closed_text))
                binding.eventStateDropdown.imageTintList = ContextCompat.getColorStateList(this, com.celdy.groufr.R.color.event_state_closed_text)
            }
            "cancelled" -> {
                binding.eventStateContainer.setBackgroundResource(com.celdy.groufr.R.drawable.bg_event_status_cancelled)
                binding.eventState.setTextColor(ContextCompat.getColor(this, com.celdy.groufr.R.color.event_state_cancelled_text))
                binding.eventStateDropdown.imageTintList = ContextCompat.getColorStateList(this, com.celdy.groufr.R.color.event_state_cancelled_text)
            }
            else -> {
                binding.eventStateContainer.setBackgroundResource(com.celdy.groufr.R.drawable.bg_event_status_badge)
                binding.eventState.setTextColor(ContextCompat.getColor(this, com.celdy.groufr.R.color.primary_700))
                binding.eventStateDropdown.imageTintList = ContextCompat.getColorStateList(this, com.celdy.groufr.R.color.primary_700)
            }
        }
    }

    private fun formatParticipantStatus(status: String): String {
        return when (status) {
            "joined" -> getString(com.celdy.groufr.R.string.event_participant_joined)
            "maybe" -> getString(com.celdy.groufr.R.string.event_participant_maybe)
            "declined" -> getString(com.celdy.groufr.R.string.event_participant_declined)
            "invited" -> getString(com.celdy.groufr.R.string.event_participant_invited)
            "not_invited" -> getString(com.celdy.groufr.R.string.event_participant_not_invited)
            else -> status
        }
    }

    private fun styleUserStatusBadge(status: String) {
        val bgRes = when (status) {
            "joined" -> com.celdy.groufr.R.drawable.bg_status_joined
            "maybe" -> com.celdy.groufr.R.drawable.bg_status_maybe
            "declined" -> com.celdy.groufr.R.drawable.bg_status_declined
            "invited" -> com.celdy.groufr.R.drawable.bg_status_invited
            "not_invited" -> com.celdy.groufr.R.drawable.bg_status_not_invited
            else -> com.celdy.groufr.R.drawable.bg_status_not_invited
        }
        binding.eventUserStatusContainer.setBackgroundResource(bgRes)
    }

    private fun formatEventDate(startAt: String?, endAt: String?, locale: Locale): String {
        val startBlank = startAt.isNullOrBlank()
        val endBlank = endAt.isNullOrBlank()
        if (startBlank && endBlank) {
            return getString(com.celdy.groufr.R.string.event_date_not_set)
        }
        if (!startBlank && endBlank) {
            return ChatDateFormatter.format(startAt!!, locale)
        }
        if (startBlank && !endBlank) {
            val endText = ChatDateFormatter.format(endAt!!, locale)
            return "${getString(com.celdy.groufr.R.string.event_date_end_prefix)} $endText"
        }
        return ChatDateFormatter.formatRange(startAt, endAt, locale)
            ?: getString(com.celdy.groufr.R.string.event_date_not_set)
    }

    private fun currentLocale(): Locale {
        val locales = resources.configuration.locales
        return if (locales.isEmpty) Locale.getDefault() else locales[0]
    }

    private fun formatTime(value: String?, prefix: String): String {
        if (value.isNullOrBlank()) return ""
        return try {
            val instant = OffsetDateTime.parse(value)
            "$prefix ${instant.format(DISPLAY_FORMAT)}"
        } catch (exception: DateTimeParseException) {
            ""
        }
    }

    private fun buildChatItems(
        messages: List<com.celdy.groufr.data.messages.MessageDto>,
        unreadCount: Int
    ): List<EventChatItem> {
        if (messages.isEmpty()) return emptyList()
        val safeUnread = unreadCount.coerceAtLeast(0).coerceAtMost(messages.size)
        if (safeUnread == 0) {
            return messages.map { EventChatItem.Message(it) }
        }
        val dividerIndex = messages.size - safeUnread
        val items = mutableListOf<EventChatItem>()
        messages.forEachIndexed { index, message ->
            if (index == dividerIndex) {
                items.add(EventChatItem.Divider)
            }
            items.add(EventChatItem.Message(message))
        }
        return items
    }

    private fun sortParticipants(
        list: List<com.celdy.groufr.data.events.EventParticipantDto>
    ): List<com.celdy.groufr.data.events.EventParticipantDto> {
        val order = mapOf(
            "joined" to 0,
            "maybe" to 1,
            "declined" to 2,
            "invited" to 3
        )
        return list.sortedWith(compareBy({ order[it.status] ?: 99 }, { it.user.name.lowercase() }))
    }

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        const val EXTRA_EVENT_ID = "extra_event_id"
        private val DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        private val STATUS_OPTIONS = listOf(
            "offered" to com.celdy.groufr.R.string.event_status_offered,
            "preparing" to com.celdy.groufr.R.string.event_status_preparing,
            "closed" to com.celdy.groufr.R.string.event_status_closed,
            "cancelled" to com.celdy.groufr.R.string.event_status_cancelled
        )
        private val USER_STATUS_OPTIONS = listOf(
            "joined" to com.celdy.groufr.R.string.event_participant_joined,
            "maybe" to com.celdy.groufr.R.string.event_participant_maybe,
            "declined" to com.celdy.groufr.R.string.event_participant_declined
        )
    }
}

private enum class EventSection {
    INFO,
    CHAT,
    PARTICIPANTS
}
