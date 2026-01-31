package com.celdy.groufr.ui.polls

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.R
import com.celdy.groufr.data.polls.PollDto
import com.celdy.groufr.databinding.ItemPollBinding
import com.celdy.groufr.ui.common.ChatDateFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

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
            val context = binding.root.context
            binding.pollQuestion.text = poll.question
            binding.pollDescription.text = poll.description.orEmpty()
            binding.pollStatus.text = formatStatus(poll.status)
            binding.pollVotes.text = context.getString(R.string.poll_votes_format, poll.totalVoters)
            val deadlineText = formatDeadline(poll.deadlineAt)
            binding.pollDeadline.text = deadlineText
            binding.pollDeadline.isVisible = deadlineText.isNotBlank()
            binding.pollDeadlineIcon.isVisible = deadlineText.isNotBlank()
            binding.root.setOnClickListener { onClick(poll) }
        }

        private fun formatStatus(status: String): String {
            val context = binding.root.context
            return when (status.lowercase()) {
                "open" -> context.getString(R.string.poll_status_open)
                "closed" -> context.getString(R.string.poll_status_closed)
                else -> status
            }
        }

        private fun formatDeadline(deadline: String?): String {
            if (deadline.isNullOrBlank()) return ""
            return try {
                val locale = currentLocale()
                ChatDateFormatter.formatAbsolute(deadline, locale)
            } catch (exception: DateTimeParseException) {
                ""
            }
        }

        private fun currentLocale(): Locale {
            val locales = binding.root.context.resources.configuration.locales
            return if (locales.isEmpty) Locale.getDefault() else locales[0]
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

}
