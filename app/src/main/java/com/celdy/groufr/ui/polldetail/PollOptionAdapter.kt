package com.celdy.groufr.ui.polldetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.data.polls.PollOptionDto
import com.celdy.groufr.databinding.ItemPollOptionBinding

class PollOptionAdapter : ListAdapter<PollOptionDto, PollOptionAdapter.OptionViewHolder>(DiffCallback) {
    private val selected = linkedSetOf<Long>()
    var multiselect: Boolean = false

    fun setSelected(optionIds: List<Long>) {
        selected.clear()
        selected.addAll(optionIds)
        notifyDataSetChanged()
    }

    fun selectedOptionIds(): List<Long> = selected.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val binding = ItemPollOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(getItem(position), selected, multiselect) { optionId ->
            if (!multiselect) {
                selected.clear()
            }
            if (selected.contains(optionId)) {
                selected.remove(optionId)
            } else {
                selected.add(optionId)
            }
            notifyDataSetChanged()
        }
    }

    class OptionViewHolder(
        private val binding: ItemPollOptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            option: PollOptionDto,
            selected: Set<Long>,
            multiselect: Boolean,
            onClick: (Long) -> Unit
        ) {
            binding.optionLabel.text = option.label
            binding.optionVotes.text = "Votes: ${option.voteCount}"
            binding.optionCheck.setOnCheckedChangeListener(null)
            binding.optionCheck.isChecked = selected.contains(option.id)
            binding.optionCheck.setOnCheckedChangeListener { _, _ -> onClick(option.id) }
            binding.root.setOnClickListener { onClick(option.id) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PollOptionDto>() {
        override fun areItemsTheSame(oldItem: PollOptionDto, newItem: PollOptionDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PollOptionDto, newItem: PollOptionDto): Boolean {
            return oldItem == newItem
        }
    }
}
