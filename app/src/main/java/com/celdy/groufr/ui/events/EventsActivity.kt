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
import com.celdy.groufr.R
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.databinding.ActivityEventsBinding
import com.celdy.groufr.ui.eventcreate.EventCreateActivity
import com.celdy.groufr.ui.eventdetail.EventDetailActivity
import com.celdy.groufr.ui.groupdetail.GroupDetailActivity
import com.celdy.groufr.ui.login.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        binding.eventsToolbar.title = getString(R.string.events_title)
        setSupportActionBar(binding.eventsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.eventsToolbar.setNavigationOnClickListener { finish() }
        binding.eventsGroupBadge.text = groupName
        binding.eventsGroupBadge.isVisible = groupName.isNotBlank()

        binding.eventsList.layoutManager = LinearLayoutManager(this)
        binding.eventsList.adapter = adapter

        setupFilters()

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
                    binding.eventsEmpty.text = getString(R.string.events_error)
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
            viewModel.loadAllEvents()
        }
    }

    override fun onResume() {
        super.onResume()
        if (groupId > 0) {
            viewModel.loadEvents(groupId)
        } else {
            viewModel.loadAllEvents()
        }
    }

    private fun setupFilters() {
        // Time filter chip - shows dialog on click
        binding.chipTimeFilter.setOnClickListener {
            showTimeFilterDialog()
        }

        // Participation filter chip - shows dialog on click
        binding.chipParticipationFilter.setOnClickListener {
            showParticipationFilterDialog()
        }

        // Update chip text when filter changes
        viewModel.timeFilter.observe(this) { filter ->
            binding.chipTimeFilter.text = getTimeFilterText(filter)
        }

        viewModel.participationFilter.observe(this) { filter ->
            binding.chipParticipationFilter.text = getParticipationFilterText(filter)
        }
    }

    private fun showTimeFilterDialog() {
        val options = arrayOf(
            getString(R.string.events_filter_upcoming),
            getString(R.string.events_filter_past),
            getString(R.string.events_filter_all)
        )
        val filters = arrayOf(TimeFilter.UPCOMING, TimeFilter.PAST, TimeFilter.ALL)
        val currentIndex = filters.indexOf(viewModel.timeFilter.value ?: TimeFilter.UPCOMING)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.events_filter_time_title)
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                viewModel.setTimeFilter(filters[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showParticipationFilterDialog() {
        val options = arrayOf(
            getString(R.string.events_filter_participation_going_maybe),
            getString(R.string.events_filter_participation_unresponded),
            getString(R.string.events_filter_participation_going),
            getString(R.string.events_filter_participation_maybe),
            getString(R.string.events_filter_participation_declined),
            getString(R.string.events_filter_participation_all)
        )
        val filters = arrayOf(
            ParticipationFilter.GOING_AND_MAYBE,
            ParticipationFilter.UNRESPONDED,
            ParticipationFilter.GOING,
            ParticipationFilter.MAYBE,
            ParticipationFilter.DECLINED,
            ParticipationFilter.ALL
        )
        val currentIndex = filters.indexOf(viewModel.participationFilter.value ?: ParticipationFilter.GOING_AND_MAYBE)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.events_filter_participation_title)
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                viewModel.setParticipationFilter(filters[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun getTimeFilterText(filter: TimeFilter): String {
        return when (filter) {
            TimeFilter.UPCOMING -> getString(R.string.events_filter_upcoming)
            TimeFilter.PAST -> getString(R.string.events_filter_past)
            TimeFilter.ALL -> getString(R.string.events_filter_all)
        }
    }

    private fun getParticipationFilterText(filter: ParticipationFilter): String {
        return when (filter) {
            ParticipationFilter.GOING_AND_MAYBE -> getString(R.string.events_filter_participation_going_maybe)
            ParticipationFilter.UNRESPONDED -> getString(R.string.events_filter_participation_unresponded)
            ParticipationFilter.GOING -> getString(R.string.events_filter_participation_going)
            ParticipationFilter.MAYBE -> getString(R.string.events_filter_participation_maybe)
            ParticipationFilter.DECLINED -> getString(R.string.events_filter_participation_declined)
            ParticipationFilter.ALL -> getString(R.string.events_filter_participation_all)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_events, menu)
        menu.findItem(R.id.action_create_event)?.isVisible = groupId > 0
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_create_event)?.isVisible = groupId > 0
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_create_event -> {
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
