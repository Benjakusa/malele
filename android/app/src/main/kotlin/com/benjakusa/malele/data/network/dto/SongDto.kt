package com.benjakusa.malele.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaveSongRequest(
    val title: String? = null,
)

@Serializable
data class SongDto(
    val id: String,
    val title: String,
    @SerialName("job_id") val jobId: String? = null,
    val genre: String? = null,
    val mood: String? = null,
    val language: String? = null,
    @SerialName("is_favourite") val isFavourite: Boolean = false,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)
