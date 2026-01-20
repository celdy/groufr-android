package com.celdy.groufr.ui.groupdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.data.messages.MessageDto
import com.celdy.groufr.databinding.ItemMessageBinding
import androidx.core.view.isVisible
import androidx.core.content.ContextCompat
import com.celdy.groufr.ui.common.ChatDateFormatter
import java.util.Locale

class MessageAdapter(
    private val currentUserId: Long,
    private val groupId: Long,
    private val groupName: String,
    private val onEventClick: (Long, String) -> Unit,
    private val onPollClick: (Long, String) -> Unit
) : ListAdapter<MessageDto, MessageAdapter.MessageViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(
            getItem(position),
            currentUserId,
            groupId,
            groupName,
            onEventClick,
            onPollClick
        )
    }

    class MessageViewHolder(
        private val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            message: MessageDto,
            currentUserId: Long,
            groupId: Long,
            groupName: String,
            onEventClick: (Long, String) -> Unit,
            onPollClick: (Long, String) -> Unit
        ) {
            val context = binding.root.context
            val locales = context.resources.configuration.locales
            val locale = if (locales.isEmpty) Locale.getDefault() else locales[0]
            val isOwn = message.user?.id == currentUserId && message.messageType == "text"
            val isJoin = message.messageType == "user_joined"
            val isEvent = message.messageType.contains("event")
            val isPoll = message.messageType.contains("poll")
            val authorName = message.refUser?.name
                ?: message.user?.name
                ?: context.getString(com.celdy.groufr.R.string.chat_system_user)

            val bodyText = when {
                isJoin -> context.getString(com.celdy.groufr.R.string.chat_joined_group, authorName)
                isEvent -> {
                    val title = message.refEvent?.title
                        ?: context.getString(com.celdy.groufr.R.string.chat_event_fallback)
                    context.getString(com.celdy.groufr.R.string.chat_event_created, authorName, title)
                }
                isPoll -> {
                    val title = message.refPoll?.question
                        ?: context.getString(com.celdy.groufr.R.string.chat_poll_fallback)
                    val deadline = formatDeadline(message.refPoll?.deadlineAt, locale)
                    if (deadline.isNotBlank()) {
                        context.getString(
                            com.celdy.groufr.R.string.chat_poll_created_due,
                            authorName,
                            title,
                            deadline
                        )
                    } else {
                        context.getString(com.celdy.groufr.R.string.chat_poll_created, authorName, title)
                    }
                }
                else -> message.body.orEmpty()
            }

            binding.messageAuthor.isVisible = !isOwn && authorName.isNotBlank()
            binding.messageAuthor.text = authorName
            binding.messageBody.text = bodyText
            binding.messageTimestamp.text = formatTimestamp(message.createdAt, locale)

            val (bgColor, textColor) = when {
                isOwn -> Pair(com.celdy.groufr.R.color.message_self_bg, com.celdy.groufr.R.color.message_self_text)
                isJoin -> Pair(com.celdy.groufr.R.color.message_join_bg, com.celdy.groufr.R.color.message_join_text)
                isEvent -> Pair(com.celdy.groufr.R.color.message_event_bg, com.celdy.groufr.R.color.message_event_text)
                isPoll -> Pair(com.celdy.groufr.R.color.message_poll_bg, com.celdy.groufr.R.color.message_poll_text)
                else -> Pair(com.celdy.groufr.R.color.white, com.celdy.groufr.R.color.black)
            }

            binding.messageCard.setCardBackgroundColor(ContextCompat.getColor(context, bgColor))
            binding.messageBody.setTextColor(ContextCompat.getColor(context, textColor))
            binding.messageAuthor.setTextColor(ContextCompat.getColor(context, textColor))
            binding.messageTimestamp.setTextColor(ContextCompat.getColor(context, textColor))

            val params = binding.messageCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.horizontalBias = if (isOwn) 1f else 0f
            binding.messageCard.layoutParams = params

            binding.messageCard.setOnClickListener(null)
            if (isEvent && message.refEvent != null) {
                binding.messageCard.setOnClickListener {
                    onEventClick(message.refEvent.id, groupName)
                }
            } else if (isPoll && message.refPoll != null) {
                binding.messageCard.setOnClickListener {
                    onPollClick(message.refPoll.id, groupName)
                }
            }
        }

        private fun formatTimestamp(createdAt: String, locale: Locale): String {
            return ChatDateFormatter.format(createdAt, locale)
        }

        private fun formatDeadline(deadline: String?, locale: Locale): String {
            if (deadline.isNullOrBlank()) return ""
            return ChatDateFormatter.format(deadline, locale)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MessageDto>() {
        override fun areItemsTheSame(oldItem: MessageDto, newItem: MessageDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MessageDto, newItem: MessageDto): Boolean {
            return oldItem == newItem
        }
    }
}
