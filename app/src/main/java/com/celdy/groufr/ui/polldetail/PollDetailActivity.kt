package com.celdy.groufr.ui.polldetail

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.data.reactions.ReactionContentType
import com.celdy.groufr.databinding.ActivityPollDetailBinding
import com.celdy.groufr.ui.common.ChatDateFormatter
import com.celdy.groufr.ui.common.ReactionDialogFragment
import com.celdy.groufr.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PollDetailActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    private lateinit var binding: ActivityPollDetailBinding
    private val viewModel: PollDetailViewModel by viewModels()
    private val adapter = PollOptionAdapter { option ->
        showVotersDialog(option)
    }
    private var pollId: Long = -1L
    private var groupId: Long = -1L
    private var originalVotes: Set<Long> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPollDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pollId = intent.getLongExtra(EXTRA_POLL_ID, -1L)
        groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty()

        binding.pollGroupName.text = groupName
        binding.pollGroupName.isVisible = groupName.isNotBlank()
        setSupportActionBar(binding.pollDetailToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.pollDetailToolbar.setNavigationOnClickListener { finish() }

        binding.pollOptions.layoutManager = LinearLayoutManager(this)
        binding.pollOptions.adapter = adapter
        adapter.setOnSelectionChanged { selection ->
            updateVoteButton(selection)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.pollDetailToolbar.updatePadding(top = systemBars.top)
            binding.pollDetailContent.updatePadding(bottom = systemBars.bottom)
            binding.root.updatePadding(left = systemBars.left, right = systemBars.right)
            insets
        }

        binding.pollVoteButton.setOnClickListener {
            val selection = adapter.selectedOptionIds()
            if (selection.isEmpty()) {
                viewModel.clearVote(pollId)
            } else {
                viewModel.vote(pollId, selection)
            }
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                PollDetailState.Loading -> {
                    binding.pollDetailLoading.isVisible = true
                    binding.pollDetailContent.isVisible = false
                    binding.pollDetailError.isVisible = false
                }
                is PollDetailState.Content -> {
                    val poll = state.poll
                    binding.pollDetailLoading.isVisible = false
                    binding.pollDetailContent.isVisible = true
                    binding.pollDetailError.isVisible = false
                    binding.pollQuestion.text = poll.question
                    binding.pollDescription.text = poll.description.orEmpty()
                    binding.pollState.text = formatStatus(poll.status)
                    adapter.multiselect = poll.multiselect
                    adapter.totalVotes = if (poll.totalVotes > 0) poll.totalVotes else poll.totalVoters
                    adapter.submitList(poll.options)
                    adapter.setSelected(poll.yourVotes)
                    originalVotes = poll.yourVotes.toSet()
                    updateVoteButton(originalVotes)
                    binding.pollVotes.text = getString(com.celdy.groufr.R.string.poll_votes_format, poll.totalVoters)
                    val deadlineText = formatDeadline(poll.deadlineAt)
                    binding.pollDeadline.text = deadlineText
                    val showDeadline = deadlineText.isNotBlank()
                    binding.pollDeadline.isVisible = showDeadline
                    binding.pollDeadlineIcon.isVisible = showDeadline
                }
                PollDetailState.Error -> {
                    binding.pollDetailLoading.isVisible = false
                    binding.pollDetailContent.isVisible = false
                    binding.pollDetailError.isVisible = true
                    if (!authRepository.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }

        viewModel.voteState.observe(this) { state ->
            when (state) {
                VoteState.Idle -> Unit
                VoteState.Sending -> {
                    binding.pollVoteButton.isEnabled = false
                }
                VoteState.Sent -> {
                    binding.pollVoteButton.isEnabled = true
                    originalVotes = adapter.selectedOptionIds().toSet()
                    updateVoteButton(originalVotes)
                }
                is VoteState.Error -> {
                    binding.pollVoteButton.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (groupId > 0 && pollId > 0) {
            viewModel.loadPoll(groupId, pollId)
        } else {
            binding.pollDetailLoading.isVisible = false
            binding.pollDetailContent.isVisible = false
            binding.pollDetailError.isVisible = true
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(com.celdy.groufr.R.menu.menu_poll_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            com.celdy.groufr.R.id.action_poll_react -> {
                if (pollId > 0) {
                    ReactionDialogFragment.newInstance(ReactionContentType.POLL, pollId)
                        .show(supportFragmentManager, ReactionDialogFragment.TAG)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun formatStatus(status: String): String {
        return when (status.lowercase()) {
            "open" -> getString(com.celdy.groufr.R.string.poll_status_open)
            "closed" -> getString(com.celdy.groufr.R.string.poll_status_closed)
            else -> status
        }
    }

    private fun formatDeadline(deadline: String?): String {
        if (deadline.isNullOrBlank()) return ""
        val locale = currentLocale()
        return ChatDateFormatter.formatAbsolute(deadline, locale)
    }

    private fun currentLocale(): Locale {
        val locales = resources.configuration.locales
        return if (locales.isEmpty) Locale.getDefault() else locales[0]
    }

    private fun updateVoteButton(selection: Set<Long>) {
        val hasVoted = originalVotes.isNotEmpty()
        binding.pollVoteButton.text = if (hasVoted) {
            getString(com.celdy.groufr.R.string.poll_vote_change_button)
        } else {
            getString(com.celdy.groufr.R.string.poll_vote_button)
        }
        val sameSelection = selection == originalVotes
        binding.pollVoteButton.isEnabled = if (hasVoted) {
            !sameSelection
        } else {
            selection.isNotEmpty()
        }
    }

    private fun showVotersDialog(option: com.celdy.groufr.data.polls.PollOptionDto) {
        val voters = option.voters.map { it.name }
        val title = option.label.takeIf { it.isNotBlank() }
            ?: getString(com.celdy.groufr.R.string.poll_voters_title)
        val builder = AlertDialog.Builder(this)
            .setTitle(title)
        if (voters.isEmpty()) {
            builder.setMessage(com.celdy.groufr.R.string.poll_voters_empty)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        builder.setItems(voters.toTypedArray(), null)
            .show()
    }

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        const val EXTRA_POLL_ID = "extra_poll_id"
    }
}
