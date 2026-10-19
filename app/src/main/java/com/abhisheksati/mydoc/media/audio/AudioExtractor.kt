package com.abhisheksati.mydoc.media.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

class AudioExtractor(private val context: Context) {

    /**
     * Extracts the raw audio track from any video container into an .m4a container with zero quality loss.
     */
    suspend fun extractAudioFromVideo(
        videoUri: Uri,
        outputFileNameWithoutExt: String
    ): Result<File> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(context, videoUri, null)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || audioFormat == null) {
                return@withContext Result.failure(Exception("No audio track found in this video."))
            }

            extractor.selectTrack(audioTrackIndex)

            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (!musicDir.exists()) musicDir.mkdirs()

            val sanitizedName = outputFileNameWithoutExt.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val outputFile = File(musicDir, "${sanitizedName}.m4a")

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerAudioTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufferSize = try {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } catch (_: Exception) {
                1024 * 1024
            }
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = mapExtractorFlagsToBufferFlags(extractor.sampleFlags)
                muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                extractor.advance()
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
            try {
                extractor.release()
            } catch (_: Exception) {}
        }
    }

    companion object {
        fun mapExtractorFlagsToBufferFlags(sampleFlags: Int): Int {
            var flags = 0
            if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
            }
            if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
                flags = flags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
            }
            return flags
        }
    }
}
