package com.celdy.groufr.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.celdy.groufr.BuildConfig
import com.celdy.groufr.R
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.data.calendar.CalendarSyncManager
import com.celdy.groufr.data.calendar.CalendarSyncStore
import com.celdy.groufr.data.device.PushTokenManager
import com.celdy.groufr.data.local.UserDao
import com.celdy.groufr.data.storage.TokenStore
import com.celdy.groufr.databinding.ActivityProfileBinding
import com.celdy.groufr.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var userDao: UserDao
    @Inject lateinit var calendarSyncManager: CalendarSyncManager
    @Inject lateinit var pushTokenManager: PushTokenManager
    @Inject lateinit var calendarSyncStore: CalendarSyncStore
    private lateinit var binding: ActivityProfileBinding

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            tryEnableSync()
        } else {
            setCalendarSwitchWithoutListener(false)
            Toast.makeText(this, R.string.profile_calendar_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.profileToolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.profileToolbar.updatePadding(top = systemBars.top)
            binding.root.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        val name = tokenStore.getUserName().orEmpty()
        binding.profileName.text = name
        binding.profileAvatar.text = name.firstOrNull()?.uppercase().orEmpty()

        lifecycleScope.launch {
            val user = userDao.getById(tokenStore.getUserId())
            user?.email?.let { binding.profileEmail.text = it }
        }

        binding.profileVersionValue.text = BuildConfig.VERSION_NAME

        binding.profileWebsiteValue.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://groufr.com")))
        }

        binding.profileDeveloperWebValue.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://celdy.com")))
        }

        // Calendar sync toggle
        binding.profileCalendarSwitch.isChecked = calendarSyncStore.isEnabled()
        binding.profileCalendarSwitch.setOnCheckedChangeListener(calendarSwitchListener)

        binding.profileSignoutCard.setOnClickListener {
            lifecycleScope.launch {
                pushTokenManager.unregisterToken()
                calendarSyncManager.disableSync()
                authRepository.clearTokens()
                startActivity(Intent(this@ProfileActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun onCalendarSwitchChanged(isChecked: Boolean) {
        if (isChecked) {
            if (hasCalendarPermissions()) {
                tryEnableSync()
            } else {
                calendarPermissionLauncher.launch(
                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                )
            }
        } else {
            calendarSyncManager.disableSync()
        }
    }

    private fun tryEnableSync() {
        val success = calendarSyncManager.enableSync()
        if (!success) {
            setCalendarSwitchWithoutListener(false)
            Toast.makeText(this, R.string.profile_calendar_sync_error, Toast.LENGTH_LONG).show()
        }
    }

    private val calendarSwitchListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked -> onCalendarSwitchChanged(isChecked) }

    private fun setCalendarSwitchWithoutListener(checked: Boolean) {
        binding.profileCalendarSwitch.setOnCheckedChangeListener(null)
        binding.profileCalendarSwitch.isChecked = checked
        binding.profileCalendarSwitch.setOnCheckedChangeListener(calendarSwitchListener)
    }

    private fun hasCalendarPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }
}
