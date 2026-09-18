package com.maksimowiczm.foodyou.app.ui.home.goals

import kotlin.math.abs

/** Visual state of the daily calorie number, derived from how far [CalorieSummary.consumed] is past the target. */
internal enum class CalorieState {
    NORMAL,
    OVER_LIMIT,
}

/**
 * Result of comparing calories consumed against the daily target, with an `allowedDifference`
 * tolerance band around the target that keeps small overages from immediately looking alarming.
 *
 * Three independent thresholds drive the visuals:
 * - [isExcess] flips the "Remaining"/"Excess" label at `target`.
 * - [normalEndFraction]/[warningEndFraction] mark where the progress bar's fill switches from
 *   normal to a darker "tolerance" tint (`target - allowedDifference`) and then to the danger/red
 *   tint (`target + allowedDifference`).
 * - [state] flips the calorie number's color at `target + allowedDifference`.
 *
 * @param remainingOrExcess Always non-negative: the number of calories still available today
 *   (when not [isExcess]) or consumed beyond the target (when [isExcess]).
 * @param progress The main calorie progress bar's fill fraction, in `0f..1f`, relative to a bar
 *   whose 100%-width mark grows past `target + 2 * allowedDifference` once [consumed] exceeds it —
 *   so a very large excess is shown as a large proportion of red, rather than maxing the bar out.
 * @param normalEndFraction Fraction (of the bar's current width) where the normal/blue fill ends
 *   and the darker tolerance tint begins, i.e. `target - allowedDifference`. Shrinks as the bar
 *   grows past `target + 2 * allowedDifference`.
 * @param warningEndFraction Fraction (of the bar's current width) where the tolerance tint ends
 *   and the danger/red tint begins, i.e. `target + allowedDifference`. Shrinks as the bar grows
 *   past `target + 2 * allowedDifference`.
 * @param targetFraction Fraction (of the bar's current width) marking the exact `target`, for
 *   drawing a reference line on the progress bar. Shrinks as the bar grows past
 *   `target + 2 * allowedDifference`.
 */
internal data class CalorieSummary(
    val target: Int,
    val consumed: Int,
    val remainingOrExcess: Int,
    val isExcess: Boolean,
    val state: CalorieState,
    val progress: Float,
    val normalEndFraction: Float,
    val warningEndFraction: Float,
    val targetFraction: Float,
)

/**
 * Computes the [CalorieSummary] for a given [target] and [consumed] amount of calories, with an
 * [allowedDifference] (kcal) tolerance band around the target.
 *
 * Consuming exactly the target (`consumed == target`) is treated as not-excess, matching the
 * existing convention that hitting the target exactly isn't a problem.
 *
 * The bar's 100%-width mark ("barMax") is normally `target + 2 * allowedDifference`, but grows to
 * match [consumed] whenever it's exceeded — this way a very large excess is represented as a
 * growing proportion of the (still fully-filled) bar turning red, instead of the bar simply
 * capping out at 100% red regardless of how large the excess actually is.
 */
internal fun calorieSummaryOf(target: Int, consumed: Int, allowedDifference: Int): CalorieSummary {
    val isExcess = consumed > target
    val remainingOrExcess = abs(consumed - target)
    val state =
        if (consumed > target + allowedDifference) {
            CalorieState.OVER_LIMIT
        } else {
            CalorieState.NORMAL
        }

    val fixedMax = (target + 2 * allowedDifference).coerceAtLeast(1)
    val barMax = maxOf(fixedMax, consumed).coerceAtLeast(1)

    val progress = if (target <= 0) 0f else (consumed.toFloat() / barMax).coerceIn(0f, 1f)
    val normalEndFraction =
        if (target <= 0) {
            0f
        } else {
            ((target - allowedDifference).toFloat() / barMax).coerceIn(0f, 1f)
        }
    val warningEndFraction =
        if (target <= 0) {
            0f
        } else {
            ((target + allowedDifference).toFloat() / barMax).coerceIn(0f, 1f)
        }
    val targetFraction =
        if (target <= 0) {
            0f
        } else {
            (target.toFloat() / barMax).coerceIn(0f, 1f)
        }

    return CalorieSummary(
        target = target,
        consumed = consumed,
        remainingOrExcess = remainingOrExcess,
        isExcess = isExcess,
        state = state,
        progress = progress,
        normalEndFraction = normalEndFraction,
        warningEndFraction = warningEndFraction,
        targetFraction = targetFraction,
    )
}

/** The fill fraction, in `0f..1f`, of a single nutrient's progress bar. */
internal fun nutrientProgress(current: Int, target: Int): Float =
    if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)

/** Whether a nutrient's consumption has gone over its daily target. */
internal fun isNutrientExceeded(current: Int, target: Int): Boolean = target > 0 && current > target
