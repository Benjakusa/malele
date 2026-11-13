package com.benjakusa.malele.audio.synth

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object DrumSynth {
    const val SAMPLE_RATE = 44100

    fun renderKick(sampleRate: Int = SAMPLE_RATE, velocity: Float = 1.0f): ShortArray {
        val durationSec = 0.25f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        val startFreq = 150.0
        val endFreq = 45.0
        val decayRate = 25.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val currentFreq = endFreq + (startFreq - endFreq) * exp(-decayRate * t)
            phase += 2.0 * PI * currentFreq / sampleRate
            val sineVal = sin(phase)

            val env = exp(-8.0 * t)
            val click = if (i < (sampleRate * 0.005)) (1.0 - i / (sampleRate * 0.005)) * 0.3 else 0.0
            val sampleVal = (sineVal * env + click) * velocity.toDouble()
            val clamped = sampleVal.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * 32767.0).toInt().toShort()
        }
        return buffer
    }

    fun renderSnare(sampleRate: Int = SAMPLE_RATE, velocity: Float = 1.0f): ShortArray {
        val durationSec = 0.20f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        val toneFreq = 180.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * toneFreq / sampleRate
            val tone = sin(phase) * exp(-15.0 * t)
            val noise = (Random.nextDouble() * 2.0 - 1.0) * exp(-12.0 * t)

            val sampleVal = (tone * 0.4 + noise * 0.6) * velocity.toDouble()
            val clamped = sampleVal.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * 32767.0).toInt().toShort()
        }
        return buffer
    }

    fun renderHat(sampleRate: Int = SAMPLE_RATE, velocity: Float = 1.0f): ShortArray {
        val durationSec = 0.08f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var lastNoise = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val rawNoise = Random.nextDouble() * 2.0 - 1.0
            // Simple high-pass filter: noise minus previous low-pass component
            val hpNoise = rawNoise - lastNoise
            lastNoise = rawNoise * 0.5

            val env = exp(-35.0 * t)
            val sampleVal = hpNoise * env * velocity.toDouble() * 0.7
            val clamped = sampleVal.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * 32767.0).toInt().toShort()
        }
        return buffer
    }

    fun renderBass(sampleRate: Int = SAMPLE_RATE, velocity: Float = 1.0f, freqHz: Double = 65.41): ShortArray {
        // C2 default (~65.41Hz)
        val durationSec = 0.35f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var phaseSine = 0.0
        var phaseSaw = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phaseSine += 2.0 * PI * freqHz / sampleRate
            phaseSaw += freqHz / sampleRate
            if (phaseSaw > 1.0) phaseSaw -= 1.0

            val sine = sin(phaseSine)
            val saw = (phaseSaw * 2.0 - 1.0)
            val env = exp(-5.0 * t)

            val sampleVal = (sine * 0.7 + saw * 0.3) * env * velocity.toDouble() * 0.8
            val clamped = sampleVal.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * 32767.0).toInt().toShort()
        }
        return buffer
    }

    fun renderTrack(trackType: Int, sampleRate: Int = SAMPLE_RATE, velocity: Float = 1.0f): ShortArray {
        return when (trackType) {
            0 -> renderKick(sampleRate, velocity)
            1 -> renderSnare(sampleRate, velocity)
            2 -> renderHat(sampleRate, velocity)
            3 -> renderBass(sampleRate, velocity)
            else -> ShortArray(0)
        }
    }

    /**
     * String-based track dispatcher added in 7G-A.
     * Delegates to the four built-in drum synths for the default tracks,
     * and to AfricanPercSynth for the African percussion tracks.
     * The old Int-based overload above is preserved unchanged.
     */
    fun renderTrack(trackId: String, sampleRate: Int = SAMPLE_RATE, velocity: Float = 1.0f): ShortArray {
        return when (trackId) {
            "kick" -> renderKick(sampleRate, velocity)
            "snare" -> renderSnare(sampleRate, velocity)
            "hat" -> renderHat(sampleRate, velocity)
            "bass" -> renderBass(sampleRate, velocity)
            "conga" -> AfricanPercSynth.renderConga(sampleRate, velocity = velocity)
            "djembe" -> AfricanPercSynth.renderDjembe(sampleRate, velocity = velocity)
            "shaker" -> AfricanPercSynth.renderShaker(sampleRate, velocity = velocity)
            "talking_drum" -> AfricanPercSynth.renderTalkingDrum(sampleRate, velocity = velocity)
            "kalimba" -> AfricanPercSynth.renderKalimba(sampleRate, velocity = velocity)
            else -> ShortArray(0)
        }
    }
}
