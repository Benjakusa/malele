package com.benjakusa.malele.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LyricsGenerateRequest(
    val theme: String,
    val genre: String? = null,
    val language: String = "en",
    val mood: String? = null,
    val sections: List<String>? = null,
)

@Serializable
data class LyricsGenerateResponse(
    val sections: Map<String, String>,
)
