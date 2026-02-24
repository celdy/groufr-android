package com.celdy.groufr.data.device

import android.util.Log
import com.celdy.groufr.data.network.ApiService
import com.celdy.groufr.data.storage.TokenStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushTokenManager @Inject constructor(
    private val apiService: ApiService,
    private val tokenStore: TokenStore
) {
    suspend fun registerTokenIfNeeded() {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            val storedToken = tokenStore.getFcmToken()
            if (fcmToken == storedToken) return

            apiService.registerPushToken(RegisterPushTokenRequest(pushToken = fcmToken))
            tokenStore.setFcmToken(fcmToken)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register push token", e)
        }
    }

    suspend fun unregisterToken() {
        try {
            apiService.deletePushToken()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister push token", e)
        }
        tokenStore.clearFcmToken()
    }

    suspend fun onNewToken(token: String) {
        if (!tokenStore.isLoggedIn()) return
        try {
            apiService.registerPushToken(RegisterPushTokenRequest(pushToken = token))
            tokenStore.setFcmToken(token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register new push token", e)
        }
    }

    companion object {
        private const val TAG = "PushTokenManager"
    }
}
