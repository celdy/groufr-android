package com.celdy.groufr.ui.groupdetail

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.data.messages.MessageDto
import com.celdy.groufr.data.storage.TokenStore
import com.celdy.groufr.databinding.ActivityGroupDetailBinding
import com.celdy.groufr.data.reports.ReportContentType
import com.celdy.groufr.data.reactions.ReactionContentType
import com.celdy.groufr.ui.common.ReportDialogFragment
import com.celdy.groufr.ui.common.ReactionDialogFragment
import com.celdy.groufr.ui.common.ReactorListDialogFragment
import com.celdy.groufr.ui.eventcreate.EventCreateActivity
import com.celdy.groufr.ui.login.LoginActivity
import com.celdy.groufr.ui.eventdetail.EventDetailActivity
import com.celdy.groufr.ui.polldetail.PollDetailActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class GroupDetailActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var tokenStore: TokenStore
    private lateinit var binding: ActivityGroupDetailBinding
    private val viewModel: GroupDetailViewModel by viewModels()
    private lateinit var adapter: MessageAdapter
    private var groupId: Long = -1L
    private var groupName: String = ""
    private var menuRef: Menu? = null
    private val createEventLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && groupId > 0) {
            viewModel.refresh()
            viewModel.loadBadgeCounts(groupId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
        groupName = intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty()

        binding.groupToolbar.title = groupName
        setSupportActionBar(binding.groupToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.groupToolbar.setNavigationOnClickListener { finish() }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.messagesList.layoutManager = layoutManager
        adapter = MessageAdapter(
            currentUserId = tokenStore.getUserId(),
            groupId = groupId,
            groupName = groupName,
            onEventClick = { eventId, name ->
                val intent = Intent(this, EventDetailActivity::class.java)
                    .putExtra(EventDetailActivity.EXTRA_GROUP_ID, groupId)
                    .putExtra(EventDetailActivity.EXTRA_GROUP_NAME, name)
                    .putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId)
                startActivity(intent)
            },
            onPollClick = { pollId, name ->
                val intent = Intent(this, PollDetailActivity::class.java)
                    .putExtra(PollDetailActivity.EXTRA_GROUP_ID, groupId)
                    .putExtra(PollDetailActivity.EXTRA_GROUP_NAME, name)
                    .putExtra(PollDetailActivity.EXTRA_POLL_ID, pollId)
                startActivity(intent)
            },
            onReactMessage = { message ->
                ReactionDialogFragment.newInstance(
                    ReactionContentType.MESSAGE,
                    message.id,
                    message.reactions?.userReaction
                ).show(supportFragmentManager, ReactionDialogFragment.TAG)
            },
            onReportMessage = { message ->
                ReportDialogFragment.newInstance(ReportContentType.MESSAGE, message.id)
                    .show(supportFragmentManager, ReportDialogFragment.TAG)
            },
            onShowReactors = { message ->
                ReactorListDialogFragment.newInstance(ReactionContentType.MESSAGE, message.id, tokenStore.getUserId())
                    .show(supportFragmentManager, ReactorListDialogFragment.TAG)
            }
        )
        binding.messagesList.adapter = adapter
        binding.messagesList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (lastVisible >= adapter.itemCount - 5) {
                    viewModel.loadMore()
                }
            }
        })

        binding.messagesRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.messageSend.setOnClickListener {
            if (groupId > 0) {
                viewModel.sendMessage(
                    groupId = groupId,
                    body = binding.messageInput.text?.toString().orEmpty()
                )
            }
        }

        binding.messageCreateEvent.setOnClickListener {
            if (groupId > 0) {
                val intent = Intent(this, EventCreateActivity::class.java)
                    .putExtra(EXTRA_GROUP_ID, groupId)
                    .putExtra(EXTRA_GROUP_NAME, groupName)
                createEventLauncher.launch(intent)
            }
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                GroupDetailState.Loading -> {
                    binding.messagesLoading.isVisible = true
                    binding.messagesEmpty.isVisible = false
                }
                is GroupDetailState.Content -> {
                    binding.messagesLoading.isVisible = false
                    val items = buildChatItems(state.messages, state.dividerBeforeMessageId)
                    val shouldScrollToBottom = !binding.messagesList.canScrollVertically(1)
                    adapter.submitList(items)
                    binding.messagesEmpty.isVisible = state.messages.isEmpty()
                    if (shouldScrollToBottom && items.isNotEmpty()) {
                        binding.messagesList.scrollToPosition(items.size - 1)
                    }
                }
                GroupDetailState.Error -> {
                    binding.messagesLoading.isVisible = false
                    binding.messagesEmpty.isVisible = true
                    binding.messagesEmpty.text = getString(com.celdy.groufr.R.string.messages_error)
                    if (!authRepository.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }

        viewModel.sendState.observe(this) { state ->
            when (state) {
                SendState.Idle -> Unit
                SendState.Sending -> {
                    binding.messageSend.isEnabled = false
                }
                SendState.Sent -> {
                    binding.messageSend.isEnabled = true
                    binding.messageInput.text?.clear()
                }
                is SendState.Error -> {
                    binding.messageSend.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.badgeCounts.observe(this) { counts ->
            updateMenuBadges(counts)
        }

        viewModel.refreshing.observe(this) { refreshing ->
            binding.messagesRefresh.isRefreshing = refreshing
        }

        supportFragmentManager.setFragmentResultListener(
            ReactionDialogFragment.RESULT_KEY, this
        ) { _, bundle ->
            if (bundle.getBoolean(ReactionDialogFragment.RESULT_CHANGED, false)) {
                viewModel.refresh()
            }
        }

        if (groupId > 0) {
            viewModel.loadMessages(groupId)
            viewModel.loadBadgeCounts(groupId)
        } else {
            binding.messagesLoading.isVisible = false
            binding.messagesEmpty.isVisible = true
            binding.messagesEmpty.text = getString(com.celdy.groufr.R.string.messages_error)
        }

        val listPadding = binding.messagesList.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = max(systemBars.bottom, imeInsets.bottom)
            binding.groupToolbar.updatePadding(top = systemBars.top)
            binding.messagesList.updatePadding(bottom = listPadding)
            binding.messageComposer.updatePadding(bottom = bottomInset)
            binding.root.updatePadding(
                left = systemBars.left,
                right = systemBars.right
            )
            insets
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(com.celdy.groufr.R.menu.menu_group_detail, menu)
        menuRef = menu
        updateMenuBadges(viewModel.badgeCounts.value ?: GroupBadgeCounts())
        menu.findItem(com.celdy.groufr.R.id.action_polls)?.actionView?.setOnClickListener {
            onOptionsItemSelected(menu.findItem(com.celdy.groufr.R.id.action_polls))
        }
        menu.findItem(com.celdy.groufr.R.id.action_events)?.actionView?.setOnClickListener {
            onOptionsItemSelected(menu.findItem(com.celdy.groufr.R.id.action_events))
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            com.celdy.groufr.R.id.action_polls -> {
                if (groupId > 0) {
                    val intent = Intent(this, com.celdy.groufr.ui.polls.PollsActivity::class.java)
                        .putExtra(EXTRA_GROUP_ID, groupId)
                        .putExtra(EXTRA_GROUP_NAME, groupName)
                    startActivity(intent)
                }
                true
            }
            com.celdy.groufr.R.id.action_events -> {
                if (groupId > 0) {
                    val intent = Intent(this, com.celdy.groufr.ui.events.EventsActivity::class.java)
                        .putExtra(EXTRA_GROUP_ID, groupId)
                        .putExtra(EXTRA_GROUP_NAME, groupName)
                    startActivity(intent)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateMenuBadges(counts: GroupBadgeCounts) {
        val menu = menuRef ?: return
        updateBadge(menu.findItem(com.celdy.groufr.R.id.action_polls), counts.openPolls)
        updateBadge(menu.findItem(com.celdy.groufr.R.id.action_events), counts.activeEvents)
    }

    private fun updateBadge(item: MenuItem?, count: Int) {
        val actionView = item?.actionView ?: return
        val badgeView = actionView.findViewById<android.widget.TextView>(com.celdy.groufr.R.id.menu_badge)
        if (count > 0) {
            badgeView.text = count.toString()
            badgeView.visibility = android.view.View.VISIBLE
        } else {
            badgeView.visibility = android.view.View.GONE
        }
    }

    private fun buildChatItems(
        messages: List<MessageDto>,
        dividerBeforeMessageId: Long?
    ): List<GroupChatItem> {
        if (messages.isEmpty() || dividerBeforeMessageId == null) {
            return messages.map { GroupChatItem.Message(it) }
        }
        val items = mutableListOf<GroupChatItem>()
        for (message in messages) {
            if (message.id == dividerBeforeMessageId) {
                items.add(GroupChatItem.Divider)
            }
            items.add(GroupChatItem.Message(message))
        }
        return items
    }

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
    }
}
