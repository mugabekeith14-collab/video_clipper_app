package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EditorBottomControls
import com.example.ui.components.EmptyImportState
import com.example.ui.components.ExportDialog
import com.example.ui.components.TimelineTrimmer
import com.example.ui.components.VideoPlayerView
import com.example.viewmodel.VideoEditorViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    viewModel: VideoEditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var cameraVideoUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadVideoUri(context, uri)
        }
    }

    // Camera Video Recording launcher
    val cameraRecordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success && cameraVideoUri != null) {
            viewModel.loadVideoUri(context, cameraVideoUri!!)
        }
    }

    fun launchCameraRecording() {
        try {
            val videoFile = File(context.cacheDir, "camera_rec_${System.currentTimeMillis()}.mp4")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )
            cameraVideoUri = uri
            cameraRecordLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera Permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCameraRecording()
        } else {
            Toast.makeText(context, "Camera permission needed to record video", Toast.LENGTH_SHORT).show()
        }
    }

    val requestCameraAndRecord: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCameraRecording()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D14)),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Video Clipper",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        if (uiState.video != null) {
                            Text(
                                text = uiState.video!!.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA5B4FC),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.video != null) {
                        // Quick Import Icon Button
                        IconButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier.testTag("topbar_pick_gallery")
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Choose another video",
                                tint = Color(0xFFC7D2FE)
                            )
                        }

                        IconButton(
                            onClick = requestCameraAndRecord,
                            modifier = Modifier.testTag("topbar_record_video")
                        ) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = "Record new video",
                                tint = Color(0xFFC7D2FE)
                            )
                        }

                        // Export Button
                        Button(
                            onClick = { viewModel.openExportDialog() },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("topbar_export_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Export",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF101321)
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0B0D14))
        ) {
            val video = uiState.video

            if (video == null) {
                EmptyImportState(
                    isLoading = uiState.isLoadingVideo,
                    onPickGallery = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onRecordCamera = requestCameraAndRecord,
                    onLoadSample = { viewModel.loadSampleVideo(context) }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Video Player Preview Area (Takes majority space)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        VideoPlayerView(
                            videoUri = video.uri,
                            trimStartMs = uiState.trimStartMs,
                            trimEndMs = uiState.trimEndMs,
                            playbackSpeed = uiState.playbackSpeed,
                            aspectRatioOption = uiState.aspectRatio,
                            textOverlay = uiState.textOverlay,
                            isPlaying = uiState.isPlaying,
                            onPositionChanged = { pos -> viewModel.updatePlaybackPosition(pos) },
                            onTogglePlay = { viewModel.setPlaying(!uiState.isPlaying) }
                        )
                    }

                    // Scrubbable Timeline & Draggable Trimmer
                    TimelineTrimmer(
                        durationMs = video.durationMs,
                        currentPositionMs = uiState.currentPositionMs,
                        trimStartMs = uiState.trimStartMs,
                        trimEndMs = uiState.trimEndMs,
                        isPlaying = uiState.isPlaying,
                        onTrimChange = { start, end -> viewModel.updateTrimRange(start, end) },
                        onSeek = { pos -> viewModel.updatePlaybackPosition(pos) },
                        onTogglePlay = { viewModel.setPlaying(!uiState.isPlaying) },
                        onReplayClip = {
                            viewModel.updatePlaybackPosition(uiState.trimStartMs)
                            viewModel.setPlaying(true)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    // Bottom Editing Controls (Trim, Crop, Text, Speed tabs)
                    EditorBottomControls(
                        activeTab = uiState.activeTab,
                        onTabSelected = { tab -> viewModel.setActiveTab(tab) },
                        durationMs = video.durationMs,
                        trimStartMs = uiState.trimStartMs,
                        trimEndMs = uiState.trimEndMs,
                        onTrimChange = { start, end -> viewModel.updateTrimRange(start, end) },
                        aspectRatio = uiState.aspectRatio,
                        onAspectRatioChange = { ratio -> viewModel.setAspectRatio(ratio) },
                        textOverlay = uiState.textOverlay,
                        onTextOverlayChange = { overlay -> viewModel.setTextOverlay(overlay) },
                        playbackSpeed = uiState.playbackSpeed,
                        onPlaybackSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) }
                    )
                }
            }

            // Export Dialog
            ExportDialog(
                isOpen = uiState.isExportDialogOpen,
                exportState = uiState.exportState,
                selectedResolution = uiState.exportResolution,
                clipDurationMs = uiState.trimEndMs - uiState.trimStartMs,
                aspectRatio = uiState.aspectRatio,
                playbackSpeed = uiState.playbackSpeed,
                hasTextOverlay = uiState.textOverlay.text.isNotBlank(),
                onResolutionChange = { res -> viewModel.setExportResolution(res) },
                onStartExport = { viewModel.executeExport(context) },
                onShare = { viewModel.shareExportedVideo(context) },
                onDismiss = { viewModel.dismissExportDialog() }
            )
        }
    }
}
