package com.celdy.groufr.ui.eventdetail

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celdy.groufr.R
import com.celdy.groufr.data.messages.MessageDto
import com.celdy.groufr.databinding.ItemMessageBinding
import com.celdy.groufr.databinding.ItemMessageDividerBinding
import com.celdy.groufr.ui.common.ChatDateFormatter
import com.celdy.groufr.ui.common.MarkdownRenderer
import com.celdy.groufr.ui.common.AvatarHelper
import com.celdy.groufr.ui.common.ReactionHelper
import java.util.Locale

sealed class EventChatItem {
    data class Message(val value: MessageDto) : EventChatItem()
    data object Divider : EventChatItem()
}

class EventChatAdapter(
    private val currentUserId: Long,
    private val onReactMessage: (MessageDto) -> Unit = {},
    private val onReportMessage: (MessageDto) -> Unit = {},
    private val onShowReactors: (MessageDto) -> Unit = {}
) : ListAdapter<EventChatItem, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is EventChatItem.Message -> VIEW_MESSAGE
            EventChatItem.Divider -> VIEW_DIVIDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_MESSAGE -> {
                val binding = ItemMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                MessageViewHolder(binding, onReactMessage, onReportMessage, onShowReactors)
            }
            else -> {
                val binding = ItemMessageDividerBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                DividerViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is EventChatItem.Message -> (holder as MessageViewHolder).bind(item.value, currentUserId)
            EventChatItem.Divider -> Unit
        }
    }

    class MessageViewHolder(
        private val binding: ItemMessageBinding,
        private val onReactMessage: (MessageDto) -> Unit,
        private val onReportMessage: (MessageDto) -> Unit,
        private val onShowReactors: (MessageDto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: MessageDto, currentUserId: Long) {
            val context = binding.root.context
            val locales = context.resources.configuration.locales
            val locale = if (locales.isEmpty) Locale.getDefault() else locales[0]
            val isOwn = message.user?.id == currentUserId && message.messageType == "text"
            val isJoin = message.messageType == "user_joined"
            val isEvent = message.messageType.contains("event")
            val isPoll = message.messageType.contains("poll")
            val isSystemMessage = isJoin || isEvent || isPoll
            val authorName = message.refUser?.name
                ?: message.user?.name
                ?: context.getString(R.string.chat_system_user)

            val bodyText = when {
                isJoin -> context.getString(R.string.chat_joined_group, authorName)
                isEvent -> {
                    val title = message.refEvent?.title
                        ?: context.getString(R.string.chat_event_fallback)
                    context.getString(R.string.chat_event_created, authorName, title)
                }
                isPoll -> {
                    val title = message.refPoll?.question
                        ?: context.getString(R.string.chat_poll_fallback)
                    context.getString(R.string.chat_poll_created, authorName, title)
                }
                else -> message.body.orEmpty()
            }

            val showAvatar = !isOwn && !isSystemMessage
            binding.messageAvatar.isVisible = showAvatar
            if (showAvatar) {
                AvatarHelper.bindAvatar(binding.messageAvatar, authorName)
            }

            binding.messageAuthor.isVisible = !isOwn && authorName.isNotBlank()
            binding.messageAuthor.text = authorName
            binding.messageBody.text = MarkdownRenderer.render(bodyText)
            binding.messageBody.movementMethod = LinkMovementMethod.getInstance()
            binding.messageTimestamp.text = ChatDateFormatter.format(message.createdAt, locale)

            val showMenu = !isSystemMessage && !isOwn
            binding.messageMenu.isVisible = showMenu
            if (showMenu) {
                binding.messageMenu.setOnClickListener { view ->
                    val popup = PopupMenu(context, view)
                    popup.menu.add(0, MENU_REPORT, 0, R.string.message_menu_report)
                    popup.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            MENU_REPORT -> {
                                onReportMessage(message)
                                true
                            }
                            else -> false
                        }
                    }
                    popup.show()
                }
            } else {
                binding.messageMenu.setOnClickListener(null)
            }

            val (bgColor, textColor) = when {
                isOwn -> Pair(R.color.message_self_bg, R.color.message_self_text)
                isJoin -> Pair(R.color.message_join_bg, R.color.message_join_text)
                isEvent -> Pair(R.color.message_event_bg, R.color.message_event_text)
                isPoll -> Pair(R.color.message_poll_bg, R.color.message_poll_text)
                else -> Pair(R.color.white, R.color.black)
            }

            binding.messageCard.setCardBackgroundColor(ContextCompat.getColor(context, bgColor))
            binding.messageBody.setTextColor(ContextCompat.getColor(context, textColor))
            binding.messageBody.setLinkTextColor(ContextCompat.getColor(context, if (isOwn) R.color.white else R.color.primary_700))
            binding.messageAuthor.setTextColor(ContextCompat.getColor(context, textColor))
            binding.messageTimestamp.setTextColor(ContextCompat.getColor(context, textColor))
            binding.messageMenu.imageTintList = ContextCompat.getColorStateList(context, textColor)

            val params = binding.messageCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.horizontalBias = if (isOwn) 1f else 0f
            binding.messageCard.layoutParams = params

            val reactionsParams = binding.reactionsRow.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            if (isOwn) {
                reactionsParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                reactionsParams.startToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                reactionsParams.endToEnd = binding.messageCard.id
            } else {
                reactionsParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                reactionsParams.startToStart = binding.messageCard.id
            }
            binding.reactionsRow.layoutParams = reactionsParams

            ReactionHelper.bindReactions(
                binding, message, isSystemMessage,
                onSummaryClick = { onShowReactors(message) },
                onReactClick = { onReactMessage(message) }
            )
        }
    }

    class DividerViewHolder(binding: ItemMessageDividerBinding) : RecyclerView.ViewHolder(binding.root)

    private object DiffCallback : DiffUtil.ItemCallback<EventChatItem>() {
        override fun areItemsTheSame(oldItem: EventChatItem, newItem: EventChatItem): Boolean {
            return when {
                oldItem is EventChatItem.Message && newItem is EventChatItem.Message -> oldItem.value.id == newItem.value.id
                oldItem is EventChatItem.Divider && newItem is EventChatItem.Divider -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: EventChatItem, newItem: EventChatItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_MESSAGE = 1
        private const val VIEW_DIVIDER = 2
        private const val MENU_REPORT = 1
    }
}
