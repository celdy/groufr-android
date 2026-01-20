package com.celdy.groufr.data.auth

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String,
    val device: DeviceInfo,
    @SerializedName("remember_me")
    val rememberMe: Boolean = true
)

data class DeviceInfo(
    val uuid: String,
    val platform: String,
    val name: String,
    @SerializedName("app_version")
    val appVersion: String,
    @SerializedName("os_version")
    val osVersion: String
)

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
    @SerializedName("token_type")
    val tokenType: String,
    val user: UserDto
)

data class UserDto(
    val id: Long,
    val name: String,
    val email: String
)

data class RefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)
