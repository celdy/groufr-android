package com.celdy.groufr.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.databinding.ActivityNotificationsBinding
import com.celdy.groufr.ui.eventdetail.EventDetailActivity
import com.celdy.groufr.ui.groupdetail.GroupDetailActivity
import com.celdy.groufr.ui.login.LoginActivity
import com.celdy.groufr.ui.polldetail.PollDetailActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationsActivity : AppCompatActivity() {
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

        viewModel.loadNotifications()
    }

    private fun handleNotificationClick(notification: com.celdy.groufr.data.notifications.NotificationDto) {
        val groupId = notification.groupId ?: -1L
        val groupName = notification.groupName.orEmpty()
        when (notification.eventType) {
            "event_created", "event_updated", "participant_status_changed" -> {
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
            "poll_created", "poll_closed" -> {
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
            "new_message", "user_joined" -> {
                if (groupId > 0) {
                    val intent = Intent(this, GroupDetailActivity::class.java)
                        .putExtra(GroupDetailActivity.EXTRA_GROUP_ID, groupId)
                        .putExtra(GroupDetailActivity.EXTRA_GROUP_NAME, groupName)
                    startActivity(intent)
                    finish()
                }
            }
        }
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
}
