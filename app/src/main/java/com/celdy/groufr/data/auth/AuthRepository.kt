package com.celdy.groufr.data.auth

import com.celdy.groufr.data.device.DeviceInfoProvider
import com.celdy.groufr.data.network.ApiService
import com.celdy.groufr.data.auth.RefreshRequest
import com.celdy.groufr.data.local.UserDao
import com.celdy.groufr.data.local.toEntity
import com.celdy.groufr.data.storage.TokenStore
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenStore: TokenStore,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val userDao: UserDao
) {
    private val refreshMutex = Mutex()

    suspend fun login(email: String, password: String) {
        val request = LoginRequest(
            email = email,
            password = password,
            device = deviceInfoProvider.buildDeviceInfo()
        )
        val response = apiService.login(request)
        userDao.upsert(response.user.toEntity())
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

        return refreshMutex.withLock {
            // Another caller might have refreshed while we waited
            if (tokenStore.hasValidAccessToken() && !tokenStore.needsRefresh()) {
                return@withLock true
            }

            // No refresh token means no session
            val refreshToken = tokenStore.getRefreshToken() ?: return@withLock false

            // Token expired or about to expire - try to refresh
            try {
                val response = apiService.refresh(RefreshRequest(refreshToken))
                userDao.upsert(response.user.toEntity())
                tokenStore.saveTokens(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    expiresInSeconds = response.expiresIn,
                    userName = response.user.name,
                    userId = response.user.id
                )
                true
            } catch (e: HttpException) {
                // HTTP 401 means refresh token is invalid/expired - clear tokens
                if (e.code() == 401) {
                    tokenStore.clearTokens()
                    false
                } else {
                    // Other HTTP errors (5xx, etc.) - keep tokens, let user retry
                    // Return true if we have a valid access token, false to show error
                    tokenStore.hasValidAccessToken()
                }
            } catch (e: IOException) {
                // Network error - don't clear tokens, user might have valid refresh token
                // Return true if we have a valid access token to let user continue
                // Return false to show error but keep tokens for retry
                tokenStore.hasValidAccessToken()
            } catch (e: Exception) {
                // Unknown error - be conservative, don't clear tokens
                tokenStore.hasValidAccessToken()
            }
        }
    }

    fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    fun hasSession(): Boolean = tokenStore.hasValidAccessToken() || tokenStore.hasRefreshToken()

    fun clearTokens() = tokenStore.clearTokens()
}
