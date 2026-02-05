package com.celdy.groufr.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.R
import com.celdy.groufr.data.notifications.NotificationDto
import com.celdy.groufr.data.notifications.eventIdFromPayload
import com.celdy.groufr.data.notifications.invitedGroupNameFromPayload
import com.celdy.groufr.databinding.ItemNotificationBinding
import com.celdy.groufr.ui.common.ChatDateFormatter
import java.util.Locale

class NotificationsAdapter(
    private val onClick: (NotificationDto) -> Unit
) : ListAdapter<NotificationDto, NotificationsAdapter.NotificationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: NotificationDto, onClick: (NotificationDto) -> Unit) {
            val context = binding.root.context
            val locales = context.resources.configuration.locales
            val locale = if (locales.isEmpty) Locale.getDefault() else locales[0]
            val actor = notification.actor?.name ?: "System"
            val title = buildTitle(context, actor, notification)
            val preview = extractPreview(notification)

            binding.notificationRoot.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    if (notification.isRead) R.color.notification_read_bg else R.color.notification_unread_bg
                )
            )
            binding.notificationIcon.setImageResource(resolveIcon(notification.eventType))
            binding.notificationTitle.text = title
            val eventTitle = extractEventTitle(notification)
            val showEvent = shouldShowEvent(notification, eventTitle)
            binding.notificationEvent.isVisible = showEvent
            binding.notificationEvent.text = eventTitle
            val showSubtitle = (notification.eventType == "new_message" || notification.eventType == "reaction_message") && preview.isNotBlank()
            binding.notificationSubtitle.isVisible = showSubtitle
            binding.notificationSubtitle.text = preview

            // For invitation notifications, show the invited group name from payload
            val displayGroupName = if (notification.eventType == "invitation_received") {
                notification.invitedGroupNameFromPayload() ?: notification.groupName ?: "Group"
            } else {
                notification.groupName ?: "Group"
            }
            binding.notificationMeta.text = "$displayGroupName - ${formatTimestamp(notification.createdAt, locale)}"
            binding.root.setOnClickListener { onClick(notification) }
        }

        private fun buildTitle(context: android.content.Context, actor: String, notification: NotificationDto): String {
            val eventType = notification.eventType
            val resId = when (eventType) {
                "event_created" -> R.string.notification_title_event_created
                "event_updated" -> R.string.notification_title_event_updated
                "new_message" -> R.string.notification_title_new_message
                "poll_created" -> R.string.notification_title_poll_created
                "poll_closed" -> R.string.notification_title_poll_closed
                "user_joined" -> R.string.notification_title_user_joined
                "participant_status_changed" -> R.string.notification_title_participant_status_changed
                "invitation_received" -> R.string.notification_title_invitation_received
                "reaction_message" -> R.string.notification_title_reaction_message
                "reaction_event" -> R.string.notification_title_reaction_event
                "reaction_poll" -> R.string.notification_title_reaction_poll
                else -> R.string.notification_title_generic
            }

            return when {
                resId == R.string.notification_title_generic ->
                    context.getString(resId, actor, eventType)
                eventType.startsWith("reaction_") ->
                    context.getString(resId, actor, extractReactionEmojis(notification))
                else ->
                    context.getString(resId, actor)
            }
        }

        private fun extractReactionEmojis(notification: NotificationDto): String {
            val payload = notification.payload ?: return "👍"
            val reactions = payload["reactions"]
            if (reactions is List<*>) {
                val emojis = reactions.mapNotNull { item ->
                    if (item is Map<*, *>) item["emoji"] as? String else null
                }.distinct()
                if (emojis.isNotEmpty()) return emojis.joinToString("")
            }
            return "👍"
        }

        private fun extractPreview(notification: NotificationDto): String {
            val payload = notification.payload ?: return ""
            val preview = payload["preview"] ?: return ""
            return preview as? String ?: preview.toString()
        }

        private fun resolveIcon(eventType: String): Int {
            return when (eventType) {
                "event_created", "event_updated", "participant_status_changed" -> R.drawable.ico_event
                "new_message" -> R.drawable.ico_message
                "poll_created", "poll_closed" -> R.drawable.ico_poll
                "user_joined", "invitation_received" -> R.drawable.ico_user
                "reaction_message", "reaction_event", "reaction_poll" -> R.drawable.ico_reaction
                else -> R.drawable.ico_message
            }
        }

        private fun formatTimestamp(createdAt: String, locale: Locale): String {
            return ChatDateFormatter.format(createdAt, locale)
        }

        private fun shouldShowEvent(notification: NotificationDto, eventTitle: String): Boolean {
            if (eventTitle.isBlank()) return false
            return when (notification.eventType) {
                "event_created",
                "event_updated",
                "participant_status_changed",
                "event_invitation_received",
                "reaction_event",
                "reaction_poll" -> true
                "new_message" -> (notification.eventIdFromPayload() ?: -1L) > 0
                else -> false
            }
        }

        private fun extractEventTitle(notification: NotificationDto): String {
            val payload = notification.payload ?: return ""
            if (notification.eventType == "reaction_event" || notification.eventType == "reaction_poll") {
                val preview = payload["preview"]
                    ?: payload["event_title"]
                    ?: payload["eventTitle"]
                    ?: payload["poll_question"]
                    ?: payload["pollQuestion"]
                    ?: payload["title"]
                    ?: return ""
                return preview as? String ?: preview.toString()
            }
            val title = payload["event_title"]
                ?: payload["eventTitle"]
                ?: payload["title"]
                ?: payload["event_name"]
                ?: payload["eventName"]
                ?: return ""
            return title as? String ?: title.toString()
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<NotificationDto>() {
        override fun areItemsTheSame(oldItem: NotificationDto, newItem: NotificationDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationDto, newItem: NotificationDto): Boolean {
            return oldItem == newItem
        }
    }
}
