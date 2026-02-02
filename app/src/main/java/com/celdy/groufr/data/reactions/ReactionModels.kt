package com.celdy.groufr.data.reactions

import com.google.gson.annotations.SerializedName

data class ReactionRequest(
    @SerializedName("content_type") val contentType: String,
    @SerializedName("content_id") val contentId: Long,
    @SerializedName("reaction_type") val reactionType: String
)

data class ReactionSummary(
    @SerializedName("user_reaction") val userReaction: String?,
    val counts: Map<String, Int> = emptyMap(),
    @SerializedName("total_count") val totalCount: Int = 0
)

data class ReactionDetail(
    @SerializedName("user_reaction") val userReaction: String?,
    val counts: Map<String, Int> = emptyMap(),
    @SerializedName("total_count") val totalCount: Int = 0,
    val reactors: List<ReactionReactor> = emptyList()
)

data class ReactionReactor(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("user_name") val userName: String,
    @SerializedName("reaction_type") val reactionType: String,
    val emoji: String
)

object ReactionContentType {
    const val MESSAGE = "message"
    const val POLL = "poll"
    const val EVENT = "event"
}

object ReactionType {
    const val THUMBS_UP = "thumbs_up"
    const val HEART = "heart"
    const val THANKS = "thanks"
    const val SAD = "sad"
    const val SURPRISED = "surprised"
}
