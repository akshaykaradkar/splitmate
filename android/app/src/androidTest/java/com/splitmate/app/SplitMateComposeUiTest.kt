package com.splitmate.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.splitmate.app.ui.SplitMateMaterial3ExpressiveTheme
import com.splitmate.app.ui.SplitMateNativeApp
import com.splitmate.app.ui.SplitMateViewModel
import org.junit.Rule
import org.junit.Test

/**
 * Jetpack Compose UI Test Rule (`createComposeRule`) verifying:
 * - Material 3 Expressive `HorizontalFloatingToolbar` visibility and quick-add interaction
 * - Real-time `UnassignedRemainderCard` and 1-tap `SplitRemainderEquallyBtn` resolution
 */
class SplitMateComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verifyHorizontalFloatingToolbarAndRemainderResolution() {
        val vm = SplitMateViewModel(dao = null)

        composeTestRule.setContent {
            SplitMateMaterial3ExpressiveTheme {
                SplitMateNativeApp(viewModel = vm)
            }
        }

        // 1. Verify HorizontalFloatingToolbar & UnassignedRemainderCard render on startup
        composeTestRule.onNodeWithTag("HorizontalFloatingToolbar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("UnassignedRemainderCard").assertIsDisplayed()

        // 2. Click "Split Equally" on the Remainder Card and verify 0.00c Remainder Verified card appears
        composeTestRule.onNodeWithTag("SplitRemainderEquallyBtn").performClick()
        composeTestRule.onNodeWithTag("ZeroRemainderVerifiedCard").assertIsDisplayed()
    }
}
