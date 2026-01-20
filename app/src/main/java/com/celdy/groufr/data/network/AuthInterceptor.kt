package com.celdy.groufr.data.network

import com.celdy.groufr.data.storage.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenStore.getAccessToken()
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        val response = chain.proceed(request)
        if (response.code == 401) {
            tokenStore.clearTokens()
        }
        return response
    }
}
