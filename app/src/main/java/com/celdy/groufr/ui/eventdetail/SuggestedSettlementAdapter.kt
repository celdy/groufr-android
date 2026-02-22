package com.celdy.groufr.ui.eventdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.R
import com.celdy.groufr.data.expenses.SuggestedSettlementDto
import com.celdy.groufr.ui.common.CurrencyFormatter

class SuggestedSettlementAdapter(
    private val currency: String
) : RecyclerView.Adapter<SuggestedSettlementAdapter.ViewHolder>() {

    private var items: List<SuggestedSettlementDto> = emptyList()

    fun submitList(list: List<SuggestedSettlementDto>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false) as TextView
        view.textSize = 13f
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(item: SuggestedSettlementDto) {
            val amount = CurrencyFormatter.format(item.amountCents, currency)
            textView.text = textView.context.getString(
                R.string.expense_settlement_format,
                item.from.name,
                item.to.name,
                amount
            )
        }
    }
}
