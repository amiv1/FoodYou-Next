package com.maksimowiczm.foodyou.app.ui.home.goals

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalorieSummaryTest {

    @Test
    fun `normal_state`() {
        val summary = calorieSummaryOf(target = 1200, consumed = 266)

        assertEquals(CalorieState.NORMAL, summary.state)
        assertEquals(934, summary.remainingOrExcess)
        assertApproximately(266f / 1200f, summary.progress)
    }

    @Test
    fun `over_limit_state`() {
        val summary = calorieSummaryOf(target = 1200, consumed = 2664)

        assertEquals(CalorieState.OVER_LIMIT, summary.state)
        assertEquals(1464, summary.remainingOrExcess)
        assertEquals(1f, summary.progress)
    }

    @Test
    fun `exactly_at_target_is_normal_not_exceeded`() {
        val summary = calorieSummaryOf(target = 1200, consumed = 1200)

        assertEquals(CalorieState.NORMAL, summary.state)
        assertEquals(0, summary.remainingOrExcess)
        assertEquals(1f, summary.progress)
    }

    @Test
    fun `zero_target_does_not_crash_and_yields_zero_progress`() {
        val summary = calorieSummaryOf(target = 0, consumed = 0)

        assertEquals(CalorieState.NORMAL, summary.state)
        assertEquals(0, summary.remainingOrExcess)
        assertEquals(0f, summary.progress)
    }

    @Test
    fun `zero_consumed_yields_zero_progress`() {
        val summary = calorieSummaryOf(target = 1200, consumed = 0)

        assertEquals(CalorieState.NORMAL, summary.state)
        assertEquals(1200, summary.remainingOrExcess)
        assertEquals(0f, summary.progress)
    }

    @Test
    fun `large_values_remain_correct`() {
        val summary = calorieSummaryOf(target = 12_500, consumed = 25_000)

        assertEquals(CalorieState.OVER_LIMIT, summary.state)
        assertEquals(12_500, summary.remainingOrExcess)
        assertEquals(1f, summary.progress)
    }

    @Test
    fun `nutrient_progress_is_clamped_and_guards_zero_target`() {
        assertApproximately(19f / 90f, nutrientProgress(current = 19, target = 90))
        assertEquals(0f, nutrientProgress(current = 0, target = 0))
        assertEquals(1f, nutrientProgress(current = 999, target = 90))
        assertEquals(0f, nutrientProgress(current = 0, target = 90))
    }

    @Test
    fun `nutrient_exceeded_is_only_true_when_strictly_over_a_positive_target`() {
        assertEquals(false, isNutrientExceeded(current = 90, target = 90))
        assertEquals(true, isNutrientExceeded(current = 91, target = 90))
        assertEquals(false, isNutrientExceeded(current = 0, target = 0))
        assertEquals(false, isNutrientExceeded(current = 5, target = 0))
    }

    private fun assertApproximately(expected: Float, actual: Float, tolerance: Float = 0.001f) {
        assertTrue(
            kotlin.math.abs(expected - actual) <= tolerance,
            "Expected $expected but was $actual (tolerance $tolerance)",
        )
    }
}
