package com.roll24.camera

enum class ViewfinderAspect(val label: String, val ratio: Float) {
    CLASSIC_3_2("3:2", 3f / 2f),
    SENSOR_4_3("4:3", 4f / 3f),
    SQUARE_1_1("1:1", 1f),
    WIDE_16_9("16:9", 16f / 9f)
}

enum class GridMode(val label: String) {
    OFF("Off"),
    THIRDS("Thirds"),
    CROSS("Cross"),
    DIAGONALS("Diag")
}

enum class Roll24FlashMode(val label: String) {
    OFF("Off"),
    AUTO("Auto"),
    ON("On")
}

enum class TimerMode(val label: String, val delayMillis: Long) {
    OFF("Off", 0L),
    THREE_SECONDS("3s", 3_000L),
    TEN_SECONDS("10s", 10_000L)
}

enum class CleanCaptureMode(val label: String) {
    AUTO("Auto"),
    MINIMAL_PROCESSING("Clean")
}

data class CameraSettings(
    val aspect: ViewfinderAspect = ViewfinderAspect.CLASSIC_3_2,
    val gridMode: GridMode = GridMode.THIRDS,
    val flashMode: Roll24FlashMode = Roll24FlashMode.OFF,
    val timerMode: TimerMode = TimerMode.OFF,
    val exposureCompensation: Float = 0f,
    val focusLocked: Boolean = false,
    val cleanCaptureMode: CleanCaptureMode = CleanCaptureMode.AUTO
)

