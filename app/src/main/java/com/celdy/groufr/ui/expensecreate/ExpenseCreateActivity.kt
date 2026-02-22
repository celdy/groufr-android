package com.celdy.groufr.ui.expensecreate

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.celdy.groufr.R
import com.celdy.groufr.databinding.ActivityExpenseCreateBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExpenseCreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExpenseCreateBinding
    private val viewModel: ExpenseCreateViewModel by viewModels()
    private var eventId: Long = -1L
    private var expenseId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val editId = intent.getLongExtra(EXTRA_EXPENSE_ID, -1L)
        expenseId = if (editId > 0) editId else null

        setSupportActionBar(binding.expenseToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.expenseToolbar.setNavigationOnClickListener { finish() }

        binding.expenseToolbarTitle.text = if (expenseId != null) {
            getString(R.string.expense_edit_title)
        } else {
            getString(R.string.expense_create_title)
        }

        binding.expenseSubmitBtn.text = if (expenseId != null) {
            getString(R.string.expense_submit_edit)
        } else {
            getString(R.string.expense_submit_create)
        }

        // Split type dropdown
        val splitTypes = listOf(
            "equal" to getString(R.string.expense_split_equal),
            "exact" to getString(R.string.expense_split_exact),
            "percent" to getString(R.string.expense_split_percent),
            "shares" to getString(R.string.expense_split_shares)
        )
        val splitAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, splitTypes.map { it.second })
        (binding.expenseSplitType.editText as? AutoCompleteTextView)?.setAdapter(splitAdapter)
        (binding.expenseSplitType.editText as? AutoCompleteTextView)?.setText(splitTypes[0].second, false)

        // Due type dropdown
        val dueTypes = listOf(
            "immediately" to getString(R.string.expense_due_immediately),
            "event_start" to getString(R.string.expense_due_event_start),
            "event_end" to getString(R.string.expense_due_event_end),
            "custom_date" to getString(R.string.expense_due_custom)
        )
        val dueAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, dueTypes.map { it.second })
        (binding.expenseDueType.editText as? AutoCompleteTextView)?.setAdapter(dueAdapter)
        (binding.expenseDueType.editText as? AutoCompleteTextView)?.setText(dueTypes[0].second, false)

        (binding.expenseSplitType.editText as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val showValues = position > 0
            viewModel.setSplitType(splitTypes[position].first)
            updateParticipantValueVisibility(showValues)
        }

        (binding.expenseDueType.editText as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            viewModel.setDueType(dueTypes[position].first)
            binding.expenseDueDate.isVisible = position == 3
        }

        binding.expenseSubmitBtn.setOnClickListener {
            submitForm(splitTypes)
        }

        viewModel.participants.observe(this) { participants ->
            binding.expenseParticipantsContainer.removeAllViews()
            participants.forEach { participant ->
                val row = layoutInflater.inflate(R.layout.item_expense_participant, binding.expenseParticipantsContainer, false)
                val checkbox = row.findViewById<CheckBox>(R.id.participant_checkbox)
                val valueInput = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.participant_value)
                val valueLayout = row.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.participant_value_layout)
                checkbox.text = participant.name
                checkbox.isChecked = true
                checkbox.setOnCheckedChangeListener { _, checked ->
                    viewModel.toggleParticipant(participant.id, checked)
                }
                valueLayout.isVisible = viewModel.currentSplitType != "equal"
                row.tag = participant.id
                binding.expenseParticipantsContainer.addView(row)
            }
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                ExpenseFormState.Idle -> {
                    binding.expenseSubmitBtn.isEnabled = true
                }
                ExpenseFormState.Submitting -> {
                    binding.expenseSubmitBtn.isEnabled = false
                }
                ExpenseFormState.Success -> {
                    Toast.makeText(
                        this,
                        if (expenseId != null) R.string.expense_edit_success else R.string.expense_create_success,
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is ExpenseFormState.Error -> {
                    binding.expenseSubmitBtn.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (eventId > 0) {
            viewModel.loadParticipants(eventId)
        }

        if (expenseId != null) {
            viewModel.loadExpenseForEdit(expenseId!!)
            viewModel.editData.observe(this) { detail ->
                if (detail != null) {
                    binding.expenseLabelInput.setText(detail.label)
                    binding.expenseAmountInput.setText(detail.amountCents.toString())
                    binding.expenseCurrencyInput.setText(detail.currency)
                    binding.expenseNoteInput.setText(detail.note.orEmpty())
                    val splitIndex = splitTypes.indexOfFirst { it.first == detail.splitType }.coerceAtLeast(0)
                    (binding.expenseSplitType.editText as? AutoCompleteTextView)?.setText(splitTypes[splitIndex].second, false)
                    viewModel.setSplitType(detail.splitType)
                }
            }
        }
    }

    private fun submitForm(splitTypes: List<Pair<String, String>>) {
        val label = binding.expenseLabelInput.text?.toString()?.trim().orEmpty()
        val amountStr = binding.expenseAmountInput.text?.toString()?.trim().orEmpty()
        val currency = binding.expenseCurrencyInput.text?.toString()?.trim().orEmpty().ifBlank { "CZK" }
        val note = binding.expenseNoteInput.text?.toString()?.trim()?.ifBlank { null }
        val dueDate = binding.expenseDueDateInput.text?.toString()?.trim()?.ifBlank { null }

        if (label.isBlank()) {
            Toast.makeText(this, R.string.expense_validation_label_required, Toast.LENGTH_SHORT).show()
            return
        }

        val amountCents = amountStr.toLongOrNull()
        if (amountCents == null || amountCents <= 0) {
            Toast.makeText(this, R.string.expense_validation_amount_required, Toast.LENGTH_SHORT).show()
            return
        }

        val participantValues = collectParticipantValues()
        if (participantValues.isEmpty()) {
            Toast.makeText(this, R.string.expense_validation_no_participants, Toast.LENGTH_SHORT).show()
            return
        }

        if (expenseId != null) {
            viewModel.updateExpense(expenseId!!, label, amountCents, currency, participantValues, note, dueDate)
        } else {
            viewModel.createExpense(eventId, label, amountCents, currency, participantValues, note, dueDate)
        }
    }

    private fun collectParticipantValues(): List<Pair<Long, Double?>> {
        val result = mutableListOf<Pair<Long, Double?>>()
        for (i in 0 until binding.expenseParticipantsContainer.childCount) {
            val row = binding.expenseParticipantsContainer.getChildAt(i)
            val checkbox = row.findViewById<CheckBox>(R.id.participant_checkbox)
            if (checkbox.isChecked) {
                val userId = row.tag as Long
                val valueInput = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.participant_value)
                val value = valueInput.text?.toString()?.toDoubleOrNull()
                result.add(userId to value)
            }
        }
        return result
    }

    private fun updateParticipantValueVisibility(show: Boolean) {
        for (i in 0 until binding.expenseParticipantsContainer.childCount) {
            val row = binding.expenseParticipantsContainer.getChildAt(i)
            val valueLayout = row.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.participant_value_layout)
            valueLayout.isVisible = show
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EXPENSE_ID = "extra_expense_id"
    }
}
