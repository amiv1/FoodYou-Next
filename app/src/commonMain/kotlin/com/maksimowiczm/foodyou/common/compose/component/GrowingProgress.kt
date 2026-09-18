package com.maksimowiczm.foodyou.common.compose.component

/**
 * Progress-bar fractions for a value that may exceed its target, where the bar's 100%-width mark
 * grows to match [GrowingProgress.progress]'s underlying `current` once exceeded — so a large
 * excess renders as a large red proportion instead of the bar simply capping out fully red.
 *
 * @param progress The overall bar fill fraction, in `0f..1f`. Always `1f` once `current >= target`
 *   (the bar's max grows to match `current`, so it's always exactly full when exceeded).
 * @param normalEndFraction The fraction (of the bar's current width) where the normal color ends
 *   and the danger/exceeded color begins, i.e. where `target` sits. Shrinks as the bar grows past
 *   `target` to accommodate a larger `current`.
 */
internal data class GrowingProgress(val progress: Float, val normalEndFraction: Float)

/**
 * Computes the [GrowingProgress] for a [current] value against its [target].
 *
 * While `current <= target`, this behaves like a normal `0f..1f` progress fraction. Once
 * `current > target`, the bar's max grows to match `current` (so [progress] stays `1f`, the bar
 * is always fully filled), while [normalEndFraction] shrinks proportionally — the natural,
 * generic effect being a growing "danger" proportion at the end of the bar as the excess grows,
 * without the bar simply capping out solid red regardless of how large the excess actually is.
 */
internal fun growingProgressOf(current: Double, target: Double): GrowingProgress {
    if (target <= 0.0) return GrowingProgress(progress = 0f, normalEndFraction = 0f)

    val barMax = maxOf(target, current)

    return GrowingProgress(
        progress = (current / barMax).toFloat().coerceIn(0f, 1f),
        normalEndFraction = (target / barMax).toFloat().coerceIn(0f, 1f),
    )
}
