package com.example.model

import android.net.Uri

data class VideoMetadata(
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L
)

enum class AspectRatioOption(val label: String, val ratio: Float?, val description: String) {
    ORIGINAL("Original", null, "Original aspect"),
    RATIO_9_16("9:16", 9f / 16f, "Stories / TikTok / Reels"),
    RATIO_1_1("1:1", 1f / 1f, "Square / Feed"),
    RATIO_16_9("16:9", 16f / 9f, "Landscape / YouTube")
}

enum class TextPosition(val label: String) {
    TOP("Top"),
    CENTER("Center"),
    BOTTOM("Bottom")
}

data class TextOverlayConfig(
    val text: String = "",
    val position: TextPosition = TextPosition.BOTTOM,
    val textColorHex: Long = 0xFFFFFFFF,
    val hasBackground: Boolean = true,
    val fontSizeSp: Float = 20f
)

enum class ExportResolution(val label: String, val width: Int, val height: Int) {
    RES_720P("720p HD", 1280, 720),
    RES_1080P("1080p Full HD", 1920, 1080)
}

sealed class ExportState {
    object Idle : ExportState()
    data class Processing(val progress: Float, val statusMessage: String) : ExportState()
    data class Success(val fileUri: Uri, val filePath: String, val fileName: String) : ExportState()
    data class Error(val errorMessage: String) : ExportState()
}

enum class EditorTab(val label: String) {
    TRIM("Trim"),
    CROP("Crop"),
    TEXT("Text"),
    SPEED("Speed")
}
