package com.roll24.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class Camera2Controller(private val context: Context) {
    
    companion object {
        private const val TAG = "Camera2Controller"
        private const val CAMERA_OPEN_TIMEOUT_MS = 2500L
    }

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    
    private val cameraOpenCloseLock = Semaphore(1)
    
    var previewSize: Size = Size(1920, 1080)
        private set
    
    var cameraId: String = ""
        private set
    
    var characteristics: CameraCharacteristics? = null
        private set

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "Camera opened: ${camera.id}")
            cameraOpenCloseLock.release()
            cameraDevice = camera
            
            // Detect capabilities when camera opens
            detectCapabilities()
            
            createPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "Camera disconnected: ${camera.id}")
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "Camera error: ${camera.id}, error: $error")
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
        }
    }

    fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also {
            it.start()
            backgroundHandler = Handler(it.looper)
        }
    }

    fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    fun openCamera(surfaceTexture: SurfaceTexture) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        try {
            // Find back camera
            for (id in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    characteristics = chars
                    
                    // Get optimal preview size
                    val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val outputSizes = map?.getOutputSizes(SurfaceTexture::class.java)
                    previewSize = chooseOptimalSize(outputSizes ?: emptyArray())
                    
                    Log.d(TAG, "Selected camera: $cameraId, preview size: $previewSize")
                    break
                }
            }
            
            if (cameraId.isEmpty()) {
                Log.e(TAG, "No back camera found")
                return
            }

            if (!cameraOpenCloseLock.tryAcquire(CAMERA_OPEN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                Log.e(TAG, "Timeout waiting to lock camera opening")
                return
            }

            manager.openCamera(cameraId, stateCallback, backgroundHandler)
            
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access exception", e)
            cameraOpenCloseLock.release()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception - camera permission not granted", e)
            cameraOpenCloseLock.release()
        }
    }

    fun detectCapabilities(): CameraCapabilities? {
        val chars = characteristics ?: return null
        
        val hardwareLevel = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ?: 0
        val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
        
        // Check format support
        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val outputFormats = map?.outputFormats ?: intArrayOf()
        val supportsRaw = outputFormats.any { 
            it == android.graphics.ImageFormat.RAW_SENSOR || 
            it == android.graphics.ImageFormat.RAW10 ||
            it == android.graphics.ImageFormat.RAW12
        }
        val supportsYuv = outputFormats.any { it == android.graphics.ImageFormat.YUV_420_888 }
        
        // Get format sizes
        val formatSizes = mutableMapOf<String, List<Size>>()
        outputFormats.forEach { format ->
            val sizes = map?.getOutputSizes(format) ?: emptyArray()
            formatSizes[CameraCapabilities.getFormatName(format)] = sizes.toList()
        }
        
        // Manual control support
        val supportsManualSensor = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
        val supportsManualFocus = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING)
        val supportsManualWhiteBalance = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING)
        
        // Sensor ranges
        val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exposureTimeRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
        val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val activeArraySize = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        
        // Processing modes
        val noiseReductionModes = chars.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)?.toList() ?: emptyList()
        val edgeModes = chars.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES)?.toList() ?: emptyList()
        val tonemapModes = chars.get(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES)?.toList() ?: emptyList()
        
        // Additional features
        val maxDigitalZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
        val supportsAeLock = chars.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE) == 1
        val supportsAwbLock = chars.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE) == 1
        
        val caps = CameraCapabilities(
            cameraId = cameraId,
            hardwareLevel = hardwareLevel,
            hardwareLevelName = CameraCapabilities.getHardwareLevelName(hardwareLevel),
            supportsRaw = supportsRaw,
            supportsYuv = supportsYuv,
            availableFormats = outputFormats.map { CameraCapabilities.getFormatName(it) },
            formatSizes = formatSizes,
            supportsManualSensor = supportsManualSensor,
            supportsManualFocus = supportsManualFocus,
            supportsManualWhiteBalance = supportsManualWhiteBalance,
            isoRange = isoRange,
            exposureTimeRange = exposureTimeRange,
            focalLengths = focalLengths,
            sensorSize = sensorSize,
            activeArraySize = activeArraySize,
            noiseReductionModes = noiseReductionModes,
            edgeModes = edgeModes,
            tonemapModes = tonemapModes,
            maxDigitalZoom = maxDigitalZoom,
            supportsAeLock = supportsAeLock,
            supportsAwbLock = supportsAwbLock
        )
        
        logCapabilities(caps)
        return caps
    }
    
    private fun logCapabilities(caps: CameraCapabilities) {
        Log.i(TAG, "=== Camera Capabilities ===")
        Log.i(TAG, "Camera ID: ${caps.cameraId}")
        Log.i(TAG, "Hardware Level: ${caps.hardwareLevelName}")
        Log.i(TAG, "Supports RAW: ${caps.supportsRaw}")
        Log.i(TAG, "Supports YUV: ${caps.supportsYuv}")
        Log.i(TAG, "Available Formats: ${caps.availableFormats.joinToString()}")
        
        caps.formatSizes.forEach { (format, sizes) ->
            Log.i(TAG, "  $format sizes: ${sizes.joinToString { "${it.width}x${it.height}" }}")
        }
        
        Log.i(TAG, "Manual Sensor Control: ${caps.supportsManualSensor}")
        Log.i(TAG, "Manual Focus Control: ${caps.supportsManualFocus}")
        Log.i(TAG, "Manual WB Control: ${caps.supportsManualWhiteBalance}")
        Log.i(TAG, "ISO Range: ${caps.isoRange}")
        Log.i(TAG, "Exposure Time Range: ${caps.exposureTimeRange}")
        Log.i(TAG, "Focal Lengths: ${caps.focalLengths.joinToString()}")
        Log.i(TAG, "Sensor Size: ${caps.sensorSize}")
        Log.i(TAG, "Active Array: ${caps.activeArraySize}")
        Log.i(TAG, "Noise Reduction Modes: ${caps.noiseReductionModes.joinToString()}")
        Log.i(TAG, "Edge Modes: ${caps.edgeModes.joinToString()}")
        Log.i(TAG, "Tonemap Modes: ${caps.tonemapModes.joinToString()}")
        Log.i(TAG, "Max Digital Zoom: ${caps.maxDigitalZoom}")
        Log.i(TAG, "AE Lock: ${caps.supportsAeLock}")
        Log.i(TAG, "AWB Lock: ${caps.supportsAwbLock}")
        Log.i(TAG, "==========================")
    }

    private fun createPreviewSession() {
        val device = cameraDevice ?: return
        val texture = previewSurfaceTexture ?: return
        
        try {
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)
            val surface = Surface(texture)
            
            previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
            }
            
            device.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        
                        captureSession = session
                        try {
                            previewRequestBuilder?.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            
                            captureSession?.setRepeatingRequest(
                                previewRequestBuilder!!.build(),
                                null,
                                backgroundHandler
                            )
                            
                            Log.d(TAG, "Preview session configured")
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "Failed to start preview", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Preview session configuration failed")
                    }
                },
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create preview session", e)
        }
    }

    private var previewSurfaceTexture: SurfaceTexture? = null

    fun setPreviewSurface(surfaceTexture: SurfaceTexture) {
        previewSurfaceTexture = surfaceTexture
        if (cameraDevice != null) {
            createPreviewSession()
        }
    }

    // Image capture
    private var imageReader: android.media.ImageReader? = null
    private var captureCallback: ((android.media.Image) -> Unit)? = null

    fun captureImage(callback: (android.media.Image) -> Unit) {
        val device = cameraDevice ?: run {
            Log.e(TAG, "Camera not opened")
            return
        }
        
        val session = captureSession ?: run {
            Log.e(TAG, "Preview session not configured")
            return
        }
        
        captureCallback = callback
        
        try {
            // Create ImageReader for YUV capture
            val captureSize = getCaptureSize()
            imageReader = android.media.ImageReader.newInstance(
                captureSize.width,
                captureSize.height,
                android.graphics.ImageFormat.YUV_420_888,
                2
            ).apply {
                setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage()
                    if (image != null) {
                        Log.d(TAG, "Image captured: ${image.width}x${image.height}")
                        callback(image)
                    }
                }, backgroundHandler)
            }
            
            // Create capture request
            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(imageReader!!.surface)
                
                // Use preview settings for now
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }
            
            // Capture
            session.capture(
                captureBuilder.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        Log.d(TAG, "Capture completed")
                    }
                },
                backgroundHandler
            )
            
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to capture image", e)
        }
    }
    
    private fun getCaptureSize(): Size {
        // Use largest YUV size available, up to 4K
        val map = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val yuvSizes = map?.getOutputSizes(android.graphics.ImageFormat.YUV_420_888) ?: emptyArray()
        
        return yuvSizes
            .filter { it.width <= 3840 && it.height <= 2160 } // Max 4K
            .maxByOrNull { it.width * it.height }
            ?: previewSize
    }

    fun closeCamera() {
        try {
            cameraOpenCloseLock.tryAcquire(CAMERA_OPEN_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            
            captureSession?.close()
            captureSession = null
            
            cameraDevice?.close()
            cameraDevice = null
            
            imageReader?.close()
            imageReader = null
            
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while closing camera", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun chooseOptimalSize(sizes: Array<Size>): Size {
        // Prefer 16:9 aspect ratio, close to 1080p
        val targetRatio = 16.0 / 9.0
        val targetHeight = 1080
        
        return sizes
            .filter { it.height <= 1920 } // Don't go too large
            .minByOrNull { size ->
                val ratio = size.width.toDouble() / size.height
                val ratioDiff = Math.abs(ratio - targetRatio)
                val heightDiff = Math.abs(size.height - targetHeight)
                ratioDiff * 1000 + heightDiff
            } ?: Size(1920, 1080)
    }
}
