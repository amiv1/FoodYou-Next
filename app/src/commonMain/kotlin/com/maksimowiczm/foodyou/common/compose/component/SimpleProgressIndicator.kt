package com.maksimowiczm.foodyou.common.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Width of the border delineating the track from its surrounding background. */
private val TrackBorderWidth = 1.dp

/**
 * A simple, continuous capsule-shaped progress bar: a track with a single fill on top, clipped to
 * [progress] of the available width. Unlike Material3's `LinearProgressIndicator`, there is no gap
 * between the fill and the track, and no stop indicator — [progress] is expected to already be
 * coerced to the `0f..1f` range by the caller (e.g. clamped to `1f` when a value exceeds its
 * target, instead of wrapping past 100%).
 *
 * When both [dangerColor] and [dangerStartFraction] are non-null, the fill is rendered as two
 * overlapping layers instead of one: a full-width-of-[progress] [dangerColor] layer underneath,
 * overwritten by a narrower [color] layer clipped to `min(progress, dangerStartFraction)` on top —
 * so only the portion of [progress] past [dangerStartFraction] remains visibly [dangerColor]. This
 * is used to show only the *excess* part of an over-target value in a danger color, rather than
 * flipping the bar's entire fill to that color (see `growingProgressOf`, which computes a
 * [dangerStartFraction] that shrinks as an over-target bar's max grows to accommodate the excess).
 */
@Composable
fun SimpleProgressIndicator(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    dangerColor: Color? = null,
    dangerStartFraction: Float? = null,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val trackBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)

    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(trackColor)
                .border(TrackBorderWidth, trackBorderColor, CircleShape)
    ) {
        if (dangerColor != null && dangerStartFraction != null) {
            // Danger fill (widest, drawn first/underneath).
            Box(
                modifier =
                    Modifier.fillMaxWidth(clampedProgress).fillMaxHeight().background(dangerColor)
            )

            // Normal fill (narrower, drawn on top) — overwrites the danger fill up to the target.
            Box(
                modifier =
                    Modifier.fillMaxWidth(clampedProgress.coerceAtMost(dangerStartFraction))
                        .fillMaxHeight()
                        .background(color)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(clampedProgress).fillMaxHeight().background(color)
            )
        }
    }
}
