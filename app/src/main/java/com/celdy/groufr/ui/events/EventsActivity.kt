package com.celdy.groufr.ui.events

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
import com.celdy.groufr.databinding.ActivityEventsBinding
import com.celdy.groufr.ui.eventcreate.EventCreateActivity
import com.celdy.groufr.ui.eventdetail.EventDetailActivity
import com.celdy.groufr.ui.groupdetail.GroupDetailActivity
import com.celdy.groufr.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EventsActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    private lateinit var binding: ActivityEventsBinding
    private val viewModel: EventsViewModel by viewModels()
    private val adapter = EventAdapter { event ->
        val resolvedGroupName = if (groupName.isNotBlank()) groupName else (event.group?.name ?: "")
        val intent = Intent(this, EventDetailActivity::class.java)
            .putExtra(EventDetailActivity.EXTRA_GROUP_ID, event.groupId)
            .putExtra(EventDetailActivity.EXTRA_GROUP_NAME, resolvedGroupName)
            .putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.id)
        startActivity(intent)
    }
    private var groupId: Long = -1L
    private var groupName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getLongExtra(GroupDetailActivity.EXTRA_GROUP_ID, -1L)
        groupName = intent.getStringExtra(GroupDetailActivity.EXTRA_GROUP_NAME).orEmpty()
        binding.eventsToolbar.title = getString(com.celdy.groufr.R.string.events_title)
        binding.eventsToolbar.setNavigationOnClickListener { finish() }
        binding.eventsGroupBadge.text = groupName
        binding.eventsGroupBadge.isVisible = groupName.isNotBlank()

        binding.eventsList.layoutManager = LinearLayoutManager(this)
        binding.eventsList.adapter = adapter

        val listPadding = binding.eventsList.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.eventsToolbar.updatePadding(top = systemBars.top)
            binding.eventsList.updatePadding(bottom = listPadding + systemBars.bottom)
            binding.root.updatePadding(left = systemBars.left, right = systemBars.right)
            insets
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                EventsState.Loading -> {
                    binding.eventsLoading.isVisible = true
                    binding.eventsEmpty.isVisible = false
                }
                is EventsState.Content -> {
                    binding.eventsLoading.isVisible = false
                    adapter.submitList(state.events)
                    binding.eventsEmpty.isVisible = state.events.isEmpty()
                }
                EventsState.Error -> {
                    binding.eventsLoading.isVisible = false
                    binding.eventsEmpty.isVisible = true
                    binding.eventsEmpty.text = getString(com.celdy.groufr.R.string.events_error)
                    if (!authRepository.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }

        if (groupId > 0) {
            viewModel.loadEvents(groupId)
        } else {
            viewModel.loadAllFutureEvents()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(com.celdy.groufr.R.menu.menu_events, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            com.celdy.groufr.R.id.action_create_event -> {
                if (groupId > 0) {
                    val intent = Intent(this, EventCreateActivity::class.java)
                        .putExtra(EventCreateActivity.EXTRA_GROUP_ID, groupId)
                        .putExtra(EventCreateActivity.EXTRA_GROUP_NAME, groupName)
                    startActivity(intent)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
