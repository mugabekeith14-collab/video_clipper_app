package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.model.AspectRatioOption
import com.example.model.TextOverlayConfig
import com.example.model.TextPosition
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    videoUri: Uri,
    trimStartMs: Long,
    trimEndMs: Long,
    playbackSpeed: Float,
    aspectRatioOption: AspectRatioOption,
    textOverlay: TextOverlayConfig,
    isPlaying: Boolean,
    onPositionChanged: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPlayPauseIndicator by remember { mutableStateOf(false) }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
            seekTo(trimStartMs)
        }
    }

    // Update playback parameters when speed changes
    LaunchedEffect(playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    // Play/Pause sync
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            if (exoPlayer.currentPosition < trimStartMs || exoPlayer.currentPosition >= trimEndMs) {
                exoPlayer.seekTo(trimStartMs)
            }
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Position tracker & loop within trim bounds
    LaunchedEffect(trimStartMs, trimEndMs, isPlaying) {
        while (true) {
            val currentPos = exoPlayer.currentPosition
            onPositionChanged(currentPos)

            if (isPlaying) {
                if (currentPos >= trimEndMs) {
                    exoPlayer.seekTo(trimStartMs)
                } else if (currentPos < trimStartMs) {
                    exoPlayer.seekTo(trimStartMs)
                }
            }
            delay(50L)
        }
    }

    DisposableEffect(videoUri) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onTogglePlay()
                showPlayPauseIndicator = true
            },
        contentAlignment = Alignment.Center
    ) {
        // Container that respects the selected aspect ratio crop
        val cropModifier = when (aspectRatioOption) {
            AspectRatioOption.ORIGINAL -> Modifier.fillMaxSize()
            AspectRatioOption.RATIO_9_16 -> Modifier.aspectRatio(9f / 16f).fillMaxSize()
            AspectRatioOption.RATIO_1_1 -> Modifier.aspectRatio(1f).fillMaxSize()
            AspectRatioOption.RATIO_16_9 -> Modifier.aspectRatio(16f / 9f).fillMaxWidth()
        }

        Box(
            modifier = Modifier
                .wrapContentSize()
                .then(cropModifier)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F111A)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Text Overlay layer
            if (textOverlay.text.isNotBlank()) {
                val overlayAlignment = when (textOverlay.position) {
                    TextPosition.TOP -> Alignment.TopCenter
                    TextPosition.CENTER -> Alignment.Center
                    TextPosition.BOTTOM -> Alignment.BottomCenter
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = overlayAlignment
                ) {
                    if (textOverlay.hasBackground) {
                        Surface(
                            color = Color(0xB3000000),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = textOverlay.text,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = Color(textOverlay.textColorHex),
                                fontSize = textOverlay.fontSizeSp.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = textOverlay.text,
                            color = Color(textOverlay.textColorHex),
                            fontSize = textOverlay.fontSizeSp.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Aspect ratio indicator badge on top-left of video
            if (aspectRatioOption != AspectRatioOption.ORIGINAL) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    color = Color(0x99000000),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Crop: ${aspectRatioOption.label}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFFA5B4FC),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Brief Play/Pause pop indicator on tap
        LaunchedEffect(showPlayPauseIndicator) {
            if (showPlayPauseIndicator) {
                delay(600)
                showPlayPauseIndicator = false
            }
        }

        AnimatedVisibility(
            visible = showPlayPauseIndicator,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
