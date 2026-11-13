package com.benjakusa.malele.audio.export

import android.content.Context
import com.benjakusa.malele.audio.synth.DrumSynth
import com.benjakusa.malele.audio.synth.MelodicSynth
import com.benjakusa.malele.domain.model.DrumPattern
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.DataOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Singleton
class WavExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val exportsDir: File
        get() = File(context.filesDir, "exports").also { if (!it.exists()) it.mkdirs() }

    suspend fun exportToWav(pattern: DrumPattern, numBars: Int = 4): File = withContext(Dispatchers.IO) {
        val sampleRate = DrumSynth.SAMPLE_RATE
        val channels = 2 // Stereo
        val bitsPerSample = 16

        val bpm = pattern.bpm
        val stepDurationSec = 60.0 / (bpm * 4)
        val samplesPerStep = (sampleRate * stepDurationSec).toInt()
        val totalSteps = DrumPattern.STEP_COUNT * numBars
        val totalSamplesPerChannel = samplesPerStep * totalSteps

        val outputFile = File(exportsDir, "${pattern.name.replace("\\s+".toRegex(), "_")}_${System.currentTimeMillis()}.wav")
        val pcmLeft = FloatArray(totalSamplesPerChannel)
        val pcmRight = FloatArray(totalSamplesPerChannel)

        val mixer = pattern.mixer
        val hasAnySolo = mixer.solos.any { it }

        // Render each bar and step
        for (bar in 0 until numBars) {
            for (step in 0 until DrumPattern.STEP_COUNT) {
                val globalStepIndex = bar * DrumPattern.STEP_COUNT + step
                val startSampleIndex = globalStepIndex * samplesPerStep

                // 1. Render Drums (trackIds-driven)
                for (trackIdx in pattern.trackIds.indices) {
                    val isMuted = trackIdx < mixer.mutes.size && mixer.mutes[trackIdx]
                    val isSolo = trackIdx < mixer.solos.size && mixer.solos[trackIdx]

                    if (isMuted) continue
                    if (hasAnySolo && !isSolo) continue

                    if (trackIdx < pattern.steps.size && pattern.steps[trackIdx][step]) {
                        val trackVol = if (trackIdx < pattern.volumes.size) pattern.volumes[trackIdx] else 1.0f
                        val synthSamples = DrumSynth.renderTrack(pattern.trackIds[trackIdx], sampleRate, trackVol)
                        val length = minOf(synthSamples.size, totalSamplesPerChannel - startSampleIndex)

                        val pan = mixer.pans[trackIdx]
                        val angle = (pan + 1.0f) * (PI.toFloat() / 4.0f)
                        val leftGain = cos(angle)
                        val rightGain = sin(angle)

                        for (i in 0 until length) {
                            val sample = synthSamples[i] / 32768.0f
                            val targetIndex = startSampleIndex + i
                            pcmLeft[targetIndex] += sample * leftGain
                            pcmRight[targetIndex] += sample * rightGain
                        }
                    }
                }

                // 2. Render Melodic Piano Roll Notes
                val activeNotes = pattern.notes.filter { it.startStep.toInt() == step }
                for (note in activeNotes) {
                    val synthSamples = MelodicSynth.renderNote(note, stepDurationSec, sampleRate)
                    val length = minOf(synthSamples.size, totalSamplesPerChannel - startSampleIndex)

                    for (i in 0 until length) {
                        val sample = synthSamples[i] / 32768.0f
                        val targetIndex = startSampleIndex + i
                        pcmLeft[targetIndex] += sample * 0.707f
                        pcmRight[targetIndex] += sample * 0.707f
                    }
                }
            }
        }

        // Apply master volume and write 16-bit stereo WAV
        val dataSize = totalSamplesPerChannel * channels * 2
        FileOutputStream(outputFile).use { fos ->
            writeWavHeader(fos, sampleRate, channels, bitsPerSample, dataSize)
            val dos = DataOutputStream(fos)

            val masterVol = mixer.masterVolume
            for (i in 0 until totalSamplesPerChannel) {
                val leftClamped = (pcmLeft[i] * masterVol).coerceIn(-1.0f, 1.0f)
                val rightClamped = (pcmRight[i] * masterVol).coerceIn(-1.0f, 1.0f)

                val leftShort = (leftClamped * 32767.0f).toInt().toShort()
                val rightShort = (rightClamped * 32767.0f).toInt().toShort()

                dos.writeByte(leftShort.toInt() and 0xFF)
                dos.writeByte((leftShort.toInt() shr 8) and 0xFF)
                dos.writeByte(rightShort.toInt() and 0xFF)
                dos.writeByte((rightShort.toInt() shr 8) and 0xFF)
            }
            dos.flush()
        }

        outputFile
    }

    private fun writeWavHeader(fos: FileOutputStream, sampleRate: Int, channels: Int, bitsPerSample: Int, pcmDataSize: Int) {
        val totalDataLen = pcmDataSize + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte(); header[33] = 0
        header[34] = bitsPerSample.toByte(); header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (pcmDataSize and 0xff).toByte()
        header[41] = ((pcmDataSize shr 8) and 0xff).toByte()
        header[42] = ((pcmDataSize shr 16) and 0xff).toByte()
        header[43] = ((pcmDataSize shr 24) and 0xff).toByte()

        fos.write(header, 0, 44)
    }
}
