package com.celdy.groufr.data.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getNotificationSoundKey(): String =
        prefs.getString(KEY_NOTIFICATION_SOUND, DEFAULT_SOUND) ?: DEFAULT_SOUND

    fun setNotificationSoundKey(key: String) {
        prefs.edit().putString(KEY_NOTIFICATION_SOUND, key).apply()
    }

    fun getNotificationSoundUri(): Uri? {
        val key = getNotificationSoundKey()
        if (key == SOUND_NONE) return null
        val resId = SOUND_RESOURCE_MAP[key] ?: return null
        return Uri.parse("android.resource://${context.packageName}/$resId")
    }

    companion object {
        private const val PREFS_NAME = "groufr_settings"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
        const val DEFAULT_SOUND = "notify_soft_double"
        const val SOUND_NONE = "none"

        val SOUND_KEYS = listOf(
            SOUND_NONE,
            "notify_soft_double",
            "notify_soft_triple",
            "notify_bright_short",
            "notify_low_soft"
        )

        private val SOUND_RESOURCE_MAP = mapOf(
            "notify_soft_double" to com.celdy.groufr.R.raw.notify_soft_double,
            "notify_soft_triple" to com.celdy.groufr.R.raw.notify_soft_triple,
            "notify_bright_short" to com.celdy.groufr.R.raw.notify_bright_short,
            "notify_low_soft" to com.celdy.groufr.R.raw.notify_low_soft
        )

        fun getResourceIdForKey(key: String): Int? = SOUND_RESOURCE_MAP[key]
    }
}
