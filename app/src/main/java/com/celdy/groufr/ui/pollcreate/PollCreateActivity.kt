package com.celdy.groufr.ui.pollcreate

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.celdy.groufr.data.auth.AuthRepository
import com.celdy.groufr.databinding.ActivityPollCreateBinding
import com.celdy.groufr.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class PollCreateActivity : AppCompatActivity() {
    @Inject lateinit var authRepository: AuthRepository
    private lateinit var binding: ActivityPollCreateBinding
    private val viewModel: PollCreateViewModel by viewModels()
    private var groupId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPollCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty()

        binding.pollCreateToolbar.title = if (groupName.isNotBlank()) {
            "$groupName · New poll"
        } else {
            getString(com.celdy.groufr.R.string.poll_create_title)
        }
        binding.pollCreateToolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.pollCreateToolbar.updatePadding(top = systemBars.top)
            binding.root.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        binding.pollAddOption.setOnClickListener {
            addOptionField("")
        }

        binding.pollDeadlineInput.setOnClickListener {
            showDateTimePicker { isoValue ->
                binding.pollDeadlineInput.setText(isoValue)
            }
        }

        binding.pollCreateButton.setOnClickListener {
            viewModel.createPoll(
                groupId = groupId,
                question = binding.pollQuestionInput.text?.toString().orEmpty(),
                description = binding.pollDescriptionInput.text?.toString(),
                multiselect = binding.pollMultiselect.isChecked,
                options = collectOptions(),
                deadlineAt = binding.pollDeadlineInput.text?.toString()
            )
        }

        if (binding.pollOptionsContainer.childCount == 0) {
            addOptionField("")
            addOptionField("")
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                PollCreateState.Idle -> {
                    binding.pollCreateButton.isEnabled = true
                    binding.pollCreateLoading.isVisible = false
                }
                PollCreateState.Sending -> {
                    binding.pollCreateButton.isEnabled = false
                    binding.pollCreateLoading.isVisible = true
                }
                PollCreateState.Success -> {
                    binding.pollCreateButton.isEnabled = true
                    binding.pollCreateLoading.isVisible = false
                    Toast.makeText(this, com.celdy.groufr.R.string.poll_create_success, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is PollCreateState.Error -> {
                    binding.pollCreateButton.isEnabled = true
                    binding.pollCreateLoading.isVisible = false
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    if (!authRepository.isLoggedIn()) {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    private fun showDateTimePicker(onSelected: (String) -> Unit) {
        val now = OffsetDateTime.now().plusDays(1)
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val pickedDate = LocalDate.of(year, month + 1, dayOfMonth)
                val timePicker = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        val pickedTime = LocalTime.of(hourOfDay, minute)
                        val zone = ZoneId.systemDefault()
                        val offset = zone.rules.getOffset(now.toInstant())
                        val dateTime = OffsetDateTime.of(pickedDate, pickedTime, offset)
                        onSelected(dateTime.format(ISO_FORMAT))
                    },
                    now.hour,
                    now.minute,
                    true
                )
                timePicker.show()
            },
            now.year,
            now.monthValue - 1,
            now.dayOfMonth
        )
        datePicker.show()
    }

    private fun addOptionField(value: String) {
        val optionView = layoutInflater.inflate(
            com.celdy.groufr.R.layout.item_poll_option_input,
            binding.pollOptionsContainer,
            false
        )
        val input = optionView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.celdy.groufr.R.id.poll_option_input
        )
        input.setText(value)
        binding.pollOptionsContainer.addView(optionView)
    }

    private fun collectOptions(): List<String> {
        val options = mutableListOf<String>()
        for (index in 0 until binding.pollOptionsContainer.childCount) {
            val view = binding.pollOptionsContainer.getChildAt(index)
            val input = view.findViewById<com.google.android.material.textfield.TextInputEditText>(
                com.celdy.groufr.R.id.poll_option_input
            )
            options.add(input.text?.toString().orEmpty())
        }
        return options
    }

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        private val ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    }
}
