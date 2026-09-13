package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun TimelineTrimmer(
    durationMs: Long,
    currentPositionMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    isPlaying: Boolean,
    onTrimChange: (startMs: Long, endMs: Long) -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onTogglePlay: () -> Unit,
    onReplayClip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1000L)
    val clipDuration = (trimEndMs - trimStartMs).coerceAtLeast(0L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141724))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Time summary header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Clip Duration",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9E9E9E)
                )
                Text(
                    text = formatTimeMs(clipDuration),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color(0xFF818CF8)
                )
            }

            // Quick Playback Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onReplayClip,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("replay_clip_button"),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFF1E2337)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay Clip",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                FilledIconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("play_pause_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF6366F1),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Current / Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9E9E9E)
                )
                Text(
                    text = "${formatTimeMs(currentPositionMs)} / ${formatTimeMs(safeDuration)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White
                )
            }
        }

        // Scrubbable Timeline Track & Draggable Handles
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D0F18))
        ) {
            val density = LocalDensity.current
            val totalWidthPx = constraints.maxWidth.toFloat()
            val handleWidthPx = 70f
            val usableTrackPx = (totalWidthPx - handleWidthPx * 2).coerceAtLeast(10f)

            val startNorm = (trimStartMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 0.95f)
            val endNorm = (trimEndMs.toFloat() / safeDuration.toFloat()).coerceIn(0.05f, 1f)
            val playheadNorm = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

            val startHandleOffset = startNorm * usableTrackPx
            val endHandleOffset = handleWidthPx + (endNorm * usableTrackPx)
            val playheadOffset = (playheadNorm * totalWidthPx).coerceIn(0f, totalWidthPx - 4f)

            // Timeline Background Pattern (simulated frames & ticks)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(20) { index ->
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(if (index % 5 == 0) 24.dp else 12.dp)
                            .background(Color(0xFF262A40))
                    )
                }
            }

            // Darkened Dim overlay for Left unselected area
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, 0) }
                    .width(with(density) { startHandleOffset.toDp() })
                    .height(68.dp)
                    .background(Color(0x99000000))
            )

            // Active Highlighted Clip Window
            val activeWindowWidth = (endHandleOffset - startHandleOffset).coerceAtLeast(20f)
            Box(
                modifier = Modifier
                    .offset { IntOffset(startHandleOffset.roundToInt(), 0) }
                    .width(with(density) { activeWindowWidth.toDp() })
                    .height(68.dp)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF818CF8), Color(0xFF6366F1))
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(Color(0x22818CF8))
                    // Allow tapping anywhere on track to seek
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val tapRatio = (change.position.x + startHandleOffset) / totalWidthPx
                            val targetMs = (tapRatio * safeDuration).toLong().coerceIn(trimStartMs, trimEndMs)
                            onSeek(targetMs)
                        }
                    }
            )

            // Draggable Start Handle (Left)
            Box(
                modifier = Modifier
                    .offset { IntOffset(startHandleOffset.roundToInt(), 0) }
                    .width(28.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .background(Color(0xFF6366F1))
                    .testTag("trim_start_handle")
                    .pointerInput(safeDuration, trimEndMs) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val currentPx = (trimStartMs.toFloat() / safeDuration.toFloat()) * usableTrackPx
                            val newPx = (currentPx + dragAmount.x).coerceIn(0f, usableTrackPx)
                            val newRatio = newPx / usableTrackPx
                            val newStartMs = (newRatio * safeDuration).toLong().coerceIn(0L, trimEndMs - 500L)
                            onTrimChange(newStartMs, trimEndMs)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Grip icon lines
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(modifier = Modifier.width(2.dp).height(24.dp).background(Color.White))
                    Box(modifier = Modifier.width(2.dp).height(24.dp).background(Color(0xCCFFFFFF)) )
                }
            }

            // Draggable End Handle (Right)
            Box(
                modifier = Modifier
                    .offset { IntOffset((endHandleOffset - 28f).roundToInt(), 0) }
                    .width(28.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .background(Color(0xFF6366F1))
                    .testTag("trim_end_handle")
                    .pointerInput(safeDuration, trimStartMs) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val currentPx = (trimEndMs.toFloat() / safeDuration.toFloat()) * usableTrackPx
                            val newPx = (currentPx + dragAmount.x).coerceIn(0f, usableTrackPx)
                            val newRatio = newPx / usableTrackPx
                            val newEndMs = (newRatio * safeDuration).toLong().coerceIn(trimStartMs + 500L, safeDuration)
                            onTrimChange(trimStartMs, newEndMs)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Grip icon lines
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(modifier = Modifier.width(2.dp).height(24.dp).background(Color(0xCCFFFFFF)) )
                    Box(modifier = Modifier.width(2.dp).height(24.dp).background(Color.White))
                }
            }

            // Darkened Dim overlay for Right unselected area
            Box(
                modifier = Modifier
                    .offset { IntOffset(endHandleOffset.roundToInt(), 0) }
                    .width(with(density) { (totalWidthPx - endHandleOffset).coerceAtLeast(0f).toDp() })
                    .height(68.dp)
                    .background(Color(0x99000000))
            )

            // Live Playhead Needle (red/white vertical indicator)
            Box(
                modifier = Modifier
                    .offset { IntOffset(playheadOffset.roundToInt(), 0) }
                    .width(3.dp)
                    .height(68.dp)
                    .background(Color(0xFFFF5252))
            )
            // Playhead indicator dot on top
            Box(
                modifier = Modifier
                    .offset { IntOffset((playheadOffset - 5f).roundToInt(), 2) }
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5252))
            )
        }

        // Start & End Timestamps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = Color(0xFF1E2337),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Start: ${formatTimeMs(trimStartMs)}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFFA5B4FC)
                )
            }

            Surface(
                color = Color(0xFF1E2337),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "End: ${formatTimeMs(trimEndMs)}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFFA5B4FC)
                )
            }
        }
    }
}

fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (ms % 1000) / 100
    return String.format("%02d:%02d.%d", minutes, seconds, tenths)
}
