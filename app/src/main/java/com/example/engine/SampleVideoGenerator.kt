package com.example.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.view.Surface
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SampleVideoGenerator {

    suspend fun getOrCreateSampleVideo(context: Context): Uri = withContext(Dispatchers.IO) {
        val sampleFile = File(context.cacheDir, "sample_clip_demo.mp4")
        if (sampleFile.exists() && sampleFile.length() > 1024) {
            return@withContext FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                sampleFile
            )
        }

        generateDemoVideo(context, sampleFile)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            sampleFile
        )
    }

    private fun generateDemoVideo(context: Context, outputFile: File) {
        val width = 720
        val height = 1280
        val fps = 30
        val durationSeconds = 8
        val totalFrames = fps * durationSeconds
        val bitRate = 2_500_000

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface: Surface = encoder.createInputSurface()
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(200, 220, 255)
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        try {
            for (frame in 0 until totalFrames) {
                // Drain encoder outputs
                drainEncoder(encoder, bufferInfo, muxer, { idx -> trackIndex = idx }, { muxerStarted = true }, muxerStarted)

                // Render frame to input surface
                val canvas: Canvas = inputSurface.lockHardwareCanvas()
                try {
                    val progress = frame.toFloat() / totalFrames.toFloat()
                    val currentTimeSec = frame.toFloat() / fps.toFloat()

                    // Background color gradient transition
                    val red = (30 + 50 * Math.sin(progress * Math.PI * 2)).toInt().coerceIn(0, 255)
                    val green = (40 + 70 * Math.cos(progress * Math.PI * 2)).toInt().coerceIn(0, 255)
                    val blue = (110 + 100 * Math.sin(progress * Math.PI * 3)).toInt().coerceIn(0, 255)
                    canvas.drawColor(Color.rgb(red, green, blue))

                    // Draw animated orbit circle
                    val centerX = width / 2f
                    val centerY = height / 2f
                    val angle = frame * 0.1
                    val orbitX = centerX + (Math.cos(angle) * 160).toFloat()
                    val orbitY = centerY + (Math.sin(angle) * 160).toFloat()

                    circlePaint.color = Color.argb(160, 255, 215, 64)
                    canvas.drawCircle(orbitX, orbitY, 40f, circlePaint)

                    // Draw card container
                    val cardRect = RectF(60f, centerY - 220f, width - 60f, centerY + 220f)
                    val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(180, 15, 18, 36)
                    }
                    canvas.drawRoundRect(cardRect, 32f, 32f, cardBgPaint)

                    // Text labels
                    canvas.drawText("CLIPPER DEMO VIDEO", centerX, centerY - 100f, textPaint)
                    canvas.drawText(
                        String.format("Time: %02d:%02d.%d / 00:%02d",
                            (currentTimeSec / 60).toInt(),
                            (currentTimeSec % 60).toInt(),
                            ((currentTimeSec * 10) % 10).toInt(),
                            durationSeconds
                        ),
                        centerX,
                        centerY,
                        textPaint
                    )
                    canvas.drawText("Frame $frame of $totalFrames (${fps}fps)", centerX, centerY + 80f, subPaint)
                    canvas.drawText("Drag handles below to trim", centerX, centerY + 140f, subPaint)

                    // Progress bar at bottom
                    val barHeight = 16f
                    val barWidth = width - 120f
                    val barX = 60f
                    val barY = height - 100f
                    val bgBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(100, 255, 255, 255)
                    }
                    canvas.drawRoundRect(barX, barY, barX + barWidth, barY + barHeight, 8f, 8f, bgBarPaint)

                    val fillBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(99, 102, 241)
                    }
                    canvas.drawRoundRect(barX, barY, barX + (barWidth * progress), barY + barHeight, 8f, 8f, fillBarPaint)
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                // Short sleep to balance frame rate delivery to surface
                Thread.sleep(1000L / (fps * 2))
            }

            // Signal end of stream
            encoder.signalEndOfInputStream()

            // Drain remaining frames
            var eos = false
            var attempts = 0
            while (!eos && attempts < 100) {
                attempts++
                val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eos = true
                    }
                    if (bufferInfo.size > 0 && muxerStarted && trackIndex >= 0) {
                        val encodedData = encoder.getOutputBuffer(outIndex)
                        if (encodedData != null) {
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        trackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                encoder.stop()
                encoder.release()
                inputSurface.release()
                if (muxerStarted) {
                    muxer.stop()
                }
                muxer.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        bufferInfo: MediaCodec.BufferInfo,
        muxer: MediaMuxer,
        onTrackAdded: (Int) -> Unit,
        onMuxerStarted: () -> Unit,
        isMuxerStarted: Boolean
    ) {
        var started = isMuxerStarted
        while (true) {
            val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 2000)
            if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!started) {
                    val trackIndex = muxer.addTrack(encoder.outputFormat)
                    onTrackAdded(trackIndex)
                    muxer.start()
                    onMuxerStarted()
                    started = true
                }
            } else if (outIndex >= 0) {
                if (bufferInfo.size > 0 && started) {
                    val buffer = encoder.getOutputBuffer(outIndex)
                    if (buffer != null) {
                        muxer.writeSampleData(0, buffer, bufferInfo)
                    }
                }
                encoder.releaseOutputBuffer(outIndex, false)
            }
        }
    }
}
