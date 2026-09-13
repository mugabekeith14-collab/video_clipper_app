package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatioOption
import com.example.model.EditorTab
import com.example.model.TextOverlayConfig
import com.example.model.TextPosition

@Composable
fun EditorBottomControls(
    activeTab: EditorTab,
    onTabSelected: (EditorTab) -> Unit,
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    onTrimChange: (Long, Long) -> Unit,
    aspectRatio: AspectRatioOption,
    onAspectRatioChange: (AspectRatioOption) -> Unit,
    textOverlay: TextOverlayConfig,
    onTextOverlayChange: (TextOverlayConfig) -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0xFF141724))
            .padding(bottom = 16.dp)
    ) {
        // Mode Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            EditorTabItem(
                title = "Trim",
                icon = Icons.Default.ContentCut,
                isSelected = activeTab == EditorTab.TRIM,
                onClick = { onTabSelected(EditorTab.TRIM) }
            )
            EditorTabItem(
                title = "Crop",
                icon = Icons.Default.AspectRatio,
                isSelected = activeTab == EditorTab.CROP,
                onClick = { onTabSelected(EditorTab.CROP) }
            )
            EditorTabItem(
                title = "Text",
                icon = Icons.Default.TextFields,
                isSelected = activeTab == EditorTab.TEXT,
                onClick = { onTabSelected(EditorTab.TEXT) }
            )
            EditorTabItem(
                title = "Speed",
                icon = Icons.Default.Speed,
                isSelected = activeTab == EditorTab.SPEED,
                onClick = { onTabSelected(EditorTab.SPEED) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            when (activeTab) {
                EditorTab.TRIM -> {
                    TrimControlsPanel(
                        durationMs = durationMs,
                        trimStartMs = trimStartMs,
                        trimEndMs = trimEndMs,
                        onTrimChange = onTrimChange
                    )
                }
                EditorTab.CROP -> {
                    CropControlsPanel(
                        selectedOption = aspectRatio,
                        onSelect = onAspectRatioChange
                    )
                }
                EditorTab.TEXT -> {
                    TextOverlayControlsPanel(
                        config = textOverlay,
                        onChange = onTextOverlayChange
                    )
                }
                EditorTab.SPEED -> {
                    SpeedControlsPanel(
                        currentSpeed = playbackSpeed,
                        onSpeedSelected = onPlaybackSpeedChange
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF6366F1) else Color(0xFF1E2337),
        modifier = Modifier.testTag("tab_${title.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else Color(0xFF9E9E9E),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) Color.White else Color(0xFF9E9E9E)
            )
        }
    }
}

@Composable
private fun TrimControlsPanel(
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    onTrimChange: (Long, Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Quick Trim Presets & Nudge",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFA5B4FC)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { onTrimChange(0L, durationMs) },
                label = { Text("Full Video") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFF1E2337),
                    labelColor = Color.White
                )
            )

            AssistChip(
                onClick = {
                    val end = minOf(5000L, durationMs)
                    onTrimChange(0L, end)
                },
                label = { Text("First 5s") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFF1E2337),
                    labelColor = Color.White
                )
            )

            AssistChip(
                onClick = {
                    val end = minOf(15000L, durationMs)
                    onTrimChange(0L, end)
                },
                label = { Text("First 15s (Story)") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFF1E2337),
                    labelColor = Color.White
                )
            )

            AssistChip(
                onClick = {
                    val start = maxOf(0L, trimStartMs - 500L)
                    onTrimChange(start, trimEndMs)
                },
                label = { Text("Start -0.5s") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFF1E2337),
                    labelColor = Color.White
                )
            )

            AssistChip(
                onClick = {
                    val start = minOf(trimEndMs - 500L, trimStartMs + 500L)
                    onTrimChange(start, trimEndMs)
                },
                label = { Text("Start +0.5s") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFF1E2337),
                    labelColor = Color.White
                )
            )

            AssistChip(
                onClick = {
                    val end = minOf(durationMs, trimEndMs + 500L)
                    onTrimChange(trimStartMs, end)
                },
                label = { Text("End +0.5s") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFF1E2337),
                    labelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun CropControlsPanel(
    selectedOption: AspectRatioOption,
    onSelect: (AspectRatioOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Target Aspect Ratio Crop",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFA5B4FC)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AspectRatioOption.values().forEach { option ->
                val isSelected = option == selectedOption
                Surface(
                    onClick = { onSelect(option) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("crop_${option.name.lowercase()}"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF6366F1) else Color(0xFF1E2337),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E354F))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else Color(0xFFE2E8F0)
                        )
                        Text(
                            text = when (option) {
                                AspectRatioOption.ORIGINAL -> "Fit"
                                AspectRatioOption.RATIO_9_16 -> "Reels/TikTok"
                                AspectRatioOption.RATIO_1_1 -> "Square"
                                AspectRatioOption.RATIO_16_9 -> "YouTube"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isSelected) Color(0xFFE0E7FF) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextOverlayControlsPanel(
    config: TextOverlayConfig,
    onChange: (TextOverlayConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Text Input
        OutlinedTextField(
            value = config.text,
            onValueChange = { onChange(config.copy(text = it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("text_overlay_input"),
            placeholder = { Text("Add caption or title overlay...", color = Color(0xFF64748B)) },
            trailingIcon = {
                if (config.text.isNotEmpty()) {
                    IconButton(onClick = { onChange(config.copy(text = "")) }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear text", tint = Color.White)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E2337),
                unfocusedContainerColor = Color(0xFF1E2337),
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color(0xFF2E354F),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Options: Position & Color
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position Selector
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextPosition.values().forEach { pos ->
                    val isSelected = config.position == pos
                    FilterChip(
                        selected = isSelected,
                        onClick = { onChange(config.copy(position = pos)) },
                        label = { Text(pos.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E2337),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            // Background Pill Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Dark pill",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
                Switch(
                    checked = config.hasBackground,
                    onCheckedChange = { onChange(config.copy(hasBackground = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF6366F1),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF1E2337)
                    )
                )
            }
        }

        // Color Palette choices
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Color:",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            val colors = listOf(
                0xFFFFFFFF to "White",
                0xFFFACC15 to "Yellow",
                0xFFFB7185 to "Coral",
                0xFF38BDF8 to "Cyan",
                0xFF34D399 to "Green",
                0xFF18181B to "Black"
            )
            colors.forEach { (colorHex, _) ->
                val isSelected = config.textColorHex == colorHex
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(colorHex))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF6366F1) else Color(0x66FFFFFF),
                            shape = CircleShape
                        )
                        .clickable { onChange(config.copy(textColorHex = colorHex)) }
                )
            }
        }
    }
}

@Composable
private fun SpeedControlsPanel(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Playback Speed Multiplier",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFA5B4FC)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            speeds.forEach { speed ->
                val isSelected = (currentSpeed == speed)
                Surface(
                    onClick = { onSpeedSelected(speed) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("speed_${speed}x"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF6366F1) else Color(0xFF1E2337),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E354F))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${speed}x",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else Color.White
                        )
                        Text(
                            text = when (speed) {
                                0.5f -> "Slow"
                                1.0f -> "Normal"
                                2.0f -> "Fast"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (isSelected) Color(0xFFE0E7FF) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
