package com.benjakusa.malele.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class TokenPair(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Int = 900,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val role: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class AuthResponse(
    val user: UserDto,
    val tokens: TokenPair,
)

@Serializable
data class ErrorBody(
    val error: ErrorDetail? = null,
)

@Serializable
data class ErrorDetail(
    val code: String = "UNKNOWN",
    val message: String = "",
)
