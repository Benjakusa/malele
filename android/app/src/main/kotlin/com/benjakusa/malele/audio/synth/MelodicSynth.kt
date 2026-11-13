package com.benjakusa.malele.audio.synth

import com.benjakusa.malele.domain.model.Note
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.pow
import kotlin.random.Random

object MelodicSynth {
    const val SAMPLE_RATE = 44100

    val INSTRUMENT_IDS: List<String> = listOf(
        // Guitars (5)
        "guitar_bass", "guitar_solo", "guitar_rhythm1", "guitar_rhythm2", "guitar_rhythm3",
        // Keys (4)
        "piano", "epiano", "organ", "synth_pad",
        // Orchestral (4)
        "violin", "harp", "flute", "trumpet",
        // Bass (2)
        "bass_pluck", "bass_sub",
        // Leads (2)
        "synth_lead", "brass_stab",
        // Strings (1)
        "strings",
    )

    fun renderNote(
        note: Note,
        stepDurationSec: Double,
        sampleRate: Int = SAMPLE_RATE,
    ): ShortArray {
        val freq = Note.midiToFreq(note.pitch)
        val durationSec = note.lengthSteps * stepDurationSec
        val numSamples = (sampleRate * durationSec).toInt()
        if (numSamples <= 0) return ShortArray(0)

        // Migrate legacy "guitar" to guitar_solo
        val instrument = if (note.instrumentType == "guitar") "guitar_solo" else note.instrumentType

        val raw = when (instrument) {
            "guitar_bass" -> renderGuitarBass(note, freq, durationSec, numSamples, sampleRate)
            "guitar_solo" -> renderGuitarSolo(note, freq, durationSec, numSamples, sampleRate)
            "guitar_rhythm1" -> renderGuitarRhythm1(note, freq, durationSec, numSamples, sampleRate)
            "guitar_rhythm2" -> renderGuitarRhythm2(note, freq, durationSec, numSamples, sampleRate)
            "guitar_rhythm3" -> renderGuitarRhythm3(note, freq, durationSec, numSamples, sampleRate)
            "piano" -> renderPiano(note, freq, durationSec, numSamples, sampleRate)
            "epiano" -> renderEPiano(note, freq, durationSec, numSamples, sampleRate)
            "organ" -> renderOrgan(note, freq, durationSec, numSamples, sampleRate)
            "synth_pad" -> renderSynthPad(note, freq, durationSec, numSamples, sampleRate)
            "violin" -> renderViolin(note, freq, durationSec, numSamples, sampleRate)
            "harp" -> renderHarp(note, freq, durationSec, numSamples, sampleRate)
            "flute" -> renderFlute(note, freq, durationSec, numSamples, sampleRate)
            "trumpet" -> renderTrumpet(note, freq, durationSec, numSamples, sampleRate)
            "bass_pluck" -> renderBassPluck(note, freq, durationSec, numSamples, sampleRate)
            "bass_sub" -> renderBassSub(note, freq, durationSec, numSamples, sampleRate)
            "synth_lead" -> renderSynthLead(note, freq, durationSec, numSamples, sampleRate)
            "brass_stab" -> renderBrassStab(note, freq, durationSec, numSamples, sampleRate)
            "strings" -> renderStrings(note, freq, durationSec, numSamples, sampleRate)
            else -> renderPiano(note, freq, durationSec, numSamples, sampleRate)
        }

        val out = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val clamped = raw[i].coerceIn(-1.0, 1.0)
            out[i] = (clamped * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun renderGuitarBass(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var phase = 0.0
        val f = freq.coerceAtLeast(40.0)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * f / sampleRate
            val body = sin(phase) * 0.6 + sin(phase * 2.0) * 0.25 + sin(phase * 3.0) * 0.1
            val lp = body * (1.0 - exp(-8.0 * t))
            val env = exp(-3.0 * t) * (1.0 - exp(-200.0 * t))
            out[i] = lp * env * note.velocity
        }
        return out
    }

    private fun renderGuitarSolo(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var phase = 0.0
        var lfo = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            lfo += 2.0 * PI * 5.5 / sampleRate
            val vibrato = 1.0 + 0.008 * sin(lfo) * (t / durationSec.coerceAtLeast(0.01))
            phase += 2.0 * PI * freq * vibrato / sampleRate
            val saw = (phase / PI % 2.0) - 1.0
            val ring = sin(phase * 2.0) * 0.15
            val attack = (1.0 - exp(-150.0 * t))
            val env = attack * exp(-2.0 * t)
            out[i] = (saw * 0.7 + ring) * env * note.velocity
        }
        return out
    }

    private fun renderGuitarRhythm1(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var phase = 0.0
        var prevNoise = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * freq / sampleRate
            val pluck = (phase / PI % 2.0) - 1.0
            val noise = Random.nextDouble() * 2.0 - 1.0
            val hp = noise - prevNoise
            prevNoise = noise * 0.5
            val strum = if (t < 0.015) hp * (1.0 - t / 0.015) * 0.4 else 0.0
            val env = (1.0 - exp(-200.0 * t)) * exp(-6.0 * t)
            out[i] = (pluck * 0.6 + strum) * env * note.velocity
        }
        return out
    }

    private fun renderGuitarRhythm2(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var phase = 0.0
        var prev = 0.0
        val cutoffCoeff = 1.0 - exp(-2.0 * PI * 1500.0 / sampleRate)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * freq / sampleRate
            val saw = (phase / PI % 2.0) - 1.0
            prev = prev + cutoffCoeff * (saw - prev)
            val env = (1.0 - exp(-300.0 * t)) * exp(-9.0 * t)
            out[i] = prev * env * note.velocity * 0.9
        }
        return out
    }

    private fun renderGuitarRhythm3(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var phase = 0.0
        var lp = 0.0
        val lpCoeff = 1.0 - exp(-2.0 * PI * 2200.0 / sampleRate)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * freq / sampleRate
            val saw = (phase / PI % 2.0) - 1.0
            lp = lp + lpCoeff * (saw - lp)
            val mid = sin(phase * 2.0) * 0.2
            val env = (1.0 - exp(-400.0 * t)) * exp(-12.0 * t)
            out[i] = (lp + mid) * env * note.velocity * 0.85
        }
        return out
    }

    private fun renderPiano(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * freq / sampleRate
            val f1 = sin(phase)
            val f2 = sin(phase * 2.0) * 0.4 * exp(-3.0 * t)
            val f3 = sin(phase * 3.0) * 0.2 * exp(-5.0 * t)
            val hammer = if (t < 0.005) (Random.nextDouble() * 2.0 - 1.0) * (1.0 - t / 0.005) * 0.15 else 0.0
            val env = (1.0 - exp(-200.0 * t)) * exp(-2.5 * t)
            out[i] = (f1 + f2 + f3 + hammer) * env * note.velocity
        }
        return out
    }

    private fun renderEPiano(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var carrier = 0.0
        var modulator = 0.0
        val modRatio = 3.0
        val modIndex = 2.5 * exp(-2.0 * (1.0 / durationSec.coerceAtLeast(0.01)))
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            modulator += 2.0 * PI * freq * modRatio / sampleRate
            carrier += 2.0 * PI * freq / sampleRate
            val mod = sin(modulator) * modIndex * exp(-3.0 * t)
            val wave = sin(carrier + mod)
            val env = (1.0 - exp(-150.0 * t)) * exp(-2.0 * t)
            out[i] = wave * env * note.velocity
        }
        return out
    }

    private fun renderOrgan(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var p1 = 0.0
        var p2 = 0.0
        var p3 = 0.0
        var p4 = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            p1 += 2.0 * PI * freq / sampleRate
            p2 += 2.0 * PI * freq * 2.0 / sampleRate
            p3 += 2.0 * PI * freq * 3.0 / sampleRate
            p4 += 2.0 * PI * freq * 4.0 / sampleRate
            val wave = sin(p1) + sin(p2) * 0.7 + sin(p3) * 0.5 + sin(p4) * 0.3
            val env = (1.0 - exp(-60.0 * t)) * (0.85 + 0.15 * exp(-1.5 * t))
            out[i] = wave * 0.4 * env * note.velocity
        }
        return out
    }

    private fun renderSynthPad(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var p1 = 0.0
        var p2 = 0.0
        var p3 = 0.0
        var lp = 0.0
        val lpCoeff = 1.0 - exp(-2.0 * PI * 1200.0 / sampleRate)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            p1 += 2.0 * PI * freq / sampleRate
            p2 += 2.0 * PI * freq * 1.005 / sampleRate
            p3 += 2.0 * PI * freq * 0.995 / sampleRate
            val saw1 = (p1 / PI % 2.0) - 1.0
            val saw2 = (p2 / PI % 2.0) - 1.0
            val saw3 = (p3 / PI % 2.0) - 1.0
            val mix = (saw1 + saw2 + saw3) * 0.33
            lp = lp + lpCoeff * (mix - lp)
            val env = (1.0 - exp(-3.0 * t)) * exp(-0.3 * t)
            out[i] = lp * env * note.velocity
        }
        return out
    }

    private fun renderBassPluck(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var phase = 0.0
        val f = freq.coerceAtLeast(35.0)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * f / sampleRate
            val sine = sin(phase)
            val saw = (phase / PI % 2.0) - 1.0
            val env = (1.0 - exp(-250.0 * t)) * exp(-4.0 * t)
            out[i] = (sine * 0.6 + saw * 0.4) * env * note.velocity
        }
        return out
    }

    private fun renderBassSub(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var phase = 0.0
        val f = freq.coerceIn(30.0, 90.0)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * f / sampleRate
            val wave = sin(phase)
            val env = (1.0 - exp(-30.0 * t)) * exp(-2.0 * t)
            out[i] = wave * env * note.velocity * 1.05
        }
        return out
    }

    private fun renderSynthLead(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var carrier = 0.0
        var modulator = 0.0
        var lfo = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            lfo += 2.0 * PI * 5.5 / sampleRate
            val vibrato = 1.0 + 0.006 * sin(lfo)
            modulator += 2.0 * PI * freq * 2.0 * vibrato / sampleRate
            carrier += 2.0 * PI * freq * vibrato / sampleRate
            val mod = sin(modulator) * 1.5
            val wave = sin(carrier + mod)
            val env = (1.0 - exp(-120.0 * t)) * exp(-2.5 * t)
            out[i] = wave * env * note.velocity
        }
        return out
    }

    private fun renderBrassStab(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var phase = 0.0
        var lp = 0.0
        val lpCoeff = 1.0 - exp(-2.0 * PI * 3000.0 / sampleRate)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * freq / sampleRate
            val saw = (phase / PI % 2.0) - 1.0
            lp = lp + lpCoeff * (saw - lp)
            val env = (1.0 - exp(-400.0 * t)) * exp(-6.0 * t)
            out[i] = lp * env * note.velocity
        }
        return out
    }

    private fun renderViolin(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var phase = 0.0
        var bowNoise = 0.0
        var lp = 0.0
        val lpCoeff = 1.0 - exp(-2.0 * PI * 3500.0 / sampleRate)
        var lfo = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            lfo += 2.0 * PI * 5.0 / sampleRate
            val vibrato = 1.0 + 0.006 * sin(lfo)
            phase += 2.0 * PI * freq * vibrato / sampleRate
            val saw = (phase / PI % 2.0) - 1.0
            lp = lp + lpCoeff * (saw - lp)
            bowNoise = bowNoise * 0.9 + (Random.nextDouble() * 2.0 - 1.0) * 0.1
            val env = (1.0 - exp(-80.0 * t)) * exp(-0.5 * t)
            out[i] = (lp * 0.85 + bowNoise * 0.15) * env * note.velocity
        }
        return out
    }

    private fun renderHarp(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var phase = 0.0
        var phase2 = 0.0
        var phase3 = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * freq / sampleRate
            phase2 += 2.0 * PI * freq * 2.0 / sampleRate
            phase3 += 2.0 * PI * freq * 3.0 / sampleRate
            val pluck = sin(phase) + sin(phase2) * 0.35 * exp(-5.0 * t) + sin(phase3) * 0.15 * exp(-8.0 * t)
            val env = (1.0 - exp(-500.0 * t)) * exp(-1.8 * t)
            out[i] = pluck * env * note.velocity
        }
        return out
    }

    private fun renderFlute(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var phase = 0.0
        var breath = 0.0
        val breathCoeff = 1.0 - exp(-2.0 * PI * 2500.0 / sampleRate)
        var lfo = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            lfo += 2.0 * PI * 5.0 / sampleRate
            val vibrato = 1.0 + 0.003 * sin(lfo) * (t / 0.3).coerceAtMost(1.0)
            phase += 2.0 * PI * freq * vibrato / sampleRate
            val tone = sin(phase)
            breath = breath + breathCoeff * ((Random.nextDouble() * 2.0 - 1.0) - breath)
            val env = (1.0 - exp(-15.0 * t)) * exp(-0.6 * t)
            out[i] = (tone * 0.9 + breath * 0.15) * env * note.velocity
        }
        return out
    }

    private fun renderTrumpet(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var phase = 0.0
        var lp = 0.0
        var lp2 = 0.0
        val c1 = 1.0 - exp(-2.0 * PI * 1800.0 / sampleRate)
        val c2 = 1.0 - exp(-2.0 * PI * 800.0 / sampleRate)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            phase += 2.0 * PI * freq / sampleRate
            val saw = (phase / PI % 2.0) - 1.0
            lp = lp + c1 * (saw - lp)
            lp2 = lp2 + c2 * (lp - lp2)
            val formant = sin(phase * 3.0) * 0.25 * exp(-4.0 * t)
            val env = (1.0 - exp(-50.0 * t)) * exp(-2.0 * t)
            out[i] = (lp2 * 0.85 + formant) * env * note.velocity
        }
        return out
    }

    private fun renderStrings(note: Note, freq: Double, durationSec: Double, numSamples: Int, sampleRate: Int): DoubleArray {
        val out = DoubleArray(numSamples)
        var p1 = 0.0
        var p2 = 0.0
        var p3 = 0.0
        var p4 = 0.0
        var lp = 0.0
        val lpCoeff = 1.0 - exp(-2.0 * PI * 2800.0 / sampleRate)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            p1 += 2.0 * PI * freq * 0.997 / sampleRate
            p2 += 2.0 * PI * freq * 1.0 / sampleRate
            p3 += 2.0 * PI * freq * 1.003 / sampleRate
            p4 += 2.0 * PI * freq * 1.006 / sampleRate
            val saw1 = (p1 / PI % 2.0) - 1.0
            val saw2 = (p2 / PI % 2.0) - 1.0
            val saw3 = (p3 / PI % 2.0) - 1.0
            val saw4 = (p4 / PI % 2.0) - 1.0
            val mix = (saw1 + saw2 + saw3 + saw4) * 0.25
            lp = lp + lpCoeff * (mix - lp)
            val env = (1.0 - exp(-8.0 * t)) * exp(-0.4 * t)
            out[i] = lp * env * note.velocity
        }
        return out
    }
}
