package com.maksimowiczm.foodyou.app.ui.home.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.app.ui.common.theme.LocalNutrientsPalette
import com.maksimowiczm.foodyou.app.ui.common.utility.LocalEnergyFormatter
import com.maksimowiczm.foodyou.app.ui.common.utility.LocalNutrientsOrder
import com.maksimowiczm.foodyou.app.ui.home.shared.FoodYouHomeCard
import com.maksimowiczm.foodyou.common.compose.component.SimpleProgressIndicator
import com.maksimowiczm.foodyou.settings.domain.entity.NutrientsOrder
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.shimmer
import foodyou.app.generated.resources.*
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun GoalsCard(
    date: LocalDate,
    shimmer: Shimmer,
    onClick: (epochDay: Long) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalsViewModel = koinViewModel(key = "goals-${date.toEpochDays()}"),
) {
    LaunchedEffect(date) { viewModel.setDate(date) }

    val model = viewModel.model.collectAsStateWithLifecycle().value

    if (model == null) {
        GoalsCardSkeleton(
            shimmer = shimmer,
            onClick = { onClick(date.toEpochDays()) },
            onLongClick = onLongClick,
            modifier = modifier,
        )
    } else {
        GoalsCard(
            energy = model.energy,
            energyGoal = model.energyGoal,
            proteins = model.proteins,
            proteinsGoal = model.proteinsGoal,
            carbohydrates = model.carbohydrates,
            carbohydratesGoal = model.carbohydratesGoal,
            fats = model.fats,
            fatsGoal = model.fatsGoal,
            onClick = { onClick(date.toEpochDays()) },
            onLongClick = onLongClick,
            modifier = modifier,
        )
    }
}

@Composable
internal fun GoalsCard(
    energy: Int,
    energyGoal: Int,
    proteins: Int,
    proteinsGoal: Int,
    carbohydrates: Int,
    carbohydratesGoal: Int,
    fats: Int,
    fatsGoal: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val calorieSummary = remember(energy, energyGoal) { calorieSummaryOf(energyGoal, energy) }

    val calorieProgress =
        animateFloatAsState(
                targetValue = calorieSummary.progress,
                animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
            )
            .value

    FoodYouHomeCard(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CalorieSummaryRow(calorieSummary = calorieSummary, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(8.dp))

            SimpleProgressIndicator(
                progress = calorieProgress,
                color =
                    when (calorieSummary.state) {
                        CalorieState.NORMAL -> MaterialTheme.colorScheme.primary
                        CalorieState.OVER_LIMIT -> MaterialTheme.colorScheme.error
                    },
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )

            Spacer(Modifier.height(16.dp))

            NutrientRow(
                proteins = proteins,
                proteinsGoal = proteinsGoal,
                carbohydrates = carbohydrates,
                carbohydratesGoal = carbohydratesGoal,
                fats = fats,
                fatsGoal = fatsGoal,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CalorieSummaryRow(calorieSummary: CalorieSummary, modifier: Modifier = Modifier) {
    val energyFormatter = LocalEnergyFormatter.current

    val remainingOrExcessLabel =
        when (calorieSummary.state) {
            CalorieState.NORMAL -> stringResource(Res.string.headline_calories_remaining)
            CalorieState.OVER_LIMIT -> stringResource(Res.string.headline_calories_excess)
        }

    val remainingOrExcessColor =
        when (calorieSummary.state) {
            CalorieState.NORMAL -> MaterialTheme.colorScheme.primary
            CalorieState.OVER_LIMIT -> MaterialTheme.colorScheme.error
        }

    Row(modifier = modifier) {
        CalorieSummaryColumn(
            label = stringResource(Res.string.headline_calorie_target),
            value = energyFormatter.formatEnergy(calorieSummary.target, withSuffix = false),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        CalorieSummaryColumn(
            label = stringResource(Res.string.headline_calories_consumed),
            value = energyFormatter.formatEnergy(calorieSummary.consumed, withSuffix = false),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )

        CalorieSummaryColumn(
            label = remainingOrExcessLabel,
            value =
                energyFormatter.formatEnergy(calorieSummary.remainingOrExcess, withSuffix = false),
            color = remainingOrExcessColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CalorieSummaryColumn(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NutrientRow(
    proteins: Int,
    proteinsGoal: Int,
    carbohydrates: Int,
    carbohydratesGoal: Int,
    fats: Int,
    fatsGoal: Int,
    modifier: Modifier = Modifier,
) {
    val nutrientsPalette = LocalNutrientsPalette.current
    val nutrientsOrder = LocalNutrientsOrder.current
    val gramShort = stringResource(Res.string.unit_gram_short)

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        nutrientsOrder.forEach { field ->
            when (field) {
                NutrientsOrder.Proteins ->
                    NutrientColumn(
                        label = stringResource(Res.string.nutriment_proteins),
                        current = proteins,
                        target = proteinsGoal,
                        unit = gramShort,
                        color = nutrientsPalette.proteinsOnSurfaceContainer,
                        modifier = Modifier.weight(1f),
                    )

                NutrientsOrder.Fats ->
                    NutrientColumn(
                        label = stringResource(Res.string.nutriment_fats),
                        current = fats,
                        target = fatsGoal,
                        unit = gramShort,
                        color = nutrientsPalette.fatsOnSurfaceContainer,
                        modifier = Modifier.weight(1f),
                    )

                NutrientsOrder.Carbohydrates ->
                    NutrientColumn(
                        label = stringResource(Res.string.nutriment_carbohydrates),
                        current = carbohydrates,
                        target = carbohydratesGoal,
                        unit = gramShort,
                        color = nutrientsPalette.carbohydratesOnSurfaceContainer,
                        modifier = Modifier.weight(1f),
                    )

                NutrientsOrder.Other,
                NutrientsOrder.Vitamins,
                NutrientsOrder.Minerals -> Unit
            }
        }
    }
}

@Composable
private fun NutrientColumn(
    label: String,
    current: Int,
    target: Int,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val exceeded = isNutrientExceeded(current, target)
    val progress =
        animateFloatAsState(
                targetValue = nutrientProgress(current, target),
                animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
            )
            .value

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "$current",
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = stringResource(Res.string.neutral_of_target, "$target $unit"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(6.dp))

        SimpleProgressIndicator(
            progress = progress,
            color = if (exceeded) MaterialTheme.colorScheme.error else color,
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
    }
}

@Composable
private fun GoalsCardSkeleton(
    shimmer: Shimmer,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val blockColor = MaterialTheme.colorScheme.surfaceContainerHighest

    FoodYouHomeCard(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(3) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(
                            Modifier.shimmer(shimmer)
                                .size(40.dp, 14.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(blockColor)
                        )
                        Spacer(Modifier.height(6.dp))
                        Spacer(
                            Modifier.shimmer(shimmer)
                                .size(56.dp, 28.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(blockColor)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Spacer(
                Modifier.shimmer(shimmer)
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(blockColor)
            )

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(3) {
                    Column(modifier = Modifier.weight(1f)) {
                        Spacer(
                            Modifier.shimmer(shimmer)
                                .size(48.dp, 14.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(blockColor)
                        )
                        Spacer(Modifier.height(6.dp))
                        Spacer(
                            Modifier.shimmer(shimmer)
                                .size(32.dp, 22.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(blockColor)
                        )
                        Spacer(Modifier.height(4.dp))
                        Spacer(
                            Modifier.shimmer(shimmer)
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(blockColor)
                        )
                    }
                }
            }
        }
    }
}
