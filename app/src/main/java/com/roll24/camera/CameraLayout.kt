package com.roll24.camera

internal enum class CameraPanel {
    NONE,
    CAMERA,
    LAB,
    FILMS
}

internal enum class CameraLayoutMode {
    PORTRAIT,
    LANDSCAPE
}

internal data class WindowLayout(
    val mode: CameraLayoutMode,
    val compactHeight: Boolean,
    val wide: Boolean
)

internal fun resolveWindowLayout(widthDp: Float, heightDp: Float): WindowLayout {
    return WindowLayout(
        mode = if (widthDp > heightDp) CameraLayoutMode.LANDSCAPE else CameraLayoutMode.PORTRAIT,
        compactHeight = heightDp < 520f,
        wide = widthDp >= 840f
    )
}

internal fun togglePanel(current: CameraPanel, requested: CameraPanel): CameraPanel {
    return if (current == requested) CameraPanel.NONE else requested
}
