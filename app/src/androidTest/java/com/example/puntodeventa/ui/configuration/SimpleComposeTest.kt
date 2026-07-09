package com.example.puntodeventa.ui.configuration

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Minimal Compose test to verify the test rule works with MaterialTheme.
 */
@RunWith(AndroidJUnit4::class)
class SimpleComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun simpleText_isDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                Text(text = "Hello World")
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hello World").assertIsDisplayed()
    }
}
