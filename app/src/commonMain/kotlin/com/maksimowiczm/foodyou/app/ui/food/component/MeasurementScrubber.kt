package com.maksimowiczm.foodyou.app.ui.food.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maksimowiczm.foodyou.app.ui.common.utility.stringResource
import com.maksimowiczm.foodyou.common.compose.utility.formatClipZeros
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.common.domain.measurement.MeasurementType
import com.maksimowiczm.foodyou.common.domain.measurement.from
import com.maksimowiczm.foodyou.common.domain.measurement.rawValue
import com.maksimowiczm.foodyou.common.domain.measurement.type
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Displays a food [Measurement] as a measuring-tape-like ruler: drag it
 * horizontally to scrub the value, or tap the value above it to type an
 * exact number.
 *
 * The measurement's [MeasurementType] is preserved across edits; only its
 * raw quantity changes. Changes are reported through [onMeasurementChange]
 * once the drag gesture ends, or immediately when a typed value is
 * confirmed.
 */
@Composable
fun MeasurementScrubber(
    measurement: Measurement,
    onMeasurementChange: (Measurement) -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val type = measurement.type
    val step = type.scrubStep()
    val majorEvery = type.scrubMajorTickEvery()

    // Continuous local value so the ruler glides smoothly while dragging,
    // even before the persisted change flows back down through the entry.
    // It's intentionally NOT keyed on `measurement` (which changes identity
    // once the persisted update flows back), otherwise the still-running
    // drag gesture coroutine below (whose keys don't include `measurement`)
    // would keep mutating a now-orphaned state object invisible to the UI.
    var continuousValue by remember { mutableStateOf(measurement.rawValue) }
    var isEditing by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var lastHapticTick by remember { mutableStateOf(tickIndexOf(measurement.rawValue, step)) }

    val displayValue by
        remember(step) { derivedStateOf { snapToStep(continuousValue, step) } }

    // Keep in sync with externally-driven changes (initial load, or edits
    // made elsewhere), but don't fight the in-progress drag/local edit.
    LaunchedEffect(measurement) {
        if (!isDragging && !isEditing) {
            continuousValue = measurement.rawValue
            lastHapticTick = tickIndexOf(measurement.rawValue, step)
        }
    }

    val hapticFeedback = LocalHapticFeedback.current
    val latestOnMeasurementChange = rememberUpdatedState(onMeasurementChange)
    val coroutineScope = rememberCoroutineScope()
    var flingJob by remember { mutableStateOf<Job?>(null) }

    fun commit(newValue: Double) {
        val snapped = snapToStep(newValue, step)
        continuousValue = snapped
        latestOnMeasurementChange.value(Measurement.from(type, snapped))
    }

    fun tick(newValue: Double) {
        val index = tickIndexOf(newValue, step)
        if (index != lastHapticTick) {
            lastHapticTick = index
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        if (isEditing) {
            EditableMeasurementValue(
                initialValue = displayValue,
                onConfirm = { newValue ->
                    isEditing = false
                    if (newValue != null && newValue > 0.0) {
                        continuousValue = newValue
                        lastHapticTick = tickIndexOf(newValue, step)
                        latestOnMeasurementChange.value(Measurement.from(type, newValue))
                    }
                },
            )
        } else {
            Text(
                text = displayValue.formatClipZeros() + " " + type.stringResource(),
                style = MaterialTheme.typography.headlineSmall,
                modifier =
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isEditing = true },
                    ),
            )
        }

        Spacer(Modifier.height(4.dp))

        Ruler(
            value = continuousValue,
            step = step,
            majorEvery = majorEvery,
            onDrag = { deltaValue ->
                continuousValue = (continuousValue + deltaValue).coerceAtLeast(step / 2)
                tick(continuousValue)
            },
            onDragStart = {
                // A new touch cancels any ongoing fling so the finger takes
                // over control immediately.
                flingJob?.cancel()
                isDragging = true
            },
            onDragEnd = { valueVelocity ->
                if (abs(valueVelocity) < step * MIN_FLING_VALUE_PER_SECOND) {
                    isDragging = false
                    commit(continuousValue)
                } else {
                    flingJob =
                        coroutineScope.launch {
                            val fling = Animatable(continuousValue.toFloat())
                            fling.updateBounds(lowerBound = (step / 2).toFloat())
                            try {
                                fling.animateDecay(
                                    initialVelocity = valueVelocity.toFloat(),
                                    animationSpec = exponentialDecay(frictionMultiplier = 6f),
                                ) {
                                    continuousValue = value.toDouble()
                                    tick(continuousValue)
                                }
                            } finally {
                                // If cancelled (a new touch started), let that
                                // gesture own committing the final value instead.
                                if (isActive) {
                                    isDragging = false
                                    commit(continuousValue)
                                }
                            }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (caption != null) {
            Spacer(Modifier.height(4.dp))
            Text(text = caption, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun tickIndexOf(value: Double, step: Double): Long = (value / step).roundToLong()

private fun snapToStep(value: Double, step: Double): Double =
    (tickIndexOf(value, step) * step).coerceAtLeast(step)

/**
 * A horizontally scrolling ruler: tick marks glide under a fixed center
 * indicator as [value] changes. [onDrag] is called with the drag delta
 * already converted to a value delta (same unit as [value]/[step]).
 * [onDragEnd] receives the release velocity, also converted to
 * value-per-second, so callers can continue the motion as a fling.
 */
@Composable
private fun Ruler(
    value: Double,
    step: Double,
    majorEvery: Int,
    onDrag: (Double) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: (valueVelocityPerSecond: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val indicatorColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall

    val pxPerTick = with(density) { TICK_SPACING.toPx() }
    val minorTickHeightPx = with(density) { MINOR_TICK_HEIGHT.toPx() }
    val majorTickHeightPx = with(density) { MAJOR_TICK_HEIGHT.toPx() }
    val topInsetPx = with(density) { TOP_INSET.toPx() }
    val labelGapPx = with(density) { LABEL_GAP.toPx() }

    Canvas(
        modifier =
            modifier.height(RULER_HEIGHT).pointerInput(pxPerTick, step) {
                val velocityTracker = VelocityTracker()
                detectHorizontalDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                        onDragStart()
                    },
                    onDragEnd = {
                        val velocityPxPerSecond = velocityTracker.calculateVelocity().x
                        // Same sign convention as onDrag: leftward motion
                        // (negative px velocity) increases the value.
                        onDragEnd(-(velocityPxPerSecond / pxPerTick) * step)
                    },
                    onDragCancel = { onDragEnd(0.0) },
                ) { change, dragAmount ->
                    change.consume()
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    // Dragging left reveals larger values at the center,
                    // mirroring how a physical measuring tape is pulled.
                    onDrag(-(dragAmount / pxPerTick) * step)
                }
            }
    ) {
        val centerX = size.width / 2f
        val centerTick = value / step

        drawIndicator(
            centerX = centerX,
            topInsetPx = topInsetPx,
            indicatorLengthPx = topInsetPx + majorTickHeightPx,
            color = indicatorColor,
        )

        val halfTicksVisible = ceil((size.width / 2f) / pxPerTick).toLong() + 1
        val startTick = floor(centerTick - halfTicksVisible).toLong()
        val endTick = ceil(centerTick + halfTicksVisible).toLong()

        for (tick in startTick..endTick) {
            if (tick < 0) continue

            val x = centerX + ((tick - centerTick) * pxPerTick).toFloat()
            val isMajor = majorEvery > 0 && tick % majorEvery == 0L
            val tickHeightPx = if (isMajor) majorTickHeightPx else minorTickHeightPx

            drawLine(
                color = tickColor.copy(alpha = if (isMajor) 0.9f else 0.5f),
                start = Offset(x, topInsetPx),
                end = Offset(x, topInsetPx + tickHeightPx),
                strokeWidth = if (isMajor) 3f else 1.5f,
            )

            if (isMajor) {
                val tickValue = tick * step
                val label = tickValue.formatClipZeros()
                val layout = textMeasurer.measure(label, style = labelStyle.copy(color = labelColor))

                drawText(
                    textLayoutResult = layout,
                    topLeft =
                        Offset(
                            x - layout.size.width / 2f,
                            topInsetPx + majorTickHeightPx + labelGapPx,
                        ),
                )
            }
        }
    }
}

private fun DrawScope.drawIndicator(
    centerX: Float,
    topInsetPx: Float,
    indicatorLengthPx: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    drawLine(
        color = color,
        start = Offset(centerX, 0f),
        end = Offset(centerX, indicatorLengthPx),
        strokeWidth = 4f,
    )

    val caretHalfWidth = topInsetPx * 0.4f
    val path =
        Path().apply {
            moveTo(centerX - caretHalfWidth, 0f)
            lineTo(centerX + caretHalfWidth, 0f)
            lineTo(centerX, topInsetPx * 0.7f)
            close()
        }
    drawPath(path = path, color = color)
}

@Composable
private fun EditableMeasurementValue(
    initialValue: Double,
    onConfirm: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialText = remember(initialValue) { initialValue.formatClipZeros() }
    val textFieldState =
        rememberTextFieldState(initialText, initialSelection = TextRange(0, initialText.length))
    val focusRequester = remember { FocusRequester() }
    var confirmed by remember { mutableStateOf(false) }
    var hasBeenFocused by remember { mutableStateOf(false) }

    val confirm = {
        if (!confirmed) {
            confirmed = true
            val value = textFieldState.text.toString().replace(',', '.').toDoubleOrNull()
            onConfirm(value)
        }
    }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    BasicTextField(
        state = textFieldState,
        keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        onKeyboardAction = { confirm() },
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle =
            LocalTextStyle.current
                .merge(MaterialTheme.typography.headlineSmall)
                .merge(LocalContentColor.current)
                .copy(textAlign = TextAlign.Center),
        modifier =
            modifier.width(96.dp).focusRequester(focusRequester).onFocusChanged { focusState ->
                // The very first callback fires with isFocused == false,
                // before requestFocus() above has taken effect. Only treat
                // a loss of focus as "blur" once it has actually been
                // focused at least once, otherwise editing confirms and
                // closes itself instantly.
                if (focusState.isFocused) {
                    hasBeenFocused = true
                } else if (hasBeenFocused) {
                    confirm()
                }
            },
        decorator = { inner -> Box(contentAlignment = Alignment.Center) { inner() } },
    )
}

private fun MeasurementType.scrubStep(): Double =
    when (this) {
        MeasurementType.Gram,
        MeasurementType.Milliliter -> 1.0

        MeasurementType.Ounce,
        MeasurementType.FluidOunce -> 0.1

        MeasurementType.Serving,
        MeasurementType.Package -> 0.25
    }

/** Number of minor ticks between two labeled major ticks. */
private fun MeasurementType.scrubMajorTickEvery(): Int =
    when (this) {
        MeasurementType.Gram,
        MeasurementType.Milliliter -> 10

        MeasurementType.Ounce,
        MeasurementType.FluidOunce -> 10

        MeasurementType.Serving,
        MeasurementType.Package -> 4
    }

private val TICK_SPACING = 10.dp
private val MINOR_TICK_HEIGHT = 14.dp
private val MAJOR_TICK_HEIGHT = 26.dp
private val TOP_INSET = 10.dp
private val LABEL_GAP = 4.dp
private val RULER_HEIGHT = 64.dp

/**
 * Minimum release speed (in steps per second) required to trigger a fling
 * instead of just snapping to the nearest step. Keeps a slow, deliberate
 * drag from unexpectedly gliding past the intended value.
 */
private const val MIN_FLING_VALUE_PER_SECOND = 3.0
