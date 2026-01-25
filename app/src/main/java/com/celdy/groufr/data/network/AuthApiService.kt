package com.celdy.groufr.data.network

import com.celdy.groufr.data.auth.RefreshRequest
import com.celdy.groufr.data.auth.TokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Separate API service for authentication operations.
 * Used by AuthAuthenticator to refresh tokens without circular dependencies.
 * This service uses a simpler OkHttpClient without the Authenticator.
 */
interface AuthApiService {
    /**
     * Synchronous token refresh for use in OkHttp Authenticator.
     * Returns a Call instead of suspend function since Authenticator runs synchronously.
     */
    @POST("/api/v1/auth/refresh")
    fun refreshSync(@Body request: RefreshRequest): Call<TokenResponse>
}
