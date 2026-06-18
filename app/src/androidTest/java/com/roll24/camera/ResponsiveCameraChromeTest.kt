package com.roll24.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import com.roll24.film.FilmLabSettings
import com.roll24.film.FilmProfile
import com.roll24.ui.theme.Roll24Theme
import org.junit.Rule
import org.junit.Test

class ResponsiveCameraChromeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryActionsRemainAvailableAndPanelCanClose() {
        composeRule.setContent {
            var panel by rememberSaveable { mutableStateOf(CameraPanel.NONE) }
            Roll24Theme {
                ResponsiveCameraChrome(
                    selectedProfile = FilmProfile.NEUTRAL,
                    cameraSettings = CameraSettings(),
                    labSettings = FilmLabSettings(),
                    uiState = previewUiState(),
                    galleryCount = 3,
                    activePanel = panel,
                    onPanelChange = { panel = it },
                    onLensSelected = {},
                    onProfileSelected = {},
                    onAspectChange = {},
                    onGridChange = {},
                    onFlashChange = {},
                    onTimerChange = {},
                    onEvChange = {},
                    onLabChange = {},
                    onCapture = {},
                    captureEnabled = true,
                    onGalleryOpen = {}
                )
            }
        }

        composeRule.onNodeWithTag(TAG_CAMERA_CHROME).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Capturar").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Câmera").performClick()
        composeRule.onNodeWithTag(TAG_ADAPTIVE_PANEL).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Fechar").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(TAG_ADAPTIVE_PANEL).assertCountEquals(0)
    }

    private fun previewUiState(): CameraUiState {
        val lens = SensorProfile(
            cameraId = "0",
            physicalId = null,
            lensLabel = "1x",
            supportsRaw = true,
            supportsManual = true,
            focalLengthMm = 24f,
            aperture = 1.7f,
            isoRange = "50-3200",
            exposureRange = "1/12000-30s",
            rawSizes = emptyList(),
            yuvSizes = emptyList()
        )
        return CameraUiState(
            isCameraReady = true,
            activeLens = lens,
            sensorProfiles = listOf(lens)
        )
    }
}
