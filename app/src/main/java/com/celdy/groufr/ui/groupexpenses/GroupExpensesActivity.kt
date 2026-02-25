package com.celdy.groufr.ui.groupexpenses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.R
import com.celdy.groufr.data.expenses.GroupBalancesResponse
import com.celdy.groufr.data.expenses.SuggestedSettlementDto
import com.celdy.groufr.data.expenses.UserBalanceDto
import com.celdy.groufr.data.storage.TokenStore
import com.celdy.groufr.databinding.ActivityGroupExpensesBinding
import com.celdy.groufr.ui.common.CurrencyFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GroupExpensesActivity : AppCompatActivity() {

    @Inject lateinit var tokenStore: TokenStore

    private lateinit var binding: ActivityGroupExpensesBinding
    private val viewModel: GroupExpensesViewModel by viewModels()
    private lateinit var settlementAdapter: SettlementAdapter

    private var groupId: Long = -1L
    private var groupName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.groupExpensesToolbar.updatePadding(top = systemBars.top)
            binding.groupExpensesScroll.updatePadding(bottom = systemBars.bottom)
            binding.root.updatePadding(left = systemBars.left, right = systemBars.right)
            insets
        }

        groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
        groupName = intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty()

        binding.groupExpensesToolbar.setNavigationOnClickListener { finish() }

        val currentUserId = tokenStore.getUserId()

        settlementAdapter = SettlementAdapter(
            currentUserId = currentUserId,
            onConfirm = { settlement ->
                viewModel.confirmSettlement(settlement.id)
            },
            onReject = { settlement ->
                viewModel.rejectSettlement(settlement.id)
            },
            onCancel = { settlement ->
                viewModel.cancelSettlement(settlement.id)
            }
        )

        binding.groupSettlementsList.layoutManager = LinearLayoutManager(this)
        binding.groupSettlementsList.adapter = settlementAdapter

        binding.groupBalancesList.layoutManager = LinearLayoutManager(this)
        binding.groupSuggestedList.layoutManager = LinearLayoutManager(this)

        binding.groupExpensesFab.setOnClickListener {
            showCreateSettlementDialog()
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                GroupExpensesState.Loading -> {
                    binding.groupExpensesLoading.isVisible = true
                    binding.groupExpensesScroll.isVisible = false
                    binding.groupExpensesError.isVisible = false
                    binding.groupExpensesFab.isVisible = false
                }
                is GroupExpensesState.Content -> {
                    binding.groupExpensesLoading.isVisible = false
                    binding.groupExpensesScroll.isVisible = true
                    binding.groupExpensesError.isVisible = false
                    binding.groupExpensesFab.isVisible = true
                    bindContent(state.balances, state.settlements)
                }
                GroupExpensesState.Error -> {
                    binding.groupExpensesLoading.isVisible = false
                    binding.groupExpensesScroll.isVisible = false
                    binding.groupExpensesError.isVisible = true
                    binding.groupExpensesFab.isVisible = false
                }
            }
        }

        viewModel.actionState.observe(this) { state ->
            when (state) {
                SettlementActionState.Idle -> Unit
                SettlementActionState.Sending -> Unit
                SettlementActionState.Sent -> {
                    Toast.makeText(this, R.string.settlement_create_success, Toast.LENGTH_SHORT).show()
                }
                is SettlementActionState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (groupId > 0) {
            viewModel.load(groupId)
        }
    }

    private fun bindContent(
        balances: GroupBalancesResponse?,
        settlements: List<com.celdy.groufr.data.settlements.SettlementDto>
    ) {
        if (balances != null && balances.balances.isNotEmpty()) {
            binding.groupBalanceCard.isVisible = true
            binding.groupBalancesList.adapter = BalanceListAdapter(balances.balances)

            if (balances.settlements.isNotEmpty()) {
                binding.groupSuggestedLabel.isVisible = true
                binding.groupSuggestedList.isVisible = true
                binding.groupSuggestedList.adapter = SuggestedPayAdapter(
                    balances.settlements,
                    onPay = { suggestion -> showCreateSettlementDialog(suggestion) }
                )
            } else {
                binding.groupSuggestedLabel.isVisible = false
                binding.groupSuggestedList.isVisible = false
            }
        } else {
            binding.groupBalanceCard.isVisible = false
            binding.groupSuggestedLabel.isVisible = false
            binding.groupSuggestedList.isVisible = false
        }

        settlementAdapter.submitList(settlements)
        binding.groupSettlementsEmpty.isVisible = settlements.isEmpty()
    }

    private fun showCreateSettlementDialog(suggestion: SuggestedSettlementDto? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_settlement, null)
        val recipientInput = dialogView.findViewById<EditText>(R.id.settlement_recipient_input)
        val amountInput = dialogView.findViewById<EditText>(R.id.settlement_amount_input)
        val currencyInput = dialogView.findViewById<EditText>(R.id.settlement_currency_input)
        val noteInput = dialogView.findViewById<EditText>(R.id.settlement_note_input)

        if (suggestion != null) {
            recipientInput.setText(suggestion.to.id.toString())
            amountInput.setText(suggestion.amountCents.toString())
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settlement_dialog_title)
            .setView(dialogView)
            .setPositiveButton(R.string.settlement_create) { _, _ ->
                val recipientId = recipientInput.text.toString().toLongOrNull() ?: return@setPositiveButton
                val amountCents = amountInput.text.toString().toLongOrNull() ?: return@setPositiveButton
                val currency = currencyInput.text.toString().ifBlank { "CZK" }
                val note = noteInput.text.toString().ifBlank { null }
                viewModel.createSettlement(groupId, recipientId, amountCents, currency, note)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
    }
}

private class BalanceListAdapter(
    private val balances: List<UserBalanceDto>
) : RecyclerView.Adapter<BalanceListAdapter.ViewHolder>() {

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val tv = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 4, 0, 4)
            textSize = 13f
        }
        return ViewHolder(tv)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val balance = balances[position]
        val formatted = CurrencyFormatter.format(balance.balanceCents, balance.currency)
        holder.textView.text = "${balance.user.name}: $formatted"
        val colorRes = when {
            balance.balanceCents > 0 -> R.color.balance_positive
            balance.balanceCents < 0 -> R.color.balance_negative
            else -> R.color.balance_zero
        }
        holder.textView.setTextColor(ContextCompat.getColor(holder.textView.context, colorRes))
    }

    override fun getItemCount() = balances.size
}

private class SuggestedPayAdapter(
    private val suggestions: List<SuggestedSettlementDto>,
    private val onPay: (SuggestedSettlementDto) -> Unit
) : RecyclerView.Adapter<SuggestedPayAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.suggested_text)
        val payBtn: com.google.android.material.button.MaterialButton = view.findViewById(R.id.suggested_pay_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggested_settlement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestion = suggestions[position]
        val formatted = CurrencyFormatter.format(suggestion.amountCents, "CZK")
        holder.text.text = holder.itemView.context.getString(
            R.string.expense_settlement_format,
            suggestion.from.name,
            suggestion.to.name,
            formatted
        )
        holder.payBtn.setOnClickListener { onPay(suggestion) }
    }

    override fun getItemCount() = suggestions.size
}
