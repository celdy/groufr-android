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

    suspend fun ensureValidSession(): Boolean {
        if (tokenStore.hasValidAccessToken()) {
            return true
        }
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
            tokenStore.clearTokens()
            false
        }
    }

    fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    fun hasSession(): Boolean = tokenStore.hasValidAccessToken() || tokenStore.hasRefreshToken()

    fun clearTokens() = tokenStore.clearTokens()
}
