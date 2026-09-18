package com.maksimowiczm.foodyou.app.ui.home.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * A calorie-specific progress bar with three overlapping fill tiers (normal / tolerance / danger)
 * plus a gray marker for the unfilled tolerance zone, rendered widest-layer-first so each
 * narrower/bluer layer visually overwrites the wider one beneath it. See [CalorieSummary] for how
 * [normalEndFraction]/[warningEndFraction] are derived.
 *
 * Used by the Home screen Goals card ([GoalsCard]) and the full-screen Goals "Summary" view's
 * Energy row, so both places show consumed calories against the same tolerance-band/target-line
 * visualization.
 */
@Composable
internal fun CalorieProgressIndicator(
    progress: Float,
    normalEndFraction: Float,
    warningEndFraction: Float,
    targetFraction: Float,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val bandColor = MaterialTheme.colorScheme.outlineVariant
    val normalColor = MaterialTheme.colorScheme.primary
    val toleranceColor = remember(normalColor) { lerp(normalColor, Color.Black, 0.25f) }
    val dangerColor = MaterialTheme.colorScheme.error
    val targetMarkerColor = MaterialTheme.colorScheme.surfaceContainerLow

    val clampedProgress = progress.coerceIn(0f, 1f)
    val dangerWidth = clampedProgress
    val toleranceWidth = clampedProgress.coerceAtMost(warningEndFraction)
    val normalWidth = clampedProgress.coerceAtMost(normalEndFraction)

    val targetMarkerWidth = 2.dp

    BoxWithConstraints(modifier = modifier.clip(CircleShape).background(trackColor)) {
        if (warningEndFraction > normalEndFraction) {
            Box(
                modifier =
                    Modifier.offset(x = maxWidth * normalEndFraction)
                        .width(maxWidth * (warningEndFraction - normalEndFraction))
                        .fillMaxHeight()
                        .background(bandColor)
            )
        }

        // Danger/red fill (bottom, widest).
        Box(modifier = Modifier.fillMaxWidth(dangerWidth).fillMaxHeight().background(dangerColor))

        // Tolerance/dark-blue fill (middle), overwrites the danger fill for its range.
        Box(
            modifier =
                Modifier.fillMaxWidth(toleranceWidth).fillMaxHeight().background(toleranceColor)
        )

        // Normal/blue fill (top, narrowest), overwrites the tolerance fill for its range.
        Box(modifier = Modifier.fillMaxWidth(normalWidth).fillMaxHeight().background(normalColor))

        // Target reference line, drawn last/on top so it's visible regardless of fill color —
        // uses the card's own background color so it reads as a thin notch cut into the bar.
        val targetOffset =
            (maxWidth * targetFraction - targetMarkerWidth / 2).coerceIn(
                0.dp,
                maxWidth - targetMarkerWidth,
            )
        Box(
            modifier =
                Modifier.offset(x = targetOffset)
                    .width(targetMarkerWidth)
                    .fillMaxHeight()
                    .background(targetMarkerColor)
        )
    }
}
