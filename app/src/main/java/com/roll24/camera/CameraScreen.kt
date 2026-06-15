package com.roll24.camera

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roll24.Roll24ViewModel
import com.roll24.film.FilmProfile
import com.roll24.film.FilmProfileRepository
import com.roll24.haptics.rememberRoll24Haptics
import com.roll24.image.YuvConverter
import com.roll24.ui.components.CaptureButton
import com.roll24.ui.components.FilmSelector
import com.roll24.ui.theme.AccentGold
import com.roll24.ui.theme.Black
import com.roll24.ui.theme.White

@Composable
fun CameraScreen(
    viewModel: Roll24ViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = rememberRoll24Haptics()
    
    var cameraController by remember { mutableStateOf<Camera2Controller?>(null) }
    var surfaceTexture by remember { mutableStateOf<SurfaceTexture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val showReview by viewModel.showReview.collectAsState()
    
    // Observa mudanças de processamento para disparar haptics
    LaunchedEffect(isProcessing, showReview) {
        when {
            isProcessing -> haptics.developingStart()
            showReview && !isProcessing -> haptics.developingComplete()
        }
    }

    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val controller = Camera2Controller(context)
                    cameraController = controller
                    controller.startBackgroundThread()
                    
                    surfaceTexture?.let { texture ->
                        controller.openCamera(texture)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    cameraController?.closeCamera()
                    cameraController?.stopBackgroundThread()
                    cameraController = null
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            surfaceTexture = surface
                            cameraController?.setPreviewSurface(surface)
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            // Handle size changes if needed
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            surfaceTexture = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                            // Frame update callback if needed
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // UI Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            // Film selector at bottom
            FilmSelector(
                profiles = FilmProfileRepository.profiles,
                selectedProfile = selectedProfile,
                onProfileSelected = { profile ->
                    viewModel.selectProfile(profile)
                    // Update capture settings based on profile
                    cameraController?.let { controller ->
                        val captureEngine = CaptureEngine(controller.characteristics?.let { 
                            CameraCapabilities(
                                cameraId = controller.cameraId,
                                hardwareLevel = 0,
                                hardwareLevelName = "",
                                supportsRaw = false,
                                supportsYuv = true,
                                availableFormats = emptyList(),
                                formatSizes = emptyMap(),
                                supportsManualSensor = false,
                                supportsManualFocus = false,
                                supportsManualWhiteBalance = false,
                                isoRange = null,
                                exposureTimeRange = null,
                                focalLengths = emptyList(),
                                sensorSize = null,
                                activeArraySize = null,
                                noiseReductionModes = emptyList(),
                                edgeModes = emptyList(),
                                tonemapModes = emptyList(),
                                maxDigitalZoom = 1f,
                                supportsAeLock = false,
                                supportsAwbLock = false
                            )
                        })
                        // Note: In a full implementation, we'd update the preview request
                        // with the new profile settings here
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            )
            
            // Capture button
            CaptureButton(
                onClick = {
                    if (!isCapturing && !isProcessing) {
                        isCapturing = true
                        cameraController?.captureImage { image ->
                            // Convert YUV to Bitmap
                            val bitmap = YuvConverter.yuvToBitmap(image)
                            image.close()
                            
                            if (bitmap != null) {
                                // Process with FilmDevelopmentEngine
                                viewModel.processImage(bitmap)
                            }
                            
                            isCapturing = false
                        }
                    }
                },
                enabled = !isCapturing && !isProcessing,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Processing overlay
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentGold)
            }
        }
    }
}
