package com.celdy.groufr.data.calendar

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarSyncStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun getLastSyncMs(): Long = prefs.getLong(KEY_LAST_SYNC_MS, 0L)

    fun setLastSyncMs(value: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_MS, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "groufr_calendar_sync"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LAST_SYNC_MS = "last_sync_ms"
    }
}
