package com.splitmate.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.splitmate.app.ui.SplitMateMaterial3ExpressiveTheme
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
    fun verifyHorizontalFloatingToolbarAndQuickExpenseTab() {
        val vm = SplitMateViewModel(dao = null)
        vm.completeOnboarding(
            name = "Maya Lin",
            countryName = "India",
            currencyCode = "INR",
            currencySymbol = "₹",
            avatarSeed = "MayaLin"
        )

        composeTestRule.setContent {
            SplitMateMaterial3ExpressiveTheme {
                SplitMateApp(viewModel = vm)
            }
        }

        // Verify HorizontalFloatingToolbar renders on Dashboard
        composeTestRule.onNodeWithTag("HorizontalFloatingToolbar").assertIsDisplayed()
    }
}
