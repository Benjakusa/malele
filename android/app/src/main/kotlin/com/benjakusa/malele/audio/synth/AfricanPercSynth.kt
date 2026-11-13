package com.benjakusa.malele.audio.synth

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object AfricanPercSynth {
    const val SAMPLE_RATE = 44100

    fun renderConga(sampleRate: Int = SAMPLE_RATE, pitchHz: Double = 200.0, velocity: Float = 1.0f): ShortArray {
        val durationSec = 0.25f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * pitchHz / sampleRate
            val tone = sin(phase) * exp(-12.0 * t)
            val slap = (Random.nextDouble() * 2.0 - 1.0) * exp(-40.0 * t) * 0.3

            val sampleVal = (tone * 0.7 + slap) * velocity.toDouble()
            val clamped = sampleVal.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * 32767.0).toInt().toShort()
        }
        return buffer
    }

    fun renderDjembe(sampleRate: Int = SAMPLE_RATE, type: String = "bass", velocity: Float = 1.0f): ShortArray {
        val freq = when (type) {
            "slap" -> 400.0
            "tone" -> 220.0
            else -> 80.0 // "bass"
        }
        val durationSec = if (type == "bass") 0.35f else 0.18f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * freq / sampleRate
            val tone = sin(phase) * exp(-10.0 * t)
            val noise = (Random.nextDouble() * 2.0 - 1.0) * exp(-30.0 * t) * 0.4

            val sampleVal = (tone * 0.6 + noise * 0.4) * velocity.toDouble()
            val clamped = sampleVal.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * 32767.0).toInt().toShort()
        }
        return buffer
    }

    fun renderShaker(sampleRate: Int = SAMPLE_RATE, velocity: Float = 1.0f): ShortArray {
        val durationSec = 0.06f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val noise = Random.nextDouble() * 2.0 - 1.0
            val env = exp(-45.0 * t)
            val sampleVal = noise * env * velocity.toDouble() * 0.6
            val clamped = sampleVal.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * 32767.0).toInt().toShort()
        }
        return buffer
    }

    fun renderTalkingDrum(sampleRate: Int = SAMPLE_RATE, velocity: Float = 1.0f): ShortArray {
        val durationSec = 0.30f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        val startFreq = 120.0
        val endFreq = 260.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Upward pitch bend simulation
            val currentFreq = startFreq + (endFreq - startFreq) * (t / durationSec)
            phase += 2.0 * PI * currentFreq / sampleRate
            val tone = sin(phase) * exp(-6.0 * t)

            val sampleVal = tone * velocity.toDouble() * 0.8
            val clamped = sampleVal.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * 32767.0).toInt().toShort()
        }
        return buffer
    }

    fun renderKalimba(sampleRate: Int = SAMPLE_RATE, pitchHz: Double = 523.25, velocity: Float = 1.0f): ShortArray {
        // C5 default kalimba tine
        val durationSec = 0.50f
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * pitchHz / sampleRate
            val sine = sin(phase)
            val metallicHarmonic = sin(phase * 2.76) * 0.3 * exp(-30.0 * t)
            val env = exp(-4.0 * t)

            val sampleVal = (sine + metallicHarmonic) * env * velocity.toDouble() * 0.7
            val clamped = sampleVal.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * 32767.0).toInt().toShort()
        }
        return buffer
    }
}
