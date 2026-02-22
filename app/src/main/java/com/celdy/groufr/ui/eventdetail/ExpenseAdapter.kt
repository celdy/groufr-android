package com.celdy.groufr.ui.eventdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.R
import com.celdy.groufr.data.expenses.ExpenseDto
import com.celdy.groufr.databinding.ItemExpenseBinding
import com.celdy.groufr.ui.common.CurrencyFormatter

class ExpenseAdapter(
    private val currentUserId: Long,
    private val onConfirm: (ExpenseDto) -> Unit,
    private val onDispute: (ExpenseDto) -> Unit,
    private val onEdit: (ExpenseDto) -> Unit,
    private val onDelete: (ExpenseDto) -> Unit,
    private val onSettle: (ExpenseDto) -> Unit,
    private val onClick: (ExpenseDto) -> Unit
) : ListAdapter<ExpenseDto, ExpenseAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: ExpenseDto) {
            val context = binding.root.context
            binding.expenseLabel.text = expense.label
            binding.expenseAmount.text = CurrencyFormatter.format(expense.amountCents, expense.currency)
            binding.expensePayer.text = context.getString(R.string.expense_paid_by, expense.payer.name)

            // Status badge
            val (statusText, statusBg, statusTextColor) = when (expense.status) {
                "pending_confirmation" -> Triple(
                    context.getString(R.string.expense_status_pending),
                    R.drawable.bg_expense_status_pending,
                    R.color.expense_status_pending_text
                )
                "active" -> Triple(
                    context.getString(R.string.expense_status_active),
                    R.drawable.bg_expense_status_active,
                    R.color.expense_status_active_text
                )
                "disputed" -> Triple(
                    context.getString(R.string.expense_status_disputed),
                    R.drawable.bg_expense_status_disputed,
                    R.color.expense_status_disputed_text
                )
                "settled" -> Triple(
                    context.getString(R.string.expense_status_settled),
                    R.drawable.bg_expense_status_settled,
                    R.color.expense_status_settled_text
                )
                else -> Triple(
                    expense.status,
                    R.drawable.bg_expense_status_pending,
                    R.color.expense_status_pending_text
                )
            }
            binding.expenseStatus.text = statusText
            binding.expenseStatus.setBackgroundResource(statusBg)
            binding.expenseStatus.setTextColor(ContextCompat.getColor(context, statusTextColor))

            // Your share
            val myShare = expense.shares.find { it.user.id == currentUserId }
            if (myShare != null) {
                val shareFormatted = CurrencyFormatter.format(myShare.shareCents, expense.currency)
                binding.expenseYourShare.text = when (myShare.confirmationStatus) {
                    "confirmed" -> context.getString(R.string.expense_your_share_confirmed, shareFormatted)
                    "disputed" -> context.getString(R.string.expense_your_share_disputed, shareFormatted)
                    else -> context.getString(R.string.expense_your_share, shareFormatted)
                }
                binding.expenseYourShare.isVisible = true
            } else {
                binding.expenseYourShare.isVisible = false
            }

            // Action buttons visibility
            val isSettled = expense.status == "settled"
            val isPayer = expense.payer.id == currentUserId
            val isCreator = expense.createdBy?.id == currentUserId
            val canConfirm = myShare != null && myShare.confirmationStatus == "pending" && !isSettled
            val canDispute = myShare != null && myShare.confirmationStatus != "disputed" && !isPayer && !isSettled
            val canEdit = (isPayer || isCreator) && !isSettled
            val canDelete = (isPayer || isCreator) && !isSettled
            val canSettle = !isSettled

            binding.expenseBtnConfirm.isVisible = canConfirm
            binding.expenseBtnDispute.isVisible = canDispute
            binding.expenseBtnEdit.isVisible = canEdit
            binding.expenseBtnDelete.isVisible = canDelete
            binding.expenseBtnSettle.isVisible = canSettle && isPayer
            binding.expenseActions.isVisible = canConfirm || canDispute || canEdit || canDelete || (canSettle && isPayer)

            binding.expenseBtnConfirm.setOnClickListener { onConfirm(expense) }
            binding.expenseBtnDispute.setOnClickListener { onDispute(expense) }
            binding.expenseBtnEdit.setOnClickListener { onEdit(expense) }
            binding.expenseBtnDelete.setOnClickListener { onDelete(expense) }
            binding.expenseBtnSettle.setOnClickListener { onSettle(expense) }
            binding.root.setOnClickListener { onClick(expense) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ExpenseDto>() {
            override fun areItemsTheSame(oldItem: ExpenseDto, newItem: ExpenseDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ExpenseDto, newItem: ExpenseDto) = oldItem == newItem
        }
    }
}
