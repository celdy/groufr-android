package com.celdy.groufr.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.R
import com.celdy.groufr.data.events.EventDto
import com.celdy.groufr.databinding.ItemEventBinding
import com.celdy.groufr.ui.common.ChatDateFormatter
import java.util.Locale

class EventAdapter(
    private val onClick: (EventDto) -> Unit
) : ListAdapter<EventDto, EventAdapter.EventViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class EventViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val context get() = binding.root.context

        fun bind(event: EventDto, onClick: (EventDto) -> Unit) {
            binding.eventGroupName.isVisible = event.group != null
            binding.eventGroupName.text = event.group?.name.orEmpty()
            binding.eventTitle.text = event.title
            binding.eventState.text = formatEventState(event.state)
            styleStateBadge(event.state)
            binding.eventStatus.text = formatParticipantStatus(event.yourStatus)
            styleUserStatusBadge(event.yourStatus)
            binding.eventTime.text = formatEventDate(event.startAt, event.endAt)
            bindParticipantSummary(event.participants)
            binding.root.setOnClickListener { onClick(event) }
        }

        private fun styleStateBadge(state: String) {
            when (state) {
                "offered" -> {
                    binding.eventState.setBackgroundResource(R.drawable.bg_event_status_offered)
                    binding.eventState.setTextColor(ContextCompat.getColor(context, R.color.event_state_offered_text))
                }
                "preparing" -> {
                    binding.eventState.setBackgroundResource(R.drawable.bg_event_status_preparing)
                    binding.eventState.setTextColor(ContextCompat.getColor(context, R.color.event_state_preparing_text))
                }
                "closed" -> {
                    binding.eventState.setBackgroundResource(R.drawable.bg_event_status_closed)
                    binding.eventState.setTextColor(ContextCompat.getColor(context, R.color.event_state_closed_text))
                }
                "cancelled" -> {
                    binding.eventState.setBackgroundResource(R.drawable.bg_event_status_cancelled)
                    binding.eventState.setTextColor(ContextCompat.getColor(context, R.color.event_state_cancelled_text))
                }
                else -> {
                    binding.eventState.setBackgroundResource(R.drawable.bg_event_status_badge)
                    binding.eventState.setTextColor(ContextCompat.getColor(context, R.color.primary_700))
                }
            }
        }

        private fun formatEventState(state: String): String {
            return when (state) {
                "offered" -> context.getString(R.string.event_status_offered)
                "preparing" -> context.getString(R.string.event_status_preparing)
                "closed" -> context.getString(R.string.event_status_closed)
                "cancelled" -> context.getString(R.string.event_status_cancelled)
                else -> state
            }
        }

        private fun formatParticipantStatus(status: String): String {
            return when (status) {
                "joined" -> context.getString(R.string.event_participant_joined)
                "maybe" -> context.getString(R.string.event_participant_maybe)
                "declined" -> context.getString(R.string.event_participant_declined)
                "invited" -> context.getString(R.string.event_participant_invited)
                "not_invited" -> context.getString(R.string.event_participant_not_invited)
                else -> status
            }
        }

        private fun styleUserStatusBadge(status: String) {
            val bgRes = when (status) {
                "joined" -> R.drawable.bg_status_joined
                "maybe" -> R.drawable.bg_status_maybe
                "declined" -> R.drawable.bg_status_declined
                "invited" -> R.drawable.bg_status_invited
                "not_invited" -> R.drawable.bg_status_not_invited
                else -> R.drawable.bg_status_not_invited
            }
            binding.eventStatus.setBackgroundResource(bgRes)
        }

        private fun formatEventDate(startAt: String?, endAt: String?): String {
            val locale = currentLocale()
            val startBlank = startAt.isNullOrBlank()
            val endBlank = endAt.isNullOrBlank()
            if (startBlank && endBlank) {
                return context.getString(R.string.event_date_not_set)
            }
            if (!startBlank && endBlank) {
                return ChatDateFormatter.format(startAt!!, locale)
            }
            if (startBlank && !endBlank) {
                val endText = ChatDateFormatter.format(endAt!!, locale)
                return "${context.getString(R.string.event_date_end_prefix)} $endText"
            }
            return ChatDateFormatter.formatRange(startAt, endAt, locale)
                ?: context.getString(R.string.event_date_not_set)
        }

        private fun currentLocale(): Locale {
            val locales = context.resources.configuration.locales
            return if (locales.isEmpty) Locale.getDefault() else locales[0]
        }

        private fun bindParticipantSummary(participants: Map<String, Int>) {
            val joined = participants["joined"] ?: 0
            val maybe = participants["maybe"] ?: 0
            val declined = participants["declined"] ?: 0
            binding.eventSummaryJoined.text = joined.toString()
            binding.eventSummaryMaybe.text = maybe.toString()
            binding.eventSummaryDeclined.text = declined.toString()
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<EventDto>() {
        override fun areItemsTheSame(oldItem: EventDto, newItem: EventDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EventDto, newItem: EventDto): Boolean {
            return oldItem == newItem
        }
    }
}
