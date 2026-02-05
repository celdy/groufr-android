package com.celdy.groufr.data.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatLastSeenStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLastSeenMessageId(threadType: String, threadId: Long): Long {
        return prefs.getLong("${KEY_PREFIX}${threadType}_$threadId", 0L)
    }

    fun setLastSeenMessageId(threadType: String, threadId: Long, messageId: Long) {
        val current = getLastSeenMessageId(threadType, threadId)
        if (messageId > current) {
            prefs.edit().putLong("${KEY_PREFIX}${threadType}_$threadId", messageId).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "groufr_chat_last_seen"
        private const val KEY_PREFIX = "last_seen_"
    }
}
