package com.maksimowiczm.foodyou.app.ui.home.goals

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalorieSummaryTest {

    @Test
    fun `below_target_is_normal_and_not_excess`() {
        val summary = calorieSummaryOf(target = 2000, consumed = 1800, allowedDifference = 100)

        assertFalse(summary.isExcess)
        assertEquals(CalorieState.NORMAL, summary.state)
        assertEquals(200, summary.remainingOrExcess)
        // barMax = 2000 + 2*100 = 2200
        assertApproximately(1800f / 2200f, summary.progress)
        assertApproximately(1900f / 2200f, summary.normalEndFraction)
        assertApproximately(2100f / 2200f, summary.warningEndFraction)
        assertApproximately(2000f / 2200f, summary.targetFraction)
    }

    @Test
    fun `exactly_at_target_is_not_excess`() {
        val summary = calorieSummaryOf(target = 2000, consumed = 2000, allowedDifference = 100)

        assertFalse(summary.isExcess)
        assertEquals(CalorieState.NORMAL, summary.state)
        assertEquals(0, summary.remainingOrExcess)
    }

    @Test
    fun `just_over_target_but_within_tolerance_is_excess_but_not_over_limit`() {
        // Between target (2000) and target+allowedDifference (2100): label flips to "excess" but
        // the number/bar don't turn red yet.
        val summary = calorieSummaryOf(target = 2000, consumed = 2050, allowedDifference = 100)

        assertTrue(summary.isExcess)
        assertEquals(CalorieState.NORMAL, summary.state)
        assertEquals(50, summary.remainingOrExcess)
    }

    @Test
    fun `over_target_plus_allowed_difference_is_over_limit`() {
        val summary = calorieSummaryOf(target = 2000, consumed = 2150, allowedDifference = 100)

        assertTrue(summary.isExcess)
        assertEquals(CalorieState.OVER_LIMIT, summary.state)
        assertEquals(150, summary.remainingOrExcess)
        // barMax stays at the fixed 2200 since consumed (2150) doesn't exceed it.
        assertApproximately(2150f / 2200f, summary.progress)
    }

    @Test
    fun `bar_max_grows_past_fixed_max_for_large_excess_showing_excess_proportion`() {
        // target=2000, allowedDifference=100 -> fixedMax=2200. consumed=4400 far exceeds it, so
        // barMax grows to 4400 (bar stays fully filled) and the red portion of the bar reflects
        // how large the excess is, roughly half the bar in this case.
        val summary = calorieSummaryOf(target = 2000, consumed = 4400, allowedDifference = 100)

        assertTrue(summary.isExcess)
        assertEquals(CalorieState.OVER_LIMIT, summary.state)
        assertEquals(1f, summary.progress)
        assertApproximately(1900f / 4400f, summary.normalEndFraction)
        assertApproximately(2100f / 4400f, summary.warningEndFraction)
        assertApproximately(2000f / 4400f, summary.targetFraction)

        val redFraction = 1f - summary.warningEndFraction
        assertTrue(
            redFraction in 0.45f..0.55f,
            "Expected roughly half the bar to be red, but was $redFraction",
        )
    }

    @Test
    fun `zero_target_does_not_crash_and_yields_zero_progress`() {
        val summary = calorieSummaryOf(target = 0, consumed = 0, allowedDifference = 100)

        assertFalse(summary.isExcess)
        assertEquals(CalorieState.NORMAL, summary.state)
        assertEquals(0, summary.remainingOrExcess)
        assertEquals(0f, summary.progress)
        assertEquals(0f, summary.normalEndFraction)
        assertEquals(0f, summary.warningEndFraction)
        assertEquals(0f, summary.targetFraction)
    }

    @Test
    fun `zero_consumed_yields_zero_progress`() {
        val summary = calorieSummaryOf(target = 2000, consumed = 0, allowedDifference = 100)

        assertFalse(summary.isExcess)
        assertEquals(CalorieState.NORMAL, summary.state)
        assertEquals(2000, summary.remainingOrExcess)
        assertEquals(0f, summary.progress)
    }

    private fun assertApproximately(expected: Float, actual: Float, tolerance: Float = 0.001f) {
        assertTrue(
            kotlin.math.abs(expected - actual) <= tolerance,
            "Expected $expected but was $actual (tolerance $tolerance)",
        )
    }
}
