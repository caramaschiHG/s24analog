package com.roll24.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import java.util.LinkedHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/** Correlates Camera2 capture metadata with CameraX images by sensor timestamp. */
class CaptureResultStore {
    private val lock = Any()
    private val recent = LinkedHashMap<Long, TotalCaptureResult>()
    private val waiters = mutableMapOf<Long, CompletableFuture<TotalCaptureResult>>()

    val callback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            val timestamp = result.get(CaptureResult.SENSOR_TIMESTAMP) ?: return
            synchronized(lock) {
                waiters.remove(timestamp)?.complete(result) ?: run {
                    recent[timestamp] = result
                    while (recent.size > MAX_RECENT_RESULTS) {
                        recent.remove(recent.keys.first())
                    }
                }
            }
        }
    }

    fun await(timestamp: Long, timeoutMillis: Long): TotalCaptureResult {
        val future = synchronized(lock) {
            recent.remove(timestamp)?.let { return it }
            waiters.getOrPut(timestamp) { CompletableFuture() }
        }
        return try {
            future.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } finally {
            synchronized(lock) { waiters.remove(timestamp) }
        }
    }

    fun clear() = synchronized(lock) {
        recent.clear()
        waiters.values.forEach { it.cancel(true) }
        waiters.clear()
    }

    private companion object {
        const val MAX_RECENT_RESULTS = 16
    }
}
