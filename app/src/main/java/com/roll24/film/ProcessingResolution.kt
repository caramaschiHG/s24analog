package com.roll24.film

/**
 * Development output resolution presets.
 *
 * Processing at lower resolutions reduces CPU cost for previews, thumbnails and
 * quick shares. The final bitmap dimensions match the selected preset unless
 * explicit [targetOutputWidth]/[targetOutputHeight] hints are provided in
 * [FilmLabSettings].
 */
enum class ProcessingResolution {
    FULL,
    HALF,
    QUARTER,
    THUMBNAIL;

    /**
     * Computes the target output size for an input image.
     *
     * When [hintWidth] and [hintHeight] are both positive they take precedence
     * over the preset. This allows callers to request an exact output size
     * (e.g. for a fixed-width export) while still benefiting from the pipeline's
     * resolution-aware path.
     */
    fun computeTargetSize(
        inputWidth: Int,
        inputHeight: Int,
        hintWidth: Int? = null,
        hintHeight: Int? = null
    ): Pair<Int, Int> {
        if (hintWidth != null && hintHeight != null && hintWidth > 0 && hintHeight > 0) {
            return hintWidth to hintHeight
        }

        return when (this) {
            FULL -> inputWidth to inputHeight
            HALF -> (inputWidth / 2).coerceAtLeast(1) to (inputHeight / 2).coerceAtLeast(1)
            QUARTER -> (inputWidth / 4).coerceAtLeast(1) to (inputHeight / 4).coerceAtLeast(1)
            THUMBNAIL -> {
                val maxSide = THUMBNAIL_MAX_SIDE
                val scale = maxSide.toFloat() / maxOf(inputWidth, inputHeight)
                if (scale >= 1f) return inputWidth to inputHeight
                val w = (inputWidth * scale).toInt().coerceAtLeast(1)
                val h = (inputHeight * scale).toInt().coerceAtLeast(1)
                w to h
            }
        }
    }

    companion object {
        private const val THUMBNAIL_MAX_SIDE = 256
    }
}
