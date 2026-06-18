@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)

package com.roll24.camera

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.DngCreator
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import com.roll24.image.CaptureMetadata
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Captures RAW+JPEG from the ImageCapture already bound with the preview. */
object RawCaptureSession {
    private const val TAG = "RawCaptureSession"
    // S24 RAW+JPEG can take several seconds in low light. The 2 s limit applies
    // only to result correlation after an image arrives, not to sensor capture.
    private const val CAPTURE_TIMEOUT_MS = 15_000L
    private const val RESULT_TIMEOUT_MS = 1_800L
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Roll24RawCapture")
    }

    suspend fun capture(
        imageCapture: ImageCapture,
        expectsRaw: Boolean,
        characteristics: CameraCharacteristics?,
        resultStore: CaptureResultStore
    ): RawCaptureResult = withTimeout(CAPTURE_TIMEOUT_MS) {
        val deferred = CompletableDeferred<RawCaptureResult>()
        var jpegBytes: ByteArray? = null
        var jpegMetadata: CaptureMetadata? = null
        var jpegWidth = 0
        var jpegHeight = 0
        var rawBytes: ByteArray? = null
        var rawFrame: RawFrame? = null
        var rawFailure: String? = null

        fun completeIfReady() {
            val jpeg = jpegBytes ?: return
            if (expectsRaw && rawFrame == null && rawFailure == null) return
            val frame = rawFrame
            deferred.complete(
                RawCaptureResult(
                    source = if (frame != null) CaptureSource.RAW_DNG else CaptureSource.JPEG,
                    encoding = InputEncoding.SRGB,
                    jpegBytes = jpeg,
                    rawBytes = rawBytes,
                    rawFrame = frame,
                    metadata = frame?.metadata ?: jpegMetadata,
                    width = frame?.width ?: jpegWidth,
                    height = frame?.height ?: jpegHeight,
                    fallbackReason = rawFailure
                )
            )
        }

        imageCapture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val format = runCatching { image.format }.getOrElse { error ->
                        runCatching { image.close() }
                        if (!deferred.isCompleted) deferred.completeExceptionally(error)
                        return
                    }
                    try {
                        when (format) {
                            ImageFormat.JPEG -> {
                                val bytes = image.firstPlaneBytes()
                                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                                jpegWidth = bounds.outWidth
                                jpegHeight = bounds.outHeight
                                jpegMetadata = runCatching {
                                    CaptureMetadata.fromExifInterface(
                                        ExifInterface(ByteArrayInputStream(bytes)),
                                        jpegWidth,
                                        jpegHeight
                                    )
                                }.getOrNull()
                                jpegBytes = bytes
                            }
                            ImageFormat.RAW_SENSOR -> {
                                val chars = characteristics
                                    ?: error("Caracteristicas da camera indisponiveis para RAW")
                                val captureResult = resultStore.await(
                                    image.imageInfo.timestamp,
                                    RESULT_TIMEOUT_MS
                                )
                                val capturedFrame = RawFrame.fromImageProxy(image, chars, captureResult)
                                val capturedDng = ByteArrayOutputStream().use { output ->
                                    val androidImage = image.image
                                        ?: error("Imagem RAW nativa indisponivel")
                                    DngCreator(chars, captureResult).use { creator ->
                                        creator.writeImage(output, androidImage)
                                    }
                                    output.toByteArray()
                                }
                                rawFrame = capturedFrame
                                rawBytes = capturedDng
                            }
                            else -> if (expectsRaw) {
                                rawFailure = "Formato RAW inesperado: $format"
                            }
                        }
                    } catch (error: Throwable) {
                        Log.e(TAG, "Failed to consume capture format=$format", error)
                        if (format == ImageFormat.RAW_SENSOR) {
                            rawFailure = error.message ?: "Falha ao preparar o RAW"
                        } else if (!deferred.isCompleted) {
                            deferred.completeExceptionally(error)
                        }
                    } finally {
                        runCatching { image.close() }
                    }
                    completeIfReady()
                }

                override fun onError(exception: ImageCaptureException) {
                    if (!deferred.isCompleted) deferred.completeExceptionally(exception)
                }
            }
        )

        try {
            deferred.await()
        } finally {
            if (deferred.isCancelled) resultStore.clear()
        }
    }

    private fun ImageProxy.firstPlaneBytes(): ByteArray {
        val buffer = planes.first().buffer.duplicate()
        return ByteArray(buffer.remaining()).also(buffer::get)
    }
}
