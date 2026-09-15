package com.maksimowiczm.foodyou.app.ui.home.goals

/** Visual state of the daily calorie summary, derived purely from [CalorieSummary.remainingOrExcess]. */
internal enum class CalorieState {
    NORMAL,
    OVER_LIMIT,
}

/**
 * Result of comparing calories consumed against the daily target.
 *
 * @param remainingOrExcess Always non-negative. When [state] is [CalorieState.NORMAL] it's the
 *   number of calories still available today; when [CalorieState.OVER_LIMIT] it's the number of
 *   calories consumed beyond the target.
 * @param progress The main calorie progress bar's fill fraction, in `0f..1f`.
 */
internal data class CalorieSummary(
    val target: Int,
    val consumed: Int,
    val remainingOrExcess: Int,
    val state: CalorieState,
    val progress: Float,
)

/**
 * Computes the [CalorieSummary] for a given [target] and [consumed] amount of calories.
 *
 * Consuming exactly the target (`consumed == target`) is treated as [CalorieState.NORMAL] with
 * zero calories remaining, not as exceeding the limit.
 */
internal fun calorieSummaryOf(target: Int, consumed: Int): CalorieSummary {
    val remaining = target - consumed

    return if (remaining >= 0) {
        CalorieSummary(
            target = target,
            consumed = consumed,
            remainingOrExcess = remaining,
            state = CalorieState.NORMAL,
            progress = if (target <= 0) 0f else (consumed.toFloat() / target).coerceIn(0f, 1f),
        )
    } else {
        CalorieSummary(
            target = target,
            consumed = consumed,
            remainingOrExcess = -remaining,
            state = CalorieState.OVER_LIMIT,
            progress = 1f,
        )
    }
}

/** The fill fraction, in `0f..1f`, of a single nutrient's progress bar. */
internal fun nutrientProgress(current: Int, target: Int): Float =
    if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)

/** Whether a nutrient's consumption has gone over its daily target. */
internal fun isNutrientExceeded(current: Int, target: Int): Boolean = target > 0 && current > target
