package com.maksimowiczm.foodyou.app.testutil

import androidx.compose.ui.test.onNodeWithTag
import com.maksimowiczm.foodyou.common.compose.utility.TestTags
import org.junit.Test

/** Smoke test verifying the base test harness (onboarding bypass, Home reachable) works. */
class SmokeTest : FoodYouComposeTest() {
    @Test
    fun homeScreenIsReachable() {
        composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).assertExists()
    }
}
