package com.benjakusa.malele.domain.model

import org.json.JSONObject
import java.util.UUID

data class Scale(
    val name: String,
    val rootPitch: Int, // MIDI pitch for C3 = 48, C4 = 60, etc.
    val intervals: List<Int>, // Semi-tones from root
) {
    fun isPitchInScale(pitch: Int): Boolean {
        val rel = (pitch - rootPitch) % 12
        val normalizedRel = if (rel < 0) rel + 12 else rel
        return intervals.contains(normalizedRel)
    }

    companion object {
        val C_MINOR = Scale("C Minor", 60, listOf(0, 2, 3, 5, 7, 8, 10))
        val C_MAJOR = Scale("C Major", 60, listOf(0, 2, 4, 5, 7, 9, 11))
        val BENGA_PENTATONIC = Scale("Benga Pentatonic", 60, listOf(0, 2, 4, 7, 9))
        val SOUKOUS_DORIAN = Scale("Soukous Dorian", 60, listOf(0, 2, 3, 5, 7, 9, 10))

        val ALL_SCALES = listOf(BENGA_PENTATONIC, SOUKOUS_DORIAN, C_MINOR, C_MAJOR)
    }
}

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val pitch: Int = 60, // MIDI note (60 = Middle C)
    val startStep: Float = 0f, // 0.0 to 16.0
    val lengthSteps: Float = 1.0f,
    val velocity: Float = 1.0f,
    val glideFromPitch: Int? = null,
    val instrumentType: String = "bass", // bass, piano, organ, guitar, pad, brass
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("pitch", pitch)
            put("startStep", startStep.toDouble())
            put("lengthSteps", lengthSteps.toDouble())
            put("velocity", velocity.toDouble())
            if (glideFromPitch != null) put("glideFromPitch", glideFromPitch)
            put("instrumentType", instrumentType)
        }
    }

    companion object {
        fun fromJson(json: JSONObject?): Note? {
            if (json == null) return null
            return Note(
                id = json.optString("id", UUID.randomUUID().toString()),
                pitch = json.optInt("pitch", 60),
                startStep = json.optDouble("startStep", 0.0).toFloat(),
                lengthSteps = json.optDouble("lengthSteps", 1.0).toFloat(),
                velocity = json.optDouble("velocity", 1.0).toFloat(),
                glideFromPitch = if (json.has("glideFromPitch")) json.getInt("glideFromPitch") else null,
                instrumentType = json.optString("instrumentType", "bass").let { raw ->
                    when (raw) {
                        "guitar" -> "guitar_solo"
                        "pad" -> "synth_pad"
                        "brass" -> "brass_stab"
                        else -> raw
                    }
                },
            )
        }

        fun midiToFreq(pitch: Int): Double {
            return 440.0 * Math.pow(2.0, (pitch - 69).toDouble() / 12.0)
        }

        fun midiToName(pitch: Int): String {
            val names = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
            val octave = (pitch / 12) - 1
            val noteName = names[pitch % 12]
            return "$noteName$octave"
        }
    }
}
