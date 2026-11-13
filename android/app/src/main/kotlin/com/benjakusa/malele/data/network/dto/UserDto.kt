package com.benjakusa.malele.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: String,
    val email: String,
    val role: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class UpdateMeRequest(
    @SerialName("display_name") val displayName: String? = null,
)
