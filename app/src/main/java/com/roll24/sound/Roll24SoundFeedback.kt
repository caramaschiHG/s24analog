package com.roll24.sound

/**
 * Abstraction for audible feedback in Roll24.
 * Currently no-op — designed so that premium click/zoom/shutter sounds
 * can be added in the future without touching the calling code.
 */
interface Roll24SoundFeedback {
    /** Short click when zoom snaps to an optical anchor (0.6x/1x/3x/5x). */
    fun zoomAnchorClick()

    /** Mechanical shutter sound on capture. */
    fun shutterClick()
}

/**
 * Silent implementation. Used until premium audio assets are integrated.
 */
object NoOpRoll24SoundFeedback : Roll24SoundFeedback {
    override fun zoomAnchorClick() = Unit
    override fun shutterClick() = Unit
}
