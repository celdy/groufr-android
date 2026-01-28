package com.celdy.groufr.data.notifications

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSyncStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getLastCheckAtMs(): Long = prefs.getLong(KEY_LAST_CHECK_MS, 0L)

    fun setLastCheckAtMs(value: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK_MS, value).apply()
    }

    fun getIntervalIndex(): Int = prefs.getInt(KEY_INTERVAL_INDEX, 0)

    fun setIntervalIndex(value: Int) {
        prefs.edit().putInt(KEY_INTERVAL_INDEX, value).apply()
    }

    fun getLastNotifiedId(): Long = prefs.getLong(KEY_LAST_NOTIFIED_ID, 0L)

    fun setLastNotifiedId(value: Long) {
        prefs.edit().putLong(KEY_LAST_NOTIFIED_ID, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "groufr_notification_sync"
        private const val KEY_LAST_CHECK_MS = "last_check_ms"
        private const val KEY_INTERVAL_INDEX = "interval_index"
        private const val KEY_LAST_NOTIFIED_ID = "last_notified_id"
    }
}
