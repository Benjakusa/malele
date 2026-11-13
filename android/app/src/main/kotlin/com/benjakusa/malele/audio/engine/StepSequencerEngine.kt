package com.benjakusa.malele.audio.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.benjakusa.malele.audio.synth.DrumSynth
import com.benjakusa.malele.audio.synth.MelodicSynth
import com.benjakusa.malele.domain.model.DrumPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepSequencerEngine @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playheadStep = MutableStateFlow(0)
    val playheadStep: StateFlow<Int> = _playheadStep.asStateFlow()

    @Volatile
    private var currentPattern: DrumPattern = DrumPattern.defaultPattern()

    @Volatile
    private var isEngineRunning = false

    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null

    fun setPattern(pattern: DrumPattern) {
        currentPattern = pattern
    }

    fun start() {
        if (isEngineRunning) return
        isEngineRunning = true
        _isPlaying.value = true

        playbackThread = Thread {
            runAudioLoop()
        }.apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        isEngineRunning = false
        _isPlaying.value = false
        _playheadStep.value = 0
        audioTrack?.apply {
            try {
                stop()
                flush()
            } catch (e: Exception) {
                // Cleanup safety
            }
        }
    }

    fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
    }

    private fun runAudioLoop() {
        val sampleRate = DrumSynth.SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBufferSize, 8192))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        var stepIndex = 0

        while (isEngineRunning) {
            val pattern = currentPattern
            val bpm = pattern.bpm
            val stepDurationSec = 60.0 / (bpm * 4)
            val samplesPerStep = (sampleRate * stepDurationSec).toInt()

            val stepBuffer = ShortArray(samplesPerStep)

            // Render drum steps (trackIds-driven; supports African percussion)
            for (trackIdx in pattern.trackIds.indices) {
                if (trackIdx < pattern.steps.size && pattern.steps[trackIdx][stepIndex]) {
                    val trackVol = if (trackIdx < pattern.volumes.size) pattern.volumes[trackIdx] else 1.0f
                    val synthSamples = DrumSynth.renderTrack(pattern.trackIds[trackIdx], sampleRate, trackVol)
                    val mixLength = minOf(synthSamples.size, samplesPerStep)

                    for (i in 0 until mixLength) {
                        val mixed = stepBuffer[i] + synthSamples[i]
                        stepBuffer[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }
            }

            // Render Piano Roll notes starting on this step
            val activeNotes = pattern.notes.filter { it.startStep.toInt() == stepIndex }
            for (note in activeNotes) {
                val synthSamples = MelodicSynth.renderNote(note, stepDurationSec, sampleRate)
                val mixLength = minOf(synthSamples.size, samplesPerStep)

                for (i in 0 until mixLength) {
                    val mixed = stepBuffer[i] + synthSamples[i]
                    stepBuffer[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            _playheadStep.value = stepIndex

            var bytesWritten = 0
            while (bytesWritten < stepBuffer.size && isEngineRunning) {
                val written = track.write(stepBuffer, bytesWritten, stepBuffer.size - bytesWritten)
                if (written > 0) {
                    bytesWritten += written
                } else if (written < 0) {
                    break
                }
            }

            stepIndex = (stepIndex + 1) % DrumPattern.STEP_COUNT
        }

        try {
            track.stop()
            track.release()
        } catch (e: Exception) {
            // Cleanup safety
        }
        audioTrack = null
    }
}
