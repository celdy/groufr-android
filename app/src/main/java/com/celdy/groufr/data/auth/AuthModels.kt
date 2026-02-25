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

data class LogoutAllResponse(
    @SerializedName("revoked_count")
    val revokedCount: Int
)

data class ForgotPasswordRequest(
    val email: String
)

data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String
)

data class ResetPasswordRequest(
    val token: String,
    val password: String,
    @SerializedName("password_confirmation")
    val passwordConfirmation: String
)

data class ResetPasswordResponse(
    val success: Boolean,
    val message: String
)

data class UpdateProfileRequest(
    val name: String? = null,
    val locale: String? = null,
    val timezone: String? = null
)

data class UserProfileDto(
    val id: Long,
    val name: String,
    val email: String,
    val locale: String? = null,
    val timezone: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

data class ChangePasswordRequest(
    @SerializedName("current_password")
    val currentPassword: String,
    val password: String,
    @SerializedName("password_confirmation")
    val passwordConfirmation: String
)

data class SuccessResponse(
    val success: Boolean
)
