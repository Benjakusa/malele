package com.benjakusa.malele.audio.export

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M4aExporter @Inject constructor() {

    suspend fun convertWavToM4a(wavFile: File, outputFile: File): File = withContext(Dispatchers.IO) {
        val sampleRate = 44100
        val channelCount = 2
        val bitRate = 128000

        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        val inputStream = FileInputStream(wavFile)
        inputStream.skip(44) // Skip WAV header

        val inputBuffer = ByteArray(8192)
        var isInputStreamEnd = false

        while (true) {
            if (!isInputStreamEnd) {
                val inputIndex = codec.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val bytesRead = inputStream.read(inputBuffer)
                    if (bytesRead <= 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isInputStreamEnd = true
                    } else {
                        val buffer: ByteBuffer = codec.getInputBuffer(inputIndex)!!
                        buffer.clear()
                        buffer.put(inputBuffer, 0, bytesRead)
                        codec.queueInputBuffer(inputIndex, 0, bytesRead, System.nanoTime() / 1000, 0)
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                trackIndex = muxer.addTrack(codec.outputFormat)
                muxer.start()
                muxerStarted = true
            } else if (outputIndex >= 0) {
                val encodedData: ByteBuffer = codec.getOutputBuffer(outputIndex)!!
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0 && muxerStarted) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                }

                codec.releaseOutputBuffer(outputIndex, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break
                }
            }
        }

        inputStream.close()
        codec.stop()
        codec.release()

        if (muxerStarted) {
            muxer.stop()
            muxer.release()
        }

        outputFile
    }
}
