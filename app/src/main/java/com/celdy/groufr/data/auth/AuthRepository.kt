package com.celdy.groufr.data.auth

import com.celdy.groufr.data.device.DeviceInfoProvider
import com.celdy.groufr.data.network.ApiService
import com.celdy.groufr.data.auth.RefreshRequest
import com.celdy.groufr.data.storage.TokenStore
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenStore: TokenStore,
    private val deviceInfoProvider: DeviceInfoProvider
) {
    suspend fun login(email: String, password: String) {
        val request = LoginRequest(
            email = email,
            password = password,
            device = deviceInfoProvider.buildDeviceInfo()
        )
        val response = apiService.login(request)
        tokenStore.saveTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            expiresInSeconds = response.expiresIn,
            userName = response.user.name,
            userId = response.user.id
        )
    }

    /**
     * Ensures a valid session exists, proactively refreshing tokens before they expire.
     * @return true if session is valid (or was successfully refreshed), false otherwise
     */
    suspend fun ensureValidSession(): Boolean {
        // If token is valid and not about to expire, we're good
        if (tokenStore.hasValidAccessToken() && !tokenStore.needsRefresh()) {
            return true
        }

        // Token expired or about to expire - try to refresh
        val refreshToken = tokenStore.getRefreshToken() ?: return false
        return try {
            val response = apiService.refresh(RefreshRequest(refreshToken))
            tokenStore.saveTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresInSeconds = response.expiresIn,
                userName = response.user.name,
                userId = response.user.id
            )
            true
        } catch (exception: Exception) {
            // Refresh failed - if we still have a valid token, keep it
            // The AuthAuthenticator will handle the 401 if needed
            if (tokenStore.hasValidAccessToken()) {
                true
            } else {
                tokenStore.clearTokens()
                false
            }
        }
    }

    fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    fun hasSession(): Boolean = tokenStore.hasValidAccessToken() || tokenStore.hasRefreshToken()

    fun clearTokens() = tokenStore.clearTokens()
}
