package com.maksimowiczm.foodyou.common.compose.component

import kotlin.test.Test
import kotlin.test.assertEquals

class GrowingProgressTest {

    @Test
    fun `below_target_progress_is_a_plain_fraction`() {
        val result = growingProgressOf(current = 19.0, target = 90.0)

        assertApproximately(19f / 90f, result.progress)
        assertApproximately(1f, result.normalEndFraction)
    }

    @Test
    fun `exactly_at_target_is_full_and_not_exceeded`() {
        val result = growingProgressOf(current = 90.0, target = 90.0)

        assertEquals(1f, result.progress)
        assertEquals(1f, result.normalEndFraction)
    }

    @Test
    fun `over_target_grows_the_bar_and_shrinks_the_normal_fraction`() {
        // barMax grows to match current (180), so progress is always full once exceeded, while
        // normalEndFraction shrinks to reflect how large the excess is relative to the target.
        val result = growingProgressOf(current = 180.0, target = 90.0)

        assertEquals(1f, result.progress)
        assertApproximately(0.5f, result.normalEndFraction)
    }

    @Test
    fun `large_excess_shrinks_normal_fraction_further`() {
        // Mirrors the calorie bar's "large excess -> proportionally large danger zone" behavior.
        val result = growingProgressOf(current = 900.0, target = 90.0)

        assertEquals(1f, result.progress)
        assertApproximately(0.1f, result.normalEndFraction)
    }

    @Test
    fun `zero_target_does_not_crash_and_yields_zero_fractions`() {
        val result = growingProgressOf(current = 0.0, target = 0.0)

        assertEquals(0f, result.progress)
        assertEquals(0f, result.normalEndFraction)

        val resultWithCurrent = growingProgressOf(current = 5.0, target = 0.0)

        assertEquals(0f, resultWithCurrent.progress)
        assertEquals(0f, resultWithCurrent.normalEndFraction)
    }

    @Test
    fun `zero_current_yields_zero_progress_and_full_normal_fraction`() {
        val result = growingProgressOf(current = 0.0, target = 90.0)

        assertEquals(0f, result.progress)
        assertEquals(1f, result.normalEndFraction)
    }

    private fun assertApproximately(expected: Float, actual: Float, tolerance: Float = 0.001f) {
        val diff = kotlin.math.abs(expected - actual)
        assert(diff <= tolerance) { "Expected $expected but was $actual (diff $diff > $tolerance)" }
    }
}
