package com.celdy.groufr.ui.eventcreate

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.databinding.ActivityEventCreateBinding
import com.celdy.groufr.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class EventCreateActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    private lateinit var binding: ActivityEventCreateBinding
    private val viewModel: EventCreateViewModel by viewModels()
    private var groupId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty()

        binding.eventCreateToolbar.title = if (groupName.isNotBlank()) {
            "$groupName - New event"
        } else {
            getString(com.celdy.groufr.R.string.event_create_title)
        }
        binding.eventCreateToolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.eventCreateToolbar.updatePadding(top = systemBars.top)
            binding.root.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        binding.eventStartInput.setOnClickListener {
            showDateTimePicker(binding.eventStartInput.text?.toString()) { isoValue ->
                binding.eventStartInput.setText(isoValue)
            }
        }

        binding.eventEndInput.setOnClickListener {
            showDateTimePicker(binding.eventEndInput.text?.toString()) { isoValue ->
                binding.eventEndInput.setText(isoValue)
            }
        }

        binding.eventDeadlineInput.setOnClickListener {
            showDateTimePicker(binding.eventDeadlineInput.text?.toString()) { isoValue ->
                binding.eventDeadlineInput.setText(isoValue)
            }
        }

        binding.eventCreateButton.setOnClickListener {
            viewModel.createEvent(
                groupId = groupId,
                title = binding.eventTitleInput.text?.toString().orEmpty(),
                description = binding.eventDescriptionInput.text?.toString(),
                startAt = binding.eventStartInput.text?.toString().orEmpty(),
                endAt = binding.eventEndInput.text?.toString(),
                deadlineJoinAt = binding.eventDeadlineInput.text?.toString()
            )
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                CreateState.Idle -> {
                    binding.eventCreateButton.isEnabled = true
                    binding.eventCreateLoading.isVisible = false
                }
                CreateState.Sending -> {
                    binding.eventCreateButton.isEnabled = false
                    binding.eventCreateLoading.isVisible = true
                }
                CreateState.Success -> {
                    binding.eventCreateButton.isEnabled = true
                    binding.eventCreateLoading.isVisible = false
                    Toast.makeText(this, com.celdy.groufr.R.string.event_create_success, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is CreateState.Error -> {
                    binding.eventCreateButton.isEnabled = true
                    binding.eventCreateLoading.isVisible = false
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    if (!authRepository.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    private fun showDateTimePicker(initialValue: String?, onSelected: (String) -> Unit) {
        val initialDateTime = parseInitialDateTime(initialValue)
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val pickedDate = LocalDate.of(year, month + 1, dayOfMonth)
                val timePicker = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        val pickedTime = LocalTime.of(hourOfDay, minute)
                        val zone = ZoneId.systemDefault()
                        val offset = zone.rules.getOffset(initialDateTime.toInstant())
                        val dateTime = OffsetDateTime.of(pickedDate, pickedTime, offset)
                        onSelected(dateTime.format(ISO_FORMAT))
                    },
                    initialDateTime.hour,
                    initialDateTime.minute,
                    true
                )
                timePicker.show()
            },
            initialDateTime.year,
            initialDateTime.monthValue - 1,
            initialDateTime.dayOfMonth
        )
        datePicker.show()
    }

    private fun parseInitialDateTime(value: String?): OffsetDateTime {
        val trimmed = value?.trim().orEmpty()
        return if (trimmed.isBlank()) {
            OffsetDateTime.now()
        } else {
            try {
                OffsetDateTime.parse(trimmed)
            } catch (exception: Exception) {
                OffsetDateTime.now()
            }
        }
    }

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        private val ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    }
}





