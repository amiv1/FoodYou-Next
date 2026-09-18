package com.maksimowiczm.foodyou.app.testutil.home

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import com.maksimowiczm.foodyou.app.testutil.FoodYouComposeTest
import com.maksimowiczm.foodyou.app.testutil.anyDisplayed
import com.maksimowiczm.foodyou.app.testutil.onDisplayed
import com.maksimowiczm.foodyou.app.testutil.textValue
import com.maksimowiczm.foodyou.common.compose.utility.TestTags
import org.junit.Test

/**
 * Covers the Home screen's "Today" floating action button: it only appears while viewing a
 * non-today date, jumps back to today when clicked (reusing the swipe/jump animation, even from a
 * distant date), and collapses from icon+label to icon-only once the user starts scrolling the
 * selected day's content - resetting back to expanded whenever the displayed date changes again.
 */
class HomeTodayFabTest : FoodYouComposeTest() {

    private fun currentDateLabel(): String =
        composeRule.onAllNodesWithTag(TestTags.CalendarDateButton).onDisplayed().textValue()

    private fun swipeToPreviousDay() {
        composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeRight() }
        composeRule.waitForIdle()
    }

    private fun swipeToNextDay() {
        composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
    }

    private fun isFabDisplayed(): Boolean =
        composeRule.onAllNodesWithTag(TestTags.TodayFab).anyDisplayed()

    /** Current pixel width of the (single, currently displayed) "Today" FAB node. */
    private fun fabWidth(): Int =
        composeRule
            .onAllNodesWithTag(TestTags.TodayFab)
            .onDisplayed()
            .fetchSemanticsNode()
            .size
            .width

    @Test
    fun fab_is_hidden_while_viewing_today() {
        composeRule.waitForIdle()

        assert(!isFabDisplayed()) {
            "Expected the \"Today\" FAB to be absent while today is selected."
        }
    }

    @Test
    fun fab_appears_after_swiping_away_from_today() {
        composeRule.waitForIdle()
        swipeToPreviousDay()

        assert(currentDateLabel() != "Today")
        assert(isFabDisplayed()) {
            "Expected the \"Today\" FAB to appear after navigating away from today."
        }
    }

    @Test
    fun clicking_the_fab_returns_to_today_and_the_fab_disappears() {
        composeRule.waitForIdle()
        swipeToPreviousDay()
        swipeToPreviousDay()
        assert(currentDateLabel() != "Today")

        composeRule.onAllNodesWithTag(TestTags.TodayFab).onDisplayed().performClick()
        composeRule.waitForIdle()

        assert(currentDateLabel() == "Today") {
            "Expected date label to be \"Today\" after clicking the \"Today\" FAB, but was " +
                "\"${currentDateLabel()}\"."
        }
        assert(!isFabDisplayed()) {
            "Expected the \"Today\" FAB to disappear once back on today."
        }
    }

    @Test
    fun clicking_the_fab_from_a_distant_date_still_returns_to_today() {
        composeRule.waitForIdle()
        // Several days back - not just the adjacent day - to exercise the "jump" path (which
        // slides through a synthetic neighbor rather than the literal adjacent day).
        repeat(5) { swipeToPreviousDay() }
        assert(currentDateLabel() != "Today")
        assert(isFabDisplayed())

        composeRule.onAllNodesWithTag(TestTags.TodayFab).onDisplayed().performClick()
        composeRule.waitForIdle()

        assert(currentDateLabel() == "Today") {
            "Expected date label to be \"Today\" after clicking the \"Today\" FAB from a " +
                "distant date, but was \"${currentDateLabel()}\"."
        }
        assert(!isFabDisplayed())
    }

    @Test
    fun fab_collapses_to_icon_only_after_scrolling_the_content() {
        composeRule.waitForIdle()
        swipeToPreviousDay()
        assert(isFabDisplayed())

        val expandedWidth = fabWidth()

        composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        assert(isFabDisplayed()) {
            "Expected the \"Today\" FAB to remain visible (just collapsed) after scrolling."
        }
        val collapsedWidth = fabWidth()
        assert(collapsedWidth < expandedWidth) {
            "Expected the \"Today\" FAB to shrink after scrolling the content (icon-only), but " +
                "width went from $expandedWidth to $collapsedWidth."
        }
    }

    @Test
    fun fab_re_expands_after_switching_to_a_different_date() {
        composeRule.waitForIdle()
        swipeToPreviousDay()
        val expandedWidth = fabWidth()

        composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        val collapsedWidth = fabWidth()
        assert(collapsedWidth < expandedWidth)

        // Switching to yet another non-today date should reset the collapse state.
        swipeToPreviousDay()
        val reExpandedWidth = fabWidth()

        assert(reExpandedWidth > collapsedWidth) {
            "Expected the \"Today\" FAB to re-expand after switching to a new date, but width " +
                "was $reExpandedWidth (collapsed width was $collapsedWidth)."
        }
    }

    @Test
    fun fab_does_not_reappear_when_swiping_within_todays_future_bound() {
        composeRule.waitForIdle()
        // Swiping left (into the future) from today is a documented no-op (see
        // HomeDateSwitchingTest#swiping_left_from_today_is_a_no_op) - the FAB should correctly
        // stay hidden since the selected date never actually changes away from today.
        swipeToNextDay()

        assert(currentDateLabel() == "Today") {
            "Expected date label to remain \"Today\" after swiping into the future, but was " +
                "\"${currentDateLabel()}\""
        }
        assert(!isFabDisplayed()) {
            "Expected the \"Today\" FAB to remain absent since swiping into the future from " +
                "today is a no-op."
        }
    }
}
