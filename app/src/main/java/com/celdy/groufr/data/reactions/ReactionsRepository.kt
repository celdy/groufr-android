package com.celdy.groufr.data.reactions

import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class ReactionsRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun toggleReaction(
        contentType: String,
        contentId: Long,
        reactionType: String
    ): ReactionSummary {
        return apiService.toggleReaction(
            ReactionRequest(
                contentType = contentType,
                contentId = contentId,
                reactionType = reactionType
            )
        )
    }

    suspend fun getReactions(
        contentType: String,
        contentId: Long
    ): ReactionDetail {
        return apiService.getReactions(contentType, contentId)
    }

    suspend fun clearReaction(
        contentType: String,
        contentId: Long
    ): ReactionSummary {
        return apiService.clearReaction(contentType, contentId)
    }
}
