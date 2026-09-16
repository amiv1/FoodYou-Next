package com.maksimowiczm.foodyou.app.testutil.home

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import com.maksimowiczm.foodyou.app.testutil.FoodYouComposeTest
import com.maksimowiczm.foodyou.app.testutil.anyDisplayed
import com.maksimowiczm.foodyou.app.testutil.onDisplayed
import com.maksimowiczm.foodyou.common.compose.utility.TestTags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Covers the Meals section's "time-based ordering" setting (`MealsPreferences.useTimeBasedSorting`
 * in [com.maksimowiczm.foodyou.fooddiary.domain.usecase.ObserveDiaryMealsUseCase]): meals whose
 * time window does not currently include "now" get pushed far down the list when the setting is
 * enabled, while it's a no-op (plain rank order) when disabled.
 */
class MealsSortingTest : FoodYouComposeTest() {

    private val suffix = System.currentTimeMillis()

    // Created first (lower rank), but given a ~1 minute window unlikely to include "now" - will be
    // pushed to the very bottom of the list once time-based sorting is enabled.
    private val inactiveMealName = "ZZZ Inactive Meal $suffix"

    // Created second (higher rank), but given an all-day-minus-a-minute window that (almost)
    // always includes "now" - stays near its rank position once time-based sorting is enabled.
    private val activeMealName = "AAA Active Meal $suffix"

    private var inactiveMealId: Long = -1
    private var activeMealId: Long = -1

    @Before
    fun seedMeals() {
        runBlocking {
            mealRepository.insertMealWithLastRank(
                name = inactiveMealName,
                from = LocalTime(0, 0),
                to = LocalTime(0, 1),
            )
            inactiveMealId = mealRepository.observeMeals().first().first {
                it.name == inactiveMealName
            }.id

            mealRepository.insertMealWithLastRank(
                name = activeMealName,
                from = LocalTime(0, 0),
                to = LocalTime(23, 58),
            )
            activeMealId = mealRepository.observeMeals().first().first {
                it.name == activeMealName
            }.id
        }
    }

    @After
    fun cleanUp() {
        runBlocking {
            mealsPreferencesRepository.update { copy(useTimeBasedSorting = false) }
            if (inactiveMealId != -1L) mealRepository.deleteMeal(inactiveMealId)
            if (activeMealId != -1L) mealRepository.deleteMeal(activeMealId)
        }
    }

    @Test
    fun rank_order_by_default_then_time_based_order_when_enabled() {
        composeRule.waitForIdle()

        // Default: useTimeBasedSorting is false -> plain rank order -> the meal created first
        // (inactiveMealName) appears before the one created second (activeMealName).
        val defaultOrderFirst = firstToAppearWhileScrollingDown(inactiveMealName, activeMealName)
        assert(defaultOrderFirst == inactiveMealName) {
            "Expected '$inactiveMealName' (lower rank) before '$activeMealName' by default, " +
                "but '$defaultOrderFirst' appeared first."
        }

        resetScrollToTop()
        runBlocking { mealsPreferencesRepository.update { copy(useTimeBasedSorting = true) } }
        composeRule.waitForIdle()

        // With time-based sorting on, the meal whose window excludes "now" is pushed far down,
        // while the (almost) always-active meal keeps its normal rank position - order swaps.
        val timeBasedOrderFirst = firstToAppearWhileScrollingDown(inactiveMealName, activeMealName)
        assert(timeBasedOrderFirst == activeMealName) {
            "Expected '$activeMealName' (currently active) before '$inactiveMealName' once " +
                "time-based sorting is enabled, but '$timeBasedOrderFirst' appeared first."
        }
    }

    /**
     * Scrolls the Home screen's vertical content down (starting from wherever it currently is,
     * so callers should [resetScrollToTop] first for a deterministic starting point) and returns
     * whichever of [nameA]/[nameB] is encountered first - i.e. rendered higher up the list.
     */
    private fun firstToAppearWhileScrollingDown(nameA: String, nameB: String): String {
        repeat(30) {
            val aVisible = composeRule.onAllNodesWithText(nameA).anyDisplayed()
            val bVisible = composeRule.onAllNodesWithText(nameB).anyDisplayed()
            if (aVisible && bVisible) {
                val aTop = composeRule.onAllNodesWithText(nameA).onDisplayed().boundsTop()
                val bTop = composeRule.onAllNodesWithText(nameB).onDisplayed().boundsTop()
                return if (aTop <= bTop) nameA else nameB
            }
            if (aVisible) return nameA
            if (bVisible) return nameB
            composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
        error("Neither '$nameA' nor '$nameB' became visible while scrolling down.")
    }

    private fun SemanticsNodeInteraction.boundsTop(): Float = fetchSemanticsNode().boundsInRoot.top

    private fun resetScrollToTop() {
        repeat(15) {
            composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeDown() }
        }
        composeRule.waitForIdle()
    }
}
