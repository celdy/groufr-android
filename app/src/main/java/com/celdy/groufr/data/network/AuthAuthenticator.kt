package com.celdy.groufr.data.network

import com.celdy.groufr.data.auth.RefreshRequest
import com.celdy.groufr.data.storage.TokenStore
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * OkHttp Authenticator that handles 401 Unauthorized responses by refreshing tokens.
 * This enables persistent login by automatically renewing expired access tokens.
 */
class AuthAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val authApiService: AuthApiService
) : Authenticator {

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_REFRESH_ATTEMPTED = "X-Refresh-Attempted"
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry if we already attempted a refresh for this request
        if (response.request.header(HEADER_REFRESH_ATTEMPTED) != null) {
            // Refresh already attempted and failed - clear tokens and give up
            tokenStore.clearTokens()
            return null
        }

        // Synchronize to prevent multiple concurrent refresh attempts
        synchronized(this) {
            // Double-check: maybe another thread already refreshed
            val currentToken = tokenStore.getAccessToken()
            val originalToken = response.request.header(HEADER_AUTHORIZATION)
                ?.removePrefix("Bearer ")

            // If tokens differ, another thread already refreshed - retry with new token
            if (currentToken != null && currentToken != originalToken) {
                return response.request.newBuilder()
                    .header(HEADER_AUTHORIZATION, "Bearer $currentToken")
                    .header(HEADER_REFRESH_ATTEMPTED, "true")
                    .build()
            }

            // Get refresh token
            val refreshToken = tokenStore.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                tokenStore.clearTokens()
                return null
            }

            // Attempt to refresh tokens
            return try {
                val refreshResponse = authApiService
                    .refreshSync(RefreshRequest(refreshToken))
                    .execute()

                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    val tokens = refreshResponse.body()!!

                    // Store new tokens
                    tokenStore.saveTokens(
                        accessToken = tokens.accessToken,
                        refreshToken = tokens.refreshToken,
                        expiresInSeconds = tokens.expiresIn,
                        userName = tokens.user.name,
                        userId = tokens.user.id
                    )

                    // Retry the original request with new access token
                    response.request.newBuilder()
                        .header(HEADER_AUTHORIZATION, "Bearer ${tokens.accessToken}")
                        .header(HEADER_REFRESH_ATTEMPTED, "true")
                        .build()
                } else {
                    // Refresh failed - clear tokens and require re-login
                    tokenStore.clearTokens()
                    null
                }
            } catch (e: Exception) {
                // Network error during refresh - clear tokens
                tokenStore.clearTokens()
                null
            }
        }
    }
}
