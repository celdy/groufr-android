package com.celdy.groufr.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.celdy.groufr.R
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.databinding.ActivityMainBinding
import com.celdy.groufr.ui.events.EventsActivity
import com.celdy.groufr.ui.groupdetail.GroupDetailActivity
import com.celdy.groufr.ui.notifications.NotificationsActivity
import com.celdy.groufr.ui.login.LoginActivity
import com.celdy.groufr.ui.profile.ProfileActivity
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val adapter = GroupAdapter { group ->
        val intent = Intent(this, GroupDetailActivity::class.java).apply {
            putExtra(GroupDetailActivity.EXTRA_GROUP_ID, group.id)
            putExtra(GroupDetailActivity.EXTRA_GROUP_NAME, group.name)
        }
        startActivity(intent)
    }

    private val notificationsBadge: BadgeDrawable by lazy {
        BadgeDrawable.create(this).apply {
            setTextAppearance(R.style.NotificationBadge)
            verticalOffset = 12
            horizontalOffset = 12
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val notificationsActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == NotificationsActivity.RESULT_GROUPS_CHANGED) {
            viewModel.loadGroups()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.groupsList.layoutManager = LinearLayoutManager(this)
        binding.groupsList.adapter = adapter

        binding.mainToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_notifications -> {
                    notificationsActivityLauncher.launch(Intent(this, NotificationsActivity::class.java))
                    true
                }
                R.id.action_events -> {
                    startActivity(Intent(this, EventsActivity::class.java))
                    true
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        val listPadding = binding.groupsList.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.mainToolbar.updatePadding(top = systemBars.top)
            binding.groupsList.updatePadding(bottom = listPadding + systemBars.bottom)
            binding.root.updatePadding(
                left = systemBars.left,
                right = systemBars.right
            )
            insets
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                MainState.Loading -> {
                    binding.groupsLoading.isVisible = true
                    binding.groupsEmpty.isVisible = false
                }
                is MainState.Content -> {
                    binding.groupsLoading.isVisible = false
                    val sorted = state.groups.sortedByDescending { it.lastActivityAt }
                    adapter.submitList(sorted)
                    binding.groupsEmpty.isVisible = state.groups.isEmpty()
                    binding.groupsEmpty.text = getString(R.string.groups_empty)
                }
                MainState.Error -> {
                    binding.groupsLoading.isVisible = false
                    binding.groupsEmpty.isVisible = true
                    binding.groupsEmpty.text = getString(R.string.groups_error)
                    if (!authRepository.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }

        viewModel.unreadCount.observe(this) { count ->
            showNotificationsBadge(count)
        }

        viewModel.loadGroups()
        requestNotificationPermissions()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val isValid = authRepository.ensureValidSession()
            if (!isValid) {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            } else {
                viewModel.loadUnreadCount()
            }
        }
    }

    @androidx.annotation.OptIn(ExperimentalBadgeUtils::class)
    private fun showNotificationsBadge(count: Int) {
        if (count > 0) {
            notificationsBadge.number = count
            BadgeUtils.attachBadgeDrawable(notificationsBadge, binding.mainToolbar, R.id.action_notifications)
        } else {
            BadgeUtils.detachBadgeDrawable(notificationsBadge, binding.mainToolbar, R.id.action_notifications)
        }
    }

    private fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
