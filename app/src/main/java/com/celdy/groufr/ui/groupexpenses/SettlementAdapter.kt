package com.celdy.groufr.ui.groupexpenses

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.R
import com.celdy.groufr.data.settlements.SettlementDto
import com.celdy.groufr.databinding.ItemSettlementBinding
import com.celdy.groufr.ui.common.CurrencyFormatter

class SettlementAdapter(
    private val currentUserId: Long,
    private val onConfirm: (SettlementDto) -> Unit,
    private val onReject: (SettlementDto) -> Unit,
    private val onCancel: (SettlementDto) -> Unit
) : ListAdapter<SettlementDto, SettlementAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSettlementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSettlementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(settlement: SettlementDto) {
            val context = binding.root.context
            binding.settlementParties.text = context.getString(
                R.string.settlement_format,
                settlement.payer.name,
                settlement.recipient.name
            )
            binding.settlementAmount.text = CurrencyFormatter.format(settlement.amountCents, settlement.currency)

            val (statusText, statusColor) = when (settlement.status) {
                "pending" -> context.getString(R.string.settlement_status_pending) to R.color.expense_status_pending_text
                "confirmed" -> context.getString(R.string.settlement_status_confirmed) to R.color.expense_status_active_text
                "rejected" -> context.getString(R.string.settlement_status_rejected) to R.color.expense_status_disputed_text
                else -> settlement.status to R.color.notification_meta_text
            }
            binding.settlementStatus.text = statusText
            binding.settlementStatus.setTextColor(ContextCompat.getColor(context, statusColor))

            if (settlement.note != null) {
                binding.settlementNote.text = settlement.note
                binding.settlementNote.isVisible = true
            } else {
                binding.settlementNote.isVisible = false
            }

            val isPending = settlement.status == "pending"
            val isRecipient = settlement.recipient.id == currentUserId
            val isPayer = settlement.payer.id == currentUserId

            binding.settlementBtnConfirm.isVisible = isPending && isRecipient
            binding.settlementBtnReject.isVisible = isPending && isRecipient
            binding.settlementBtnCancel.isVisible = isPending && isPayer

            binding.settlementBtnConfirm.setOnClickListener { onConfirm(settlement) }
            binding.settlementBtnReject.setOnClickListener { onReject(settlement) }
            binding.settlementBtnCancel.setOnClickListener { onCancel(settlement) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SettlementDto>() {
            override fun areItemsTheSame(oldItem: SettlementDto, newItem: SettlementDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: SettlementDto, newItem: SettlementDto) = oldItem == newItem
        }
    }
}
