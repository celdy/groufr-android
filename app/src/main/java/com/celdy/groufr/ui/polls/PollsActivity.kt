package com.celdy.groufr.ui.polls

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.databinding.ActivityPollsBinding
import com.celdy.groufr.ui.groupdetail.GroupDetailActivity
import com.celdy.groufr.ui.login.LoginActivity
import com.celdy.groufr.ui.polldetail.PollDetailActivity
import com.celdy.groufr.ui.pollcreate.PollCreateActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PollsActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    private lateinit var binding: ActivityPollsBinding
    private val viewModel: PollsViewModel by viewModels()
    private val adapter = PollAdapter { poll ->
        val intent = Intent(this, PollDetailActivity::class.java)
            .putExtra(PollDetailActivity.EXTRA_GROUP_ID, groupId)
            .putExtra(PollDetailActivity.EXTRA_GROUP_NAME, groupName)
            .putExtra(PollDetailActivity.EXTRA_POLL_ID, poll.id)
        startActivity(intent)
    }
    private var groupId: Long = -1L
    private var groupName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPollsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getLongExtra(GroupDetailActivity.EXTRA_GROUP_ID, -1L)
        groupName = intent.getStringExtra(GroupDetailActivity.EXTRA_GROUP_NAME).orEmpty()
        binding.pollsToolbar.title = if (groupName.isNotBlank()) {
            "$groupName · Polls"
        } else {
            getString(com.celdy.groufr.R.string.polls_title)
        }
        binding.pollsToolbar.setNavigationOnClickListener { finish() }

        binding.pollsList.layoutManager = LinearLayoutManager(this)
        binding.pollsList.adapter = adapter

        val listPadding = binding.pollsList.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.pollsToolbar.updatePadding(top = systemBars.top)
            binding.pollsList.updatePadding(bottom = listPadding + systemBars.bottom)
            binding.root.updatePadding(left = systemBars.left, right = systemBars.right)
            insets
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                PollsState.Loading -> {
                    binding.pollsLoading.isVisible = true
                    binding.pollsEmpty.isVisible = false
                }
                is PollsState.Content -> {
                    binding.pollsLoading.isVisible = false
                    adapter.submitList(state.polls)
                    binding.pollsEmpty.isVisible = state.polls.isEmpty()
                }
                PollsState.Error -> {
                    binding.pollsLoading.isVisible = false
                    binding.pollsEmpty.isVisible = true
                    binding.pollsEmpty.text = getString(com.celdy.groufr.R.string.polls_error)
                    if (!authRepository.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }

        if (groupId > 0) {
            viewModel.loadPolls(groupId)
        } else {
            binding.pollsLoading.isVisible = false
            binding.pollsEmpty.isVisible = true
            binding.pollsEmpty.text = getString(com.celdy.groufr.R.string.polls_error)
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(com.celdy.groufr.R.menu.menu_polls, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            com.celdy.groufr.R.id.action_create_poll -> {
                if (groupId > 0) {
                    val intent = Intent(this, PollCreateActivity::class.java)
                        .putExtra(PollCreateActivity.EXTRA_GROUP_ID, groupId)
                        .putExtra(PollCreateActivity.EXTRA_GROUP_NAME, groupName)
                    startActivity(intent)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
