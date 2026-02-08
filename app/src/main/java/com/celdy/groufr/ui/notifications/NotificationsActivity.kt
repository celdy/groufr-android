package com.celdy.groufr.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.celdy.groufr.R
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.data.notifications.NotificationDto
import com.celdy.groufr.data.notifications.eventIdFromPayload
import com.celdy.groufr.data.notifications.invitationTokenFromPayload
import com.celdy.groufr.data.notifications.invitedGroupNameFromPayload
import com.celdy.groufr.databinding.ActivityNotificationsBinding
import com.celdy.groufr.ui.eventdetail.EventDetailActivity
import com.celdy.groufr.ui.groupdetail.GroupDetailActivity
import com.celdy.groufr.ui.login.LoginActivity
import com.celdy.groufr.ui.polldetail.PollDetailActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationsActivity : AppCompatActivity() {
    companion object {
        const val RESULT_GROUPS_CHANGED = 1001
    }

    private var groupsChanged = false
    @Inject lateinit var authRepository: AuthRepository
    private lateinit var binding: ActivityNotificationsBinding
    private val viewModel: NotificationsViewModel by viewModels()
    private val adapter = NotificationsAdapter { notification ->
        if (!notification.isRead) {
            viewModel.markRead(notification.id)
        }
        handleNotificationClick(notification)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.notificationsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.notificationsToolbar.setNavigationOnClickListener { finish() }
        binding.notificationsList.layoutManager = LinearLayoutManager(this)
        binding.notificationsList.adapter = adapter

        val listPadding = binding.notificationsList.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.notificationsToolbar.updatePadding(top = systemBars.top)
            binding.notificationsList.updatePadding(bottom = listPadding + systemBars.bottom)
            binding.root.updatePadding(left = systemBars.left, right = systemBars.right)
            insets
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                NotificationsState.Loading -> {
                    binding.notificationsLoading.isVisible = true
                    binding.notificationsEmpty.isVisible = false
                }
                is NotificationsState.Content -> {
                    binding.notificationsLoading.isVisible = false
                    adapter.submitList(state.notifications)
                    binding.notificationsEmpty.isVisible = state.notifications.isEmpty()
                }
                NotificationsState.Error -> {
                    binding.notificationsLoading.isVisible = false
                    binding.notificationsEmpty.isVisible = true
                    binding.notificationsEmpty.text = getString(com.celdy.groufr.R.string.notifications_error)
                    if (!authRepository.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }

        viewModel.invitationResult.observe(this) { result ->
            when (result) {
                InvitationResult.Accepted -> {
                    Toast.makeText(this, R.string.invitation_accepted, Toast.LENGTH_SHORT).show()
                    groupsChanged = true
                    viewModel.clearInvitationResult()
                }
                InvitationResult.Declined -> {
                    Toast.makeText(this, R.string.invitation_declined, Toast.LENGTH_SHORT).show()
                    viewModel.clearInvitationResult()
                }
                InvitationResult.Error -> {
                    Toast.makeText(this, R.string.invitation_error, Toast.LENGTH_SHORT).show()
                    viewModel.clearInvitationResult()
                }
                null -> { /* no-op */ }
            }
        }

        viewModel.loadNotifications()
    }

    private fun handleNotificationClick(notification: NotificationDto) {
        val groupId = notification.groupId ?: -1L
        val groupName = notification.groupName.orEmpty()
        when (notification.eventType) {
            "event_created", "event_updated", "participant_status_changed", "reaction_event" -> {
                val eventId = notification.entityId ?: -1L
                if (eventId > 0 && groupId > 0) {
                    val intent = Intent(this, EventDetailActivity::class.java)
                        .putExtra(EventDetailActivity.EXTRA_GROUP_ID, groupId)
                        .putExtra(EventDetailActivity.EXTRA_GROUP_NAME, groupName)
                        .putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId)
                    startActivity(intent)
                    finish()
                }
            }
            "poll_created", "poll_closed", "reaction_poll" -> {
                val pollId = notification.entityId ?: -1L
                if (pollId > 0 && groupId > 0) {
                    val intent = Intent(this, PollDetailActivity::class.java)
                        .putExtra(PollDetailActivity.EXTRA_GROUP_ID, groupId)
                        .putExtra(PollDetailActivity.EXTRA_GROUP_NAME, groupName)
                        .putExtra(PollDetailActivity.EXTRA_POLL_ID, pollId)
                    startActivity(intent)
                    finish()
                }
            }
            "invitation_received" -> {
                showInvitationDialog(notification)
            }
            "new_message", "user_joined", "reaction_message" -> {
                if (notification.eventType == "new_message" || notification.eventType == "reaction_message") {
                    val eventId = notification.eventIdFromPayload() ?: -1L
                    if (eventId > 0) {
                        val intent = Intent(this, EventDetailActivity::class.java)
                            .putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId)
                            .putExtra(EventDetailActivity.EXTRA_GROUP_NAME, groupName)
                            .putExtra(EventDetailActivity.EXTRA_SHOW_CHAT, true)
                        startActivity(intent)
                        finish()
                        return
                    }
                }
                if (groupId > 0) {
                    val intent = Intent(this, GroupDetailActivity::class.java)
                        .putExtra(GroupDetailActivity.EXTRA_GROUP_ID, groupId)
                        .putExtra(GroupDetailActivity.EXTRA_GROUP_NAME, groupName)
                        .putExtra(GroupDetailActivity.EXTRA_GROUP_SLUG, "")
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun showInvitationDialog(notification: NotificationDto) {
        val token = notification.invitationTokenFromPayload()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, R.string.invitation_error, Toast.LENGTH_SHORT).show()
            return
        }

        val actor = notification.actor?.name ?: "Someone"
        val invitedGroupName = notification.invitedGroupNameFromPayload() ?: "a group"

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.invitation_dialog_title)
            .setMessage(getString(R.string.invitation_dialog_message, actor, invitedGroupName))
            .setPositiveButton(R.string.invitation_accept) { dialog, _ ->
                viewModel.acceptInvitation(token, notification.id)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.invitation_decline) { dialog, _ ->
                viewModel.declineInvitation(token, notification.id)
                dialog.dismiss()
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(com.celdy.groufr.R.menu.menu_notifications, menu)
        val unreadItem = menu.findItem(com.celdy.groufr.R.id.action_unread_only)
        unreadItem.isChecked = viewModel.currentUnreadOnly()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            com.celdy.groufr.R.id.action_unread_only -> {
                val enabled = viewModel.toggleUnreadOnly()
                item.isChecked = enabled
                true
            }
            com.celdy.groufr.R.id.action_mark_all_read -> {
                viewModel.markAllRead()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun finish() {
        if (groupsChanged) {
            setResult(RESULT_GROUPS_CHANGED)
        }
        super.finish()
    }
}
