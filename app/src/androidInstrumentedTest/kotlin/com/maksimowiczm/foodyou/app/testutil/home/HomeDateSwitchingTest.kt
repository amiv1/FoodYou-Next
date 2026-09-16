package com.maksimowiczm.foodyou.app.testutil.home

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.maksimowiczm.foodyou.app.testutil.FoodYouComposeTest
import com.maksimowiczm.foodyou.app.testutil.onDisplayed
import com.maksimowiczm.foodyou.app.testutil.textValue
import com.maksimowiczm.foodyou.common.compose.utility.TestTags
import org.junit.Test

/**
 * Covers switching the Home screen's selected date, both via the whole-screen swipe gesture and
 * via the compact calendar bar (arrow buttons + date-picker dialog).
 */
class HomeDateSwitchingTest : FoodYouComposeTest() {

    private fun currentDateLabel(): String =
        composeRule.onAllNodesWithTag(TestTags.CalendarDateButton).onDisplayed().textValue()

    @Test
    fun swiping_left_from_today_is_a_no_op() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assert(currentDateLabel() == "Today") {
            "Expected date label to remain \"Today\" after swiping left (into the future), " +
                "but was \"${currentDateLabel()}\""
        }
    }

    @Test
    fun swiping_left_twice_from_today_is_still_a_no_op() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assert(currentDateLabel() == "Today") {
            "Expected date label to remain \"Today\" after two consecutive future swipes, " +
                "but was \"${currentDateLabel()}\""
        }
    }

    @Test
    fun swiping_right_moves_to_the_previous_day() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assert(currentDateLabel() != "Today") {
            "Expected date label to no longer be \"Today\" after swiping right (to the previous " +
                "day), but it was."
        }
    }

    @Test
    fun swiping_right_then_going_to_today_returns_to_today() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assert(currentDateLabel() != "Today")

        composeRule.onAllNodesWithTag(TestTags.CalendarDateButton).onDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Go to today").performClick()
        composeRule.waitForIdle()

        assert(currentDateLabel() == "Today") {
            "Expected date label to be \"Today\" after using the \"Go to today\" shortcut, but " +
                "was \"${currentDateLabel()}\""
        }
    }

    @Test
    fun picking_an_arbitrary_earlier_date_via_the_date_picker_updates_the_label() {
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(TestTags.CalendarDateButton).onDisplayed().performClick()
        composeRule.waitForIdle()

        // The Material3 DatePicker's day cells expose their full announced date (e.g. "Tuesday,
        // September 1, 2026") as their accessible text, not just the bare day-of-month number.
        // The 1st of the currently displayed month is always selectable/in the past-or-present
        // relative to "today" (since the dialog opens showing today's month), so match on that
        // month/year with a trailing comma right after the day number to avoid accidentally
        // matching "11"/"21"/"31" (e.g. "September 1, 2026" is not a substring of
        // "September 11, 2026").
        val today = java.time.LocalDate.now()
        val monthName = today.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
        val firstOfMonthText = "$monthName 1, ${today.year}"
        composeRule
            .onAllNodesWithText(firstOfMonthText, substring = true, useUnmergedTree = true)
            .onDisplayed()
            .performClick()
        composeRule.waitForIdle()

        assert(currentDateLabel() != "Today") {
            "Expected date label to change away from \"Today\" after picking an arbitrary date " +
                "from the calendar grid, but it was still \"Today\"."
        }
    }

    @Test
    fun next_day_arrow_is_disabled_on_today() {
        composeRule.waitForIdle()
        composeRule
            .onAllNodesWithTag(TestTags.CalendarNextDayButton)
            .onDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun previous_day_arrow_click_moves_back_one_day_and_re_enables_next_day_arrow() {
        composeRule.waitForIdle()
        composeRule
            .onAllNodesWithTag(TestTags.CalendarPreviousDayButton)
            .onDisplayed()
            .performClick()
        composeRule.waitForIdle()

        assert(currentDateLabel() != "Today")
        composeRule
            .onAllNodesWithTag(TestTags.CalendarNextDayButton)
            .onDisplayed()
            .assertIsEnabled()
    }
}
