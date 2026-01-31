package com.celdy.groufr.ui.polldetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.data.polls.PollOptionDto
import com.celdy.groufr.databinding.ItemPollOptionBinding

class PollOptionAdapter(
    private val onVotesClick: (PollOptionDto) -> Unit
) : ListAdapter<PollOptionDto, PollOptionAdapter.OptionViewHolder>(DiffCallback) {
    private val selected = linkedSetOf<Long>()
    private var onSelectionChanged: ((Set<Long>) -> Unit)? = null
    var multiselect: Boolean = false
    var totalVotes: Int = 0

    fun setSelected(optionIds: List<Long>) {
        selected.clear()
        selected.addAll(optionIds)
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selected)
    }

    fun selectedOptionIds(): List<Long> = selected.toList()

    fun setOnSelectionChanged(listener: (Set<Long>) -> Unit) {
        onSelectionChanged = listener
        listener.invoke(selected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val binding = ItemPollOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(getItem(position), selected, multiselect, totalVotes, onVotesClick) { optionId ->
            if (!multiselect) {
                selected.clear()
            }
            if (selected.contains(optionId)) {
                selected.remove(optionId)
            } else {
                selected.add(optionId)
            }
            notifyDataSetChanged()
            onSelectionChanged?.invoke(selected)
        }
    }

    class OptionViewHolder(
        private val binding: ItemPollOptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            option: PollOptionDto,
            selected: Set<Long>,
            multiselect: Boolean,
            totalVotes: Int,
            onVotesClick: (PollOptionDto) -> Unit,
            onClick: (Long) -> Unit
        ) {
            binding.optionLabel.text = option.label
            binding.optionVotes.text = formatVotes(option.voteCount, totalVotes)
            binding.optionCheck.isSelected = selected.contains(option.id)
            binding.root.setOnClickListener { onClick(option.id) }
            binding.optionVotesBadge.setOnClickListener { onVotesClick(option) }
        }

        private fun formatVotes(voteCount: Int, totalVotes: Int): String {
            if (totalVotes <= 0) {
                return "${voteCount} (0%)"
            }
            val percent = ((voteCount.toDouble() / totalVotes.toDouble()) * 100).toInt()
            return "${voteCount} (${percent}%)"
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
