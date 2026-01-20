package com.celdy.groufr.ui.polls

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.data.polls.PollDto
import com.celdy.groufr.databinding.ItemPollBinding
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class PollAdapter(
    private val onClick: (PollDto) -> Unit
) : ListAdapter<PollDto, PollAdapter.PollViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollViewHolder {
        val binding = ItemPollBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PollViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PollViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class PollViewHolder(
        private val binding: ItemPollBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(poll: PollDto, onClick: (PollDto) -> Unit) {
            binding.pollQuestion.text = poll.question
            binding.pollDescription.text = poll.description.orEmpty()
            binding.pollStatus.text = poll.status
            binding.pollVotes.text = "Voters: ${poll.totalVoters}"
            binding.pollDeadline.text = formatDeadline(poll.deadlineAt)
            binding.root.setOnClickListener { onClick(poll) }
        }

        private fun formatDeadline(deadline: String?): String {
            if (deadline.isNullOrBlank()) return ""
            return try {
                val instant = OffsetDateTime.parse(deadline)
                "Deadline: ${instant.format(DISPLAY_FORMAT)}"
            } catch (exception: DateTimeParseException) {
                ""
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PollDto>() {
        override fun areItemsTheSame(oldItem: PollDto, newItem: PollDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PollDto, newItem: PollDto): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private val DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
