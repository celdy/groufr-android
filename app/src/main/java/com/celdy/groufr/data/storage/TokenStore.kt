package com.celdy.groufr.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
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

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getExpiresAtEpochSeconds(): Long = prefs.getLong(KEY_EXPIRES_AT, 0L)

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, 0L)

    fun saveTokens(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        userName: String?,
        userId: Long?
    ) {
        val expiresAt = (System.currentTimeMillis() / 1000) + expiresInSeconds
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .putString(KEY_USER_NAME, userName)
            .putLong(KEY_USER_ID, userId ?: 0L)
            .apply()
    }

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_ID)
            .apply()
    }

    fun hasValidAccessToken(): Boolean {
        val token = getAccessToken()
        val expiresAt = getExpiresAtEpochSeconds()
        val now = System.currentTimeMillis() / 1000
        return !token.isNullOrBlank() && expiresAt > now
    }

    fun hasRefreshToken(): Boolean = !getRefreshToken().isNullOrBlank()

    fun isLoggedIn(): Boolean = hasValidAccessToken() || hasRefreshToken()

    fun getOrCreateDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    /**
     * Check if access token needs refresh (will expire within threshold).
     * Used for proactive token refresh before expiration.
     */
    fun needsRefresh(): Boolean {
        val token = getAccessToken()
        val expiresAt = getExpiresAtEpochSeconds()
        val now = System.currentTimeMillis() / 1000
        // Refresh if token is missing or will expire within the threshold
        return token.isNullOrBlank() || (expiresAt - now) <= REFRESH_THRESHOLD_SECONDS
    }

    companion object {
        private const val PREFS_NAME = "groufr_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ID = "user_id"

        /** Refresh tokens 60 seconds before expiration */
        private const val REFRESH_THRESHOLD_SECONDS = 60L
    }
}
