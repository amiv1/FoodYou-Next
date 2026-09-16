package com.maksimowiczm.foodyou.common.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

/**
 * A simple, continuous capsule-shaped progress bar: a track with a single fill on top, clipped to
 * [progress] of the available width. Unlike Material3's `LinearProgressIndicator`, there is no gap
 * between the fill and the track, and no stop indicator — [progress] is expected to already be
 * coerced to the `0f..1f` range by the caller (e.g. clamped to `1f` when a value exceeds its
 * target, instead of wrapping past 100%).
 */
@Composable
fun SimpleProgressIndicator(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    Box(modifier = modifier.clip(CircleShape).background(trackColor)) {
        Box(
            modifier =
                Modifier.fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
        )
    }
}
