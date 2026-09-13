package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.ExportResult
import com.example.engine.SampleVideoGenerator
import com.example.engine.VideoClipperEngine
import com.example.model.AspectRatioOption
import com.example.model.EditorTab
import com.example.model.ExportResolution
import com.example.model.ExportState
import com.example.model.TextOverlayConfig
import com.example.model.VideoMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VideoEditorUiState(
    val video: VideoMetadata? = null,
    val isLoadingVideo: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val aspectRatio: AspectRatioOption = AspectRatioOption.ORIGINAL,
    val textOverlay: TextOverlayConfig = TextOverlayConfig(),
    val playbackSpeed: Float = 1.0f,
    val activeTab: EditorTab = EditorTab.TRIM,
    val isExportDialogOpen: Boolean = false,
    val exportResolution: ExportResolution = ExportResolution.RES_720P,
    val exportState: ExportState = ExportState.Idle,
    val errorMessage: String? = null
)

class VideoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VideoEditorUiState())
    val uiState: StateFlow<VideoEditorUiState> = _uiState.asStateFlow()

    init {
        loadSampleVideo(application)
    }

    fun loadVideoUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVideo = true, errorMessage = null) }
            try {
                val metadata = VideoClipperEngine.extractMetadata(context, uri)
                val duration = if (metadata.durationMs > 0) metadata.durationMs else 5000L
                val videoData = VideoMetadata(
                    uri = uri,
                    name = metadata.fileName,
                    durationMs = duration,
                    width = metadata.width,
                    height = metadata.height
                )

                _uiState.update {
                    it.copy(
                        video = videoData,
                        isLoadingVideo = false,
                        currentPositionMs = 0L,
                        trimStartMs = 0L,
                        trimEndMs = duration,
                        isPlaying = true,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoadingVideo = false,
                        errorMessage = "Could not load video: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun loadSampleVideo(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVideo = true, errorMessage = null) }
            try {
                val sampleUri = SampleVideoGenerator.getOrCreateSampleVideo(context)
                loadVideoUri(context, sampleUri)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoadingVideo = false,
                        errorMessage = "Failed to create sample video: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun updateTrimRange(startMs: Long, endMs: Long) {
        val currentDuration = _uiState.value.video?.durationMs ?: return
        val clampedStart = startMs.coerceIn(0L, currentDuration - 500L)
        val clampedEnd = endMs.coerceIn(clampedStart + 500L, currentDuration)

        _uiState.update {
            it.copy(
                trimStartMs = clampedStart,
                trimEndMs = clampedEnd
            )
        }
    }

    fun updatePlaybackPosition(posMs: Long) {
        _uiState.update { it.copy(currentPositionMs = posMs) }
    }

    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    fun setAspectRatio(ratio: AspectRatioOption) {
        _uiState.update { it.copy(aspectRatio = ratio) }
    }

    fun setTextOverlay(config: TextOverlayConfig) {
        _uiState.update { it.copy(textOverlay = config) }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setActiveTab(tab: EditorTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun openExportDialog() {
        _uiState.update {
            it.copy(
                isExportDialogOpen = true,
                exportState = ExportState.Idle
            )
        }
    }

    fun dismissExportDialog() {
        _uiState.update {
            it.copy(isExportDialogOpen = false)
        }
    }

    fun setExportResolution(resolution: ExportResolution) {
        _uiState.update { it.copy(exportResolution = resolution) }
    }

    fun executeExport(context: Context) {
        val state = _uiState.value
        val video = state.video ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    exportState = ExportState.Processing(0.0f, "Starting video processing...")
                )
            }

            val result = VideoClipperEngine.clipAndExportVideo(
                context = context,
                sourceUri = video.uri,
                startMs = state.trimStartMs,
                endMs = state.trimEndMs,
                resolution = state.exportResolution,
                playbackSpeed = state.playbackSpeed,
                textOverlay = if (state.textOverlay.text.isNotBlank()) state.textOverlay else null,
                onProgress = { progress, status ->
                    _uiState.update {
                        it.copy(exportState = ExportState.Processing(progress, status))
                    }
                }
            )

            when (result) {
                is ExportResult.Success -> {
                    _uiState.update {
                        it.copy(
                            exportState = ExportState.Success(
                                fileUri = result.fileUri,
                                filePath = result.filePath,
                                fileName = result.fileName
                            )
                        )
                    }
                }
                is ExportResult.Error -> {
                    _uiState.update {
                        it.copy(exportState = ExportState.Error(result.message))
                    }
                }
            }
        }
    }

    fun shareExportedVideo(context: Context) {
        val exportState = _uiState.value.exportState
        if (exportState is ExportState.Success) {
            VideoClipperEngine.shareVideo(context, exportState.fileUri)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
