package com.celdy.groufr.ui.common

import androidx.core.view.isVisible
import com.celdy.groufr.R
import com.celdy.groufr.data.messages.MessageDto
import com.celdy.groufr.data.reactions.ReactionType
import com.celdy.groufr.databinding.ItemMessageBinding

object ReactionHelper {

    private val REACTION_EMOJI_MAP = mapOf(
        ReactionType.THUMBS_UP to "\uD83D\uDC4D",
        ReactionType.HEART to "\u2764\uFE0F",
        ReactionType.THANKS to "\uD83D\uDE4F",
        ReactionType.SAD to "\uD83D\uDE22",
        ReactionType.SURPRISED to "\uD83D\uDE2E"
    )

    fun emojiForType(type: String): String = REACTION_EMOJI_MAP[type] ?: ""

    fun bindReactions(
        binding: ItemMessageBinding,
        message: MessageDto,
        isSystemMessage: Boolean,
        onSummaryClick: () -> Unit = {},
        onReactClick: () -> Unit
    ) {
        val reactions = message.reactions

        if (isSystemMessage) {
            binding.reactionsRow.isVisible = false
            return
        }

        binding.reactionsRow.isVisible = true

        // Summary badge: shows only others' reactions (exclude current user)
        val userReaction = reactions?.userReaction
        val othersReactions = if (reactions != null && userReaction != null) {
            reactions.topReactions.mapNotNull { r ->
                val adjusted = if (r.type == userReaction) r.count - 1 else r.count
                if (adjusted > 0) r.copy(count = adjusted) else null
            }
        } else {
            reactions?.topReactions.orEmpty()
        }
        val othersTotal = if (reactions != null && userReaction != null) {
            (reactions.totalCount - 1).coerceAtLeast(0)
        } else {
            reactions?.totalCount ?: 0
        }

        if (othersTotal > 0 && othersReactions.isNotEmpty()) {
            binding.reactionSummary.isVisible = true
            val emojis = othersReactions
                .take(4)
                .joinToString("") { it.emoji }
            binding.reactionSummaryEmojis.text = emojis
            binding.reactionSummaryCount.text = othersTotal.toString()
            binding.reactionSummary.setOnClickListener { onSummaryClick() }
        } else {
            binding.reactionSummary.isVisible = false
            binding.reactionSummary.setOnClickListener(null)
        }

        // My reaction badge
        if (userReaction != null) {
            val emoji = emojiForType(userReaction)
            binding.myReactionEmoji.text = emoji
            binding.myReactionEmoji.isVisible = true
            binding.myReactionIcon.isVisible = false
            binding.myReactionBadge.setBackgroundResource(R.drawable.bg_reaction_badge_active)
        } else {
            binding.myReactionEmoji.isVisible = false
            binding.myReactionIcon.isVisible = true
            binding.myReactionBadge.setBackgroundResource(R.drawable.bg_reaction_badge)
        }

        binding.myReactionBadge.setOnClickListener { onReactClick() }
    }
}
