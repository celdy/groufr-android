package com.celdy.groufr.ui.eventdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.data.events.EventParticipantDto
import com.celdy.groufr.databinding.ItemParticipantBinding

class EventParticipantAdapter :
    ListAdapter<EventParticipantDto, EventParticipantAdapter.ParticipantViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ItemParticipantBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ParticipantViewHolder(
        private val binding: ItemParticipantBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(participant: EventParticipantDto) {
            val context = binding.root.context
            binding.participantName.text = participant.user.name
            binding.participantStatus.text = when (participant.status) {
                "joined" -> context.getString(com.celdy.groufr.R.string.event_participant_joined)
                "maybe" -> context.getString(com.celdy.groufr.R.string.event_participant_maybe)
                "declined" -> context.getString(com.celdy.groufr.R.string.event_participant_declined)
                "invited" -> context.getString(com.celdy.groufr.R.string.event_participant_invited)
                "not_invited" -> context.getString(com.celdy.groufr.R.string.event_participant_not_invited)
                else -> participant.status
            }
            binding.participantRole.text = when (participant.role) {
                "owner" -> context.getString(com.celdy.groufr.R.string.event_role_owner)
                "admin" -> context.getString(com.celdy.groufr.R.string.event_role_admin)
                "participant" -> context.getString(com.celdy.groufr.R.string.event_role_participant)
                "guest" -> context.getString(com.celdy.groufr.R.string.event_role_guest)
                else -> participant.role
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<EventParticipantDto>() {
        override fun areItemsTheSame(
            oldItem: EventParticipantDto,
            newItem: EventParticipantDto
        ): Boolean {
            return oldItem.user.id == newItem.user.id
        }

        override fun areContentsTheSame(
            oldItem: EventParticipantDto,
            newItem: EventParticipantDto
        ): Boolean {
            return oldItem == newItem
        }
    }
}
