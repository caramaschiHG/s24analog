package com.roll24.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraLayoutTest {

    @Test
    fun `portrait phone uses portrait controls`() {
        val layout = resolveWindowLayout(widthDp = 360f, heightDp = 800f)

        assertEquals(CameraLayoutMode.PORTRAIT, layout.mode)
        assertFalse(layout.compactHeight)
        assertFalse(layout.wide)
    }

    @Test
    fun `short landscape uses compact side controls`() {
        val layout = resolveWindowLayout(widthDp = 800f, heightDp = 360f)

        assertEquals(CameraLayoutMode.LANDSCAPE, layout.mode)
        assertTrue(layout.compactHeight)
        assertFalse(layout.wide)
    }

    @Test
    fun `large landscape is marked as wide`() {
        val layout = resolveWindowLayout(widthDp = 1200f, heightDp = 800f)

        assertEquals(CameraLayoutMode.LANDSCAPE, layout.mode)
        assertFalse(layout.compactHeight)
        assertTrue(layout.wide)
    }

    @Test
    fun `requesting another panel replaces the current panel`() {
        assertEquals(CameraPanel.LAB, togglePanel(CameraPanel.CAMERA, CameraPanel.LAB))
        assertEquals(CameraPanel.NONE, togglePanel(CameraPanel.LAB, CameraPanel.LAB))
        assertEquals(CameraPanel.FILMS, togglePanel(CameraPanel.NONE, CameraPanel.FILMS))
    }
}
