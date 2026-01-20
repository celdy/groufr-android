package com.celdy.groufr.ui.polldetail

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.databinding.ActivityPollDetailBinding
import com.celdy.groufr.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PollDetailActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    private lateinit var binding: ActivityPollDetailBinding
    private val viewModel: PollDetailViewModel by viewModels()
    private val adapter = PollOptionAdapter()
    private var pollId: Long = -1L
    private var groupId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPollDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pollId = intent.getLongExtra(EXTRA_POLL_ID, -1L)
        groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty()

        binding.pollDetailToolbar.title = if (groupName.isNotBlank()) {
            "$groupName · Poll"
        } else {
            getString(com.celdy.groufr.R.string.poll_detail_title)
        }
        binding.pollDetailToolbar.setNavigationOnClickListener { finish() }

        binding.pollOptions.layoutManager = LinearLayoutManager(this)
        binding.pollOptions.adapter = adapter

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.pollDetailToolbar.updatePadding(top = systemBars.top)
            binding.pollDetailContent.updatePadding(bottom = systemBars.bottom)
            binding.root.updatePadding(left = systemBars.left, right = systemBars.right)
            insets
        }

        binding.pollVoteButton.setOnClickListener {
            viewModel.vote(pollId, adapter.selectedOptionIds())
        }

        binding.pollClearButton.setOnClickListener {
            viewModel.clearVote(pollId)
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
                    adapter.multiselect = poll.multiselect
                    adapter.submitList(poll.options)
                    adapter.setSelected(poll.yourVotes)
                    binding.pollMeta.text = "Voters: ${poll.totalVoters}"
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
                    binding.pollClearButton.isEnabled = false
                }
                VoteState.Sent -> {
                    binding.pollVoteButton.isEnabled = true
                    binding.pollClearButton.isEnabled = true
                }
                is VoteState.Error -> {
                    binding.pollVoteButton.isEnabled = true
                    binding.pollClearButton.isEnabled = true
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

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        const val EXTRA_POLL_ID = "extra_poll_id"
    }
}
