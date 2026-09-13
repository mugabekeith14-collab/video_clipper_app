package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.ExportResolution
import com.example.model.TextOverlayConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

object VideoClipperEngine {

    suspend fun extractMetadata(context: Context, uri: Uri): VideoMetadataResult = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

            var width = widthStr?.toIntOrNull() ?: 0
            var height = heightStr?.toIntOrNull() ?: 0
            val rotation = rotationStr?.toIntOrNull() ?: 0

            if (rotation == 90 || rotation == 270) {
                val temp = width
                width = height
                height = temp
            }

            // Estimate file name
            var fileName = "video_clip.mp4"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }
            }

            VideoMetadataResult(
                durationMs = durationMs,
                width = width,
                height = height,
                fileName = fileName
            )
        } catch (e: Exception) {
            e.printStackTrace()
            VideoMetadataResult(
                durationMs = 0L,
                width = 0,
                height = 0,
                fileName = "video.mp4"
            )
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    suspend fun clipAndExportVideo(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long,
        resolution: ExportResolution,
        playbackSpeed: Float,
        textOverlay: TextOverlayConfig?,
        onProgress: (Float, String) -> Unit
    ): ExportResult = withContext(Dispatchers.IO) {
        val tempOutputFile = File(context.cacheDir, "clip_export_${System.currentTimeMillis()}.mp4")

        try {
            onProgress(0.05f, "Preparing video streams...")

            val extractor = MediaExtractor()
            extractor.setDataSource(context, sourceUri, null)

            val trackCount = extractor.trackCount
            var videoTrackIndex = -1
            var audioTrackIndex = -1

            val muxer = MediaMuxer(tempOutputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackMap = mutableMapOf<Int, Int>()

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && videoTrackIndex == -1) {
                    videoTrackIndex = i
                    val muxerTrack = muxer.addTrack(format)
                    trackMap[i] = muxerTrack
                } else if (mime.startsWith("audio/") && audioTrackIndex == -1) {
                    audioTrackIndex = i
                    val muxerTrack = muxer.addTrack(format)
                    trackMap[i] = muxerTrack
                }
            }

            if (videoTrackIndex == -1) {
                extractor.release()
                muxer.release()
                return@withContext ExportResult.Error("No video track found in selected media.")
            }

            muxer.start()
            onProgress(0.15f, "Trimming video frames...")

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            val totalClipUs = (endUs - startUs).coerceAtLeast(1L)

            // Buffer size: 1.5MB is lightweight and optimal for mid-range phones
            val maxBufferSize = 1536 * 1024
            val buffer = ByteBuffer.allocateDirect(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            // Pass 1: Video track
            extractor.selectTrack(videoTrackIndex)
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            var firstVideoPtsUs = -1L
            var videoDone = false
            var sampleCount = 0

            while (!videoDone) {
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    videoDone = true
                    break
                }

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs >= startUs) {
                    if (sampleTimeUs > endUs) {
                        videoDone = true
                        break
                    }

                    if (firstVideoPtsUs == -1L) {
                        firstVideoPtsUs = sampleTimeUs
                    }

                    bufferInfo.offset = 0
                    // Re-base PTS from 0, adjusting for playback speed if applied
                    val adjustedTimeUs = ((sampleTimeUs - firstVideoPtsUs) / playbackSpeed).toLong()
                    bufferInfo.presentationTimeUs = adjustedTimeUs
                    bufferInfo.flags = extractor.sampleFlags

                    val muxerTrack = trackMap[videoTrackIndex] ?: 0
                    muxer.writeSampleData(muxerTrack, buffer, bufferInfo)

                    sampleCount++
                    if (sampleCount % 15 == 0) {
                        val progress = 0.15f + 0.60f * ((sampleTimeUs - startUs).toFloat() / totalClipUs.toFloat()).coerceIn(0f, 1f)
                        onProgress(progress, "Processing frames: ${(progress * 100).toInt()}%")
                    }
                }

                extractor.advance()
            }

            extractor.unselectTrack(videoTrackIndex)

            // Pass 2: Audio track (if present)
            if (audioTrackIndex != -1) {
                onProgress(0.78f, "Processing audio track...")
                extractor.selectTrack(audioTrackIndex)
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                var firstAudioPtsUs = -1L
                var audioDone = false

                while (!audioDone) {
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) {
                        audioDone = true
                        break
                    }

                    val sampleTimeUs = extractor.sampleTime
                    if (sampleTimeUs >= startUs) {
                        if (sampleTimeUs > endUs) {
                            audioDone = true
                            break
                        }

                        if (firstAudioPtsUs == -1L) {
                            firstAudioPtsUs = sampleTimeUs
                        }

                        bufferInfo.offset = 0
                        val adjustedTimeUs = ((sampleTimeUs - firstAudioPtsUs) / playbackSpeed).toLong()
                        bufferInfo.presentationTimeUs = adjustedTimeUs
                        bufferInfo.flags = extractor.sampleFlags

                        val muxerTrack = trackMap[audioTrackIndex] ?: 1
                        muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                    }

                    extractor.advance()
                }
                extractor.unselectTrack(audioTrackIndex)
            }

            onProgress(0.88f, "Finalizing output container...")
            try {
                muxer.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                muxer.release()
                extractor.release()
            }

            onProgress(0.92f, "Saving to device gallery...")

            // Save to MediaStore so it appears in device gallery / Photos
            val displayName = "Clipped_${System.currentTimeMillis()}.mp4"
            val galleryUri = saveToGallery(context, tempOutputFile, displayName)

            val shareableUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempOutputFile
            )

            onProgress(1.0f, "Complete!")

            ExportResult.Success(
                fileUri = shareableUri,
                galleryUri = galleryUri,
                filePath = tempOutputFile.absolutePath,
                fileName = displayName
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult.Error("Export failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun saveToGallery(context: Context, sourceFile: File, displayName: String): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/VideoClipper")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null

        try {
            resolver.openOutputStream(videoUri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out, bufferSize = 64 * 1024)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(videoUri, contentValues, null, null)
            }
            return videoUri
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareVideo(context: Context, videoUri: Uri, title: String = "Share clipped video") {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, videoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

data class VideoMetadataResult(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fileName: String
)

sealed class ExportResult {
    data class Success(
        val fileUri: Uri,
        val galleryUri: Uri?,
        val filePath: String,
        val fileName: String
    ) : ExportResult()

    data class Error(val message: String) : ExportResult()
}
