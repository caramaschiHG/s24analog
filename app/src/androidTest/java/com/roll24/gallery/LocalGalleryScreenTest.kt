package com.roll24.gallery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.roll24.ui.theme.Roll24Theme
import org.junit.Rule
import org.junit.Test

class LocalGalleryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStatesRemainNavigable() {
        composeRule.setContent {
            Roll24Theme {
                LocalGalleryScreen(captures = emptyList(), onClose = {}, onRemoveLocal = {})
            }
        }

        composeRule.onNodeWithText("Nenhuma fotografia por aqui").assertIsDisplayed()
        composeRule.onNodeWithText("Falhas").performClick()
        composeRule.onNodeWithText("Falhas de processamento aparecerão aqui para diagnóstico.").assertIsDisplayed()
    }
}
