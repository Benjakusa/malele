package com.benjakusa.malele.domain.model

import org.json.JSONObject
import java.util.UUID

data class VoiceTrack(
    val takeId: String = UUID.randomUUID().toString(),
    val name: String = "Vocal Take",
    val filePath: String = "",
    val offsetMs: Long = 0L,
    val volume: Float = 1.0f,
    val pan: Float = 0.0f,
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("takeId", takeId)
            put("name", name)
            put("filePath", filePath)
            put("offsetMs", offsetMs)
            put("volume", volume.toDouble())
            put("pan", pan.toDouble())
        }
    }

    companion object {
        fun fromJson(json: JSONObject?): VoiceTrack? {
            if (json == null) return null
            return VoiceTrack(
                takeId = json.optString("takeId", UUID.randomUUID().toString()),
                name = json.optString("name", "Vocal Take"),
                filePath = json.optString("filePath", ""),
                offsetMs = json.optLong("offsetMs", 0L),
                volume = json.optDouble("volume", 1.0).toFloat(),
                pan = json.optDouble("pan", 0.0).toFloat(),
            )
        }
    }
}
