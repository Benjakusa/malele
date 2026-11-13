package com.benjakusa.malele.domain.model

import org.json.JSONObject

data class InstrumentParams(
    // 1. Sound (8)
    val pitch: Float = 0f, // semitones -12..12
    val fineTune: Float = 0f, // cents -50..50
    val volume: Float = 1.0f, // 0..1
    val pan: Float = 0f, // -1..1
    val attack: Float = 0.01f, // seconds 0..2
    val decay: Float = 0.3f, // seconds 0.01..5
    val sustain: Float = 0.7f, // 0..1
    val release: Float = 0.5f, // seconds 0.01..5

    // 2. Tone (6)
    val lowPassCutoff: Float = 20000f, // Hz 20..20000
    val highPassCutoff: Float = 20f, // Hz 20..10000
    val resonance: Float = 1.0f, // Q 0.5..10
    val eqLowGain: Float = 0f, // dB -12..12
    val eqMidGain: Float = 0f, // dB -12..12
    val eqHighGain: Float = 0f, // dB -12..12

    // 3. FX (7)
    val reverbAmount: Float = 0f, // 0..1
    val reverbSize: Float = 0.5f, // 0..1
    val delayMix: Float = 0f, // 0..1
    val delayTimeSec: Float = 0.25f, // 0.05..1.0
    val compressionThreshold: Float = 0f, // dB -30..0
    val saturationAmount: Float = 0f, // 0..1
    val stereoWidth: Float = 1.0f, // 0 (mono) .. 2 (wide)

    // 4. Timing (4)
    val swingAmount: Float = 0f, // 0..1
    val nudgeMs: Float = 0f, // -50ms .. +50ms
    val humanizeAmount: Float = 0f, // 0..1
    val gateLength: Float = 1.0f, // 0.1..2.0 multiplier

    // 5. Routing (4)
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val sidechainSourceTrack: Int = -1, // -1 = none, 0..3 track index
    val sidechainAmount: Float = 0f, // 0..1
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("pitch", pitch.toDouble())
            put("fineTune", fineTune.toDouble())
            put("volume", volume.toDouble())
            put("pan", pan.toDouble())
            put("attack", attack.toDouble())
            put("decay", decay.toDouble())
            put("sustain", sustain.toDouble())
            put("release", release.toDouble())

            put("lowPassCutoff", lowPassCutoff.toDouble())
            put("highPassCutoff", highPassCutoff.toDouble())
            put("resonance", resonance.toDouble())
            put("eqLowGain", eqLowGain.toDouble())
            put("eqMidGain", eqMidGain.toDouble())
            put("eqHighGain", eqHighGain.toDouble())

            put("reverbAmount", reverbAmount.toDouble())
            put("reverbSize", reverbSize.toDouble())
            put("delayMix", delayMix.toDouble())
            put("delayTimeSec", delayTimeSec.toDouble())
            put("compressionThreshold", compressionThreshold.toDouble())
            put("saturationAmount", saturationAmount.toDouble())
            put("stereoWidth", stereoWidth.toDouble())

            put("swingAmount", swingAmount.toDouble())
            put("nudgeMs", nudgeMs.toDouble())
            put("humanizeAmount", humanizeAmount.toDouble())
            put("gateLength", gateLength.toDouble())

            put("isMuted", isMuted)
            put("isSolo", isSolo)
            put("sidechainSourceTrack", sidechainSourceTrack)
            put("sidechainAmount", sidechainAmount.toDouble())
        }
    }

    companion object {
        fun fromJson(json: JSONObject?): InstrumentParams {
            if (json == null) return InstrumentParams()
            return InstrumentParams(
                pitch = json.optDouble("pitch", 0.0).toFloat(),
                fineTune = json.optDouble("fineTune", 0.0).toFloat(),
                volume = json.optDouble("volume", 1.0).toFloat(),
                pan = json.optDouble("pan", 0.0).toFloat(),
                attack = json.optDouble("attack", 0.01).toFloat(),
                decay = json.optDouble("decay", 0.3).toFloat(),
                sustain = json.optDouble("sustain", 0.7).toFloat(),
                release = json.optDouble("release", 0.5).toFloat(),

                lowPassCutoff = json.optDouble("lowPassCutoff", 20000.0).toFloat(),
                highPassCutoff = json.optDouble("highPassCutoff", 20.0).toFloat(),
                resonance = json.optDouble("resonance", 1.0).toFloat(),
                eqLowGain = json.optDouble("eqLowGain", 0.0).toFloat(),
                eqMidGain = json.optDouble("eqMidGain", 0.0).toFloat(),
                eqHighGain = json.optDouble("eqHighGain", 0.0).toFloat(),

                reverbAmount = json.optDouble("reverbAmount", 0.0).toFloat(),
                reverbSize = json.optDouble("reverbSize", 0.5).toFloat(),
                delayMix = json.optDouble("delayMix", 0.0).toFloat(),
                delayTimeSec = json.optDouble("delayTimeSec", 0.25).toFloat(),
                compressionThreshold = json.optDouble("compressionThreshold", 0.0).toFloat(),
                saturationAmount = json.optDouble("saturationAmount", 0.0).toFloat(),
                stereoWidth = json.optDouble("stereoWidth", 1.0).toFloat(),

                swingAmount = json.optDouble("swingAmount", 0.0).toFloat(),
                nudgeMs = json.optDouble("nudgeMs", 0.0).toFloat(),
                humanizeAmount = json.optDouble("humanizeAmount", 0.0).toFloat(),
                gateLength = json.optDouble("gateLength", 1.0).toFloat(),

                isMuted = json.optBoolean("isMuted", false),
                isSolo = json.optBoolean("isSolo", false),
                sidechainSourceTrack = json.optInt("sidechainSourceTrack", -1),
                sidechainAmount = json.optDouble("sidechainAmount", 0.0).toFloat(),
            )
        }
    }
}
