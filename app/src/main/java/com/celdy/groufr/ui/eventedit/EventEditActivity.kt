package com.celdy.groufr.ui.eventedit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.databinding.ActivityEventEditBinding
import com.celdy.groufr.ui.common.ChatDateFormatter
import com.celdy.groufr.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class EventEditActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    private lateinit var binding: ActivityEventEditBinding
    private val viewModel: EventEditViewModel by viewModels()
    private var eventId: Long = -1L
    private var currentState: String = "offered"
    private var hasBoundEvent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty()

        binding.eventEditToolbar.title = if (groupName.isNotBlank()) {
            "$groupName - ${getString(com.celdy.groufr.R.string.event_edit_title)}"
        } else {
            getString(com.celdy.groufr.R.string.event_edit_title)
        }
        binding.eventEditToolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.eventEditToolbar.updatePadding(top = systemBars.top)
            binding.root.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        binding.eventStartInput.setOnClickListener {
            showDateTimePicker(binding.eventStartInput.tag as? String) { isoValue ->
                setDateField(binding.eventStartInput, isoValue)
            }
        }

        binding.eventEndInput.setOnClickListener {
            showDateTimePicker(binding.eventEndInput.tag as? String) { isoValue ->
                setDateField(binding.eventEndInput, isoValue)
            }
        }

        binding.eventDeadlineInput.setOnClickListener {
            showDateTimePicker(binding.eventDeadlineInput.tag as? String) { isoValue ->
                setDateField(binding.eventDeadlineInput, isoValue)
            }
        }

        binding.eventStateInput.setOnClickListener { showStateDialog() }
        binding.eventStateLayout.setOnClickListener { showStateDialog() }

        binding.eventEditButton.setOnClickListener {
            viewModel.updateEvent(
                eventId = eventId,
                title = binding.eventTitleInput.text?.toString().orEmpty(),
                description = binding.eventDescriptionInput.text?.toString(),
                place = binding.eventPlaceInput.text?.toString(),
                startAt = getDateIso(binding.eventStartInput),
                endAt = getOptionalDateIso(binding.eventEndInput),
                deadlineJoinAt = getOptionalDateIso(binding.eventDeadlineInput),
                minParticipants = binding.eventMinParticipantsInput.text?.toString(),
                maxParticipants = binding.eventMaxParticipantsInput.text?.toString(),
                state = currentState
            )
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                EventEditState.Loading -> {
                    binding.eventEditButton.isEnabled = false
                    binding.eventEditLoading.isVisible = true
                }
                is EventEditState.Content -> {
                    binding.eventEditButton.isEnabled = true
                    binding.eventEditLoading.isVisible = false
                    if (!hasBoundEvent) {
                        bindEvent(state.event)
                        hasBoundEvent = true
                    }
                }
                EventEditState.Saving -> {
                    binding.eventEditButton.isEnabled = false
                    binding.eventEditLoading.isVisible = true
                }
                EventEditState.Success -> {
                    binding.eventEditButton.isEnabled = true
                    binding.eventEditLoading.isVisible = false
                    Toast.makeText(this, com.celdy.groufr.R.string.event_edit_success, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is EventEditState.Error -> {
                    binding.eventEditButton.isEnabled = true
                    binding.eventEditLoading.isVisible = false
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    if (!authRepository.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }

        if (eventId > 0) {
            viewModel.loadEvent(eventId)
        } else {
            Toast.makeText(this, com.celdy.groufr.R.string.event_detail_error, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun bindEvent(event: com.celdy.groufr.data.events.EventDetailDto) {
        binding.eventTitleInput.setText(event.title)
        binding.eventDescriptionInput.setText(event.description.orEmpty())
        binding.eventPlaceInput.setText(event.place.orEmpty())
        setDateField(binding.eventStartInput, event.startAt)
        setDateField(binding.eventEndInput, event.endAt)
        setDateField(binding.eventDeadlineInput, event.deadlineJoinAt)
        binding.eventMinParticipantsInput.setText(event.minParticipants?.toString().orEmpty())
        binding.eventMaxParticipantsInput.setText(event.maxParticipants?.toString().orEmpty())
        currentState = event.state
        binding.eventStateInput.setText(formatEventStatus(event.state))
    }

    private fun showStateDialog() {
        val entries = STATUS_OPTIONS.map { getString(it.second) }.toTypedArray()
        val currentIndex = STATUS_OPTIONS.indexOfFirst { it.first == currentState }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(com.celdy.groufr.R.string.event_status_dialog_title)
            .setSingleChoiceItems(entries, currentIndex) { dialog, which ->
                currentState = STATUS_OPTIONS[which].first
                binding.eventStateInput.setText(getString(STATUS_OPTIONS[which].second))
                dialog.dismiss()
            }
            .show()
    }

    private fun formatEventStatus(status: String): String {
        return when (status) {
            "offered" -> getString(com.celdy.groufr.R.string.event_status_offered)
            "preparing" -> getString(com.celdy.groufr.R.string.event_status_preparing)
            "closed" -> getString(com.celdy.groufr.R.string.event_status_closed)
            "cancelled" -> getString(com.celdy.groufr.R.string.event_status_cancelled)
            else -> status
        }
    }

    private fun setDateField(field: android.widget.TextView, isoValue: String?) {
        field.tag = isoValue
        field.text = formatDisplayDate(isoValue)
    }

    private fun formatDisplayDate(isoValue: String?): String {
        if (isoValue.isNullOrBlank()) return ""
        return ChatDateFormatter.formatRange(isoValue, null, currentLocale()).orEmpty()
    }

    private fun getDateIso(field: android.widget.TextView): String {
        return (field.tag as? String).orEmpty()
    }

    private fun getOptionalDateIso(field: android.widget.TextView): String? {
        val value = field.tag as? String
        return value?.trim()?.ifBlank { null }
    }

    private fun currentLocale(): Locale {
        val locales = resources.configuration.locales
        return if (locales.isEmpty) Locale.getDefault() else locales[0]
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
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        private val ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        private val STATUS_OPTIONS = listOf(
            "offered" to com.celdy.groufr.R.string.event_status_offered,
            "preparing" to com.celdy.groufr.R.string.event_status_preparing,
            "closed" to com.celdy.groufr.R.string.event_status_closed,
            "cancelled" to com.celdy.groufr.R.string.event_status_cancelled
        )
    }
}
