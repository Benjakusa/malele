package com.benjakusa.malele.audio.synth

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

object Dsp {

    fun applyLowPassFilter(
        samples: ShortArray,
        cutoffHz: Float,
        resonance: Float = 1.0f,
        sampleRate: Int = 44100,
    ): ShortArray {
        if (cutoffHz >= 19000f) return samples

        val result = ShortArray(samples.size)
        val omega = 2.0 * PI * cutoffHz / sampleRate
        val alpha = sin(omega) / (2.0 * resonance)
        val cosW = cos(omega)

        val b0 = (1.0 - cosW) / 2.0
        val b1 = 1.0 - cosW
        val b2 = (1.0 - cosW) / 2.0
        val a0 = 1.0 + alpha
        val a1 = -2.0 * cosW
        val a2 = 1.0 - alpha

        var x1 = 0.0; var x2 = 0.0
        var y1 = 0.0; var y2 = 0.0

        for (i in samples.indices) {
            val x0 = samples[i] / 32768.0
            val y0 = (b0 / a0) * x0 + (b1 / a0) * x1 + (b2 / a0) * x2 - (a1 / a0) * y1 - (a2 / a0) * y2
            x2 = x1; x1 = x0
            y2 = y1; y1 = y0

            val clamped = y0.coerceIn(-1.0, 1.0)
            result[i] = (clamped * 32767.0).toInt().toShort()
        }
        return result
    }

    fun applySaturation(
        samples: ShortArray,
        drive: Float,
    ): ShortArray {
        if (drive <= 0.01f) return samples
        val result = ShortArray(samples.size)
        val k = 1.0 + drive * 5.0

        for (i in samples.indices) {
            val x = samples[i] / 32768.0
            val sat = tanh(x * k) / tanh(k)
            result[i] = (sat.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
        }
        return result
    }

    fun applyDelay(
        samples: ShortArray,
        mix: Float,
        delaySec: Float,
        sampleRate: Int = 44100,
    ): ShortArray {
        if (mix <= 0.01f) return samples
        val result = ShortArray(samples.size)
        val delaySamples = (delaySec * sampleRate).toInt().coerceAtLeast(1)

        for (i in samples.indices) {
            val dry = samples[i] / 32768.0
            val delayedIndex = i - delaySamples
            val wet = if (delayedIndex >= 0) samples[delayedIndex] / 32768.0 else 0.0

            val out = dry * (1.0 - mix * 0.5) + wet * mix * 0.5
            result[i] = (out.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
        }
        return result
    }
}
