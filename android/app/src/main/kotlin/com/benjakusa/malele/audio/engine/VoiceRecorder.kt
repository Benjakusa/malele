package com.benjakusa.malele.audio.engine

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class VoiceRecorder @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecordingLoop = false

    @SuppressLint("MissingPermission")
    fun startRecording(outputFile: File) {
        if (isRecordingLoop) return

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            maxOf(minBufferSize, 8192),
        )

        audioRecord = recorder
        isRecordingLoop = true
        _isRecording.value = true

        scope.launch {
            recorder.startRecording()
            val buffer = ShortArray(1024)

            val pcmData = mutableListOf<Short>()

            while (isRecordingLoop) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    var maxAmp = 0
                    for (i in 0 until read) {
                        val sample = buffer[i]
                        pcmData.add(sample)
                        val absSample = abs(sample.toInt())
                        if (absSample > maxAmp) maxAmp = absSample
                    }
                    _amplitude.value = (maxAmp / 32768f).coerceIn(0f, 1f)
                }
            }

            // Save to WAV file
            writeWavFile(outputFile, pcmData.toShortArray(), sampleRate)

            try {
                recorder.stop()
                recorder.release()
            } catch (e: Exception) {
                // Cleanup safety
            }
            audioRecord = null
            _isRecording.value = false
            _amplitude.value = 0f
        }
    }

    fun stopRecording() {
        isRecordingLoop = false
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val channels = 1
        val bitsPerSample = 16
        val pcmDataSize = pcmData.size * 2
        val totalDataLen = pcmDataSize + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        FileOutputStream(file).use { fos ->
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
            val dos = DataOutputStream(fos)
            for (sample in pcmData) {
                dos.writeByte(sample.toInt() and 0xFF)
                dos.writeByte((sample.toInt() shr 8) and 0xFF)
            }
            dos.flush()
        }
    }
}
