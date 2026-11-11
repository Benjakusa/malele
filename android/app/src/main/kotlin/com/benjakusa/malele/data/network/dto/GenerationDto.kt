package com.benjakusa.malele.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerationSubmitRequest(
    val prompt: String,
    val genre: String? = null,
    val vocals: Boolean = true,
    val instrumental: Boolean = false,
    val lyrics: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int = 120,
    val language: String? = null,
)

@Serializable
data class JobEventDto(
    val event: String,
    @SerialName("created_at") val createdAt: String,
    val detail: Map<String, String> = emptyMap(),
)

@Serializable
data class JobDto(
    val id: String,
    val state: String,
    val provider: String,
    @SerialName("prompt_original") val promptOriginal: String,
    @SerialName("credits_reserved") val creditsReserved: Int = 0,
    @SerialName("credits_charged") val creditsCharged: Int = 0,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val events: List<JobEventDto> = emptyList(),
)

@Serializable
data class GenerationSubmitResponse(
    val job: JobDto,
)
