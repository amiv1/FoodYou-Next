package com.maksimowiczm.foodyou.app.ui.goals.master

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.maksimowiczm.foodyou.app.ui.common.utility.LocalEnergyFormatter
import com.maksimowiczm.foodyou.app.ui.home.goals.CalorieProgressIndicator
import com.maksimowiczm.foodyou.app.ui.home.goals.calorieSummaryOf
import com.maksimowiczm.foodyou.common.compose.component.GrowingProgress
import com.maksimowiczm.foodyou.common.compose.component.SimpleProgressIndicator
import com.maksimowiczm.foodyou.common.compose.utility.formatClipZeros
import com.maksimowiczm.foodyou.common.domain.food.NutrientValue
import com.maksimowiczm.foodyou.common.domain.food.NutritionFactsField
import foodyou.app.generated.resources.*
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NutrientGoal(
    field: NutritionFactsField,
    value: NutrientValue,
    target: Double,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    suffix: String = stringResource(Res.string.unit_gram_short),
) {
    NutrientGoal(
        label = field.stringResource(),
        target =
            NutrientGoalDefaults.simpleTargetString(
                value = value,
                target = target,
                color = color,
                suffix = suffix,
            ),
        color = color,
        state = rememberNutrientGoalState(value, target),
        modifier = modifier,
    )
}

@Composable
internal fun NutrientGoal(
    label: String,
    target: AnnotatedString,
    color: Color,
    state: NutrientGoalState,
    modifier: Modifier = Modifier,
) {
    NutrientGoal(
        label = {
            val color = if (state.isExceeded) MaterialTheme.colorScheme.error else color

            Text(text = label, style = LocalTextStyle.current.copy(color = color))
        },
        value = { Text(target) },
        progressColor = color.copy(alpha = .9f),
        state = state,
        modifier = modifier,
    )
}

@Composable
internal fun NutrientGoal(
    label: @Composable () -> Unit,
    value: @Composable () -> Unit,
    progressColor: Color,
    state: NutrientGoalState,
    modifier: Modifier = Modifier,
) {
    val progress by
        animateFloatAsState(
            targetValue = state.progress,
            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        )
    val progressBarColor by
        animateColorAsState(if (state.isExceeded) MaterialTheme.colorScheme.error else progressColor)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) { label() }
            value()
        }
        SimpleProgressIndicator(
            progress = progress,
            color = progressBarColor,
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
    }
}

/**
 * The Energy row's goal display, using the same [CalorieProgressIndicator] bar (tolerance-band
 * tint + target-line marker) as the Home screen's Goals card, instead of the plain two-tone bar
 * used by every other [NutrientGoal] row.
 */
@Composable
internal fun EnergyGoal(
    value: NutrientValue,
    target: Double,
    calorieAllowedDifference: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val state = rememberNutrientGoalState(value, target)

    val calorieSummary =
        remember(value, target, calorieAllowedDifference) {
            calorieSummaryOf(
                target = target.roundToInt(),
                consumed = (value.value ?: 0.0).roundToInt(),
                allowedDifference = calorieAllowedDifference,
            )
        }

    val progress by
        animateFloatAsState(
            targetValue = calorieSummary.progress,
            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                val labelColor = if (state.isExceeded) MaterialTheme.colorScheme.error else color

                Text(
                    text = stringResource(Res.string.unit_energy),
                    style = LocalTextStyle.current.copy(color = labelColor),
                )
            }
            Text(
                NutrientGoalDefaults.energyTargetString(value = value, target = target, color = color)
            )
        }
        CalorieProgressIndicator(
            progress = progress,
            normalEndFraction = calorieSummary.normalEndFraction,
            warningEndFraction = calorieSummary.warningEndFraction,
            targetFraction = calorieSummary.targetFraction,
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
    }
}

@Composable
internal fun rememberNutrientGoalState(value: NutrientValue, target: Double): NutrientGoalState =
    remember(value, target) { NutrientGoalState(value = value.value ?: 0.0, target = target) }

@Immutable
internal class NutrientGoalState(val value: Double, val target: Double) {
    val isExceeded: Boolean
        get() = value > target

    val progress: Float
        get() = if (target == 0.0) 0f else (value / target).toFloat().coerceIn(0f, 1f)
}

internal object NutrientGoalDefaults {

    @Composable
    fun energyTargetString(
        value: NutrientValue,
        target: Double,
        color: Color = MaterialTheme.colorScheme.primary,
    ): AnnotatedString {
        val energyFormatter = LocalEnergyFormatter.current

        val value = value.value ?: 0.0
        val isExceeded = value > target
        val colorScheme = MaterialTheme.colorScheme
        val localStyle = LocalTextStyle.current

        return buildAnnotatedString {
            withStyle(
                localStyle.copy(color = if (isExceeded) colorScheme.error else color).toSpanStyle()
            ) {
                append(energyFormatter.formatEnergy(value, withSuffix = false))
            }
            withStyle(localStyle.copy(color = colorScheme.outline).toSpanStyle()) {
                append(" / ")
                append(energyFormatter.formatEnergy(target, withSuffix = true))
            }
        }
    }

    @Composable
    fun simpleTargetString(
        value: NutrientValue,
        target: Double,
        color: Color,
        suffix: String,
    ): AnnotatedString {
        val isComplete = value.isComplete
        val value = value.value ?: 0.0
        val isExceeded = value > target
        val colorScheme = MaterialTheme.colorScheme
        val localStyle = LocalTextStyle.current

        return remember(
            value,
            target,
            color,
            suffix,
            localStyle,
            colorScheme,
            isExceeded,
            isComplete,
        ) {
            buildAnnotatedString {
                withStyle(
                    localStyle
                        .copy(color = if (isExceeded) colorScheme.error else color)
                        .toSpanStyle()
                ) {
                    if (!isComplete) {
                        append("* ")
                    }
                    append(value.formatClipZeros())
                }
                withStyle(localStyle.copy(color = colorScheme.outline).toSpanStyle()) {
                    append(" / ")
                    append(target.formatClipZeros())
                    append(" ")
                    append(suffix)
                }
            }
        }
    }
}
