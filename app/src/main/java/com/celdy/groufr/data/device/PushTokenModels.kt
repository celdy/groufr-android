package com.celdy.groufr.data.device

import com.google.gson.annotations.SerializedName

data class RegisterPushTokenRequest(
    @SerializedName("push_token")
    val pushToken: String,
    val provider: String = "fcm"
)

data class RegisterPushTokenResponse(
    val success: Boolean
)
