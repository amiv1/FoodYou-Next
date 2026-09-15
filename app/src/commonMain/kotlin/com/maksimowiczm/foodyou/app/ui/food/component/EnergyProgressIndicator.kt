package com.maksimowiczm.foodyou.app.ui.food.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.maksimowiczm.foodyou.app.ui.common.theme.LocalNutrientsPalette
import com.maksimowiczm.foodyou.app.ui.common.utility.LocalNutrientsOrder
import com.maksimowiczm.foodyou.common.compose.component.MultiColorProgressIndicator
import com.maksimowiczm.foodyou.common.compose.component.MultiColorProgressIndicatorItem
import com.maksimowiczm.foodyou.settings.domain.entity.NutrientsOrder
import foodyou.app.generated.resources.*
import kotlin.math.max
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

/** Indicator with goal. */
@Composable
fun EnergyProgressIndicator(
    calories: Float,
    proteins: Float,
    carbohydrates: Float,
    fats: Float,
    goal: Float,
    modifier: Modifier = Modifier,
) {
    val nutrientsPalette = LocalNutrientsPalette.current
    val order = LocalNutrientsOrder.current

    val animatedProteins by animateFloatAsState(proteins)
    val animatedCarbohydrates by animateFloatAsState(carbohydrates)
    val animatedFats by animateFloatAsState(fats)

    val max = max(goal, calories)
    val animatedMax by animateFloatAsState(max)

    MultiColorProgressIndicator(
        items =
            order.mapNotNull {
                when (it) {
                    NutrientsOrder.Proteins ->
                        MultiColorProgressIndicatorItem(
                            progress = animatedProteins / animatedMax,
                            color = nutrientsPalette.proteinsOnSurfaceContainer,
                        )

                    NutrientsOrder.Carbohydrates ->
                        MultiColorProgressIndicatorItem(
                            progress = animatedCarbohydrates / animatedMax,
                            color = nutrientsPalette.carbohydratesOnSurfaceContainer,
                        )

                    NutrientsOrder.Fats ->
                        MultiColorProgressIndicatorItem(
                            progress = animatedFats / animatedMax,
                            color = nutrientsPalette.fatsOnSurfaceContainer,
                        )

                    else -> null
                }
            },
        modifier =
            modifier
                .defaultMinSize(minWidth = 0.dp, minHeight = 16.dp)
                .clip(MaterialTheme.shapes.small),
    )
}

/** Indicator without goal. */
@Composable
fun EnergyProgressIndicator(
    proteins: Float,
    carbohydrates: Float,
    fats: Float,
    modifier: Modifier = Modifier,
) {
    val nutrientsPalette = LocalNutrientsPalette.current
    val order = LocalNutrientsOrder.current

    val sum = proteins + carbohydrates + fats
    val items =
        remember(order, sum, proteins, carbohydrates, fats) {
            order.mapNotNull {
                when (it) {
                    NutrientsOrder.Proteins ->
                        MultiColorProgressIndicatorItem(
                            progress = proteins / sum,
                            color = nutrientsPalette.proteinsOnSurfaceContainer,
                        )

                    NutrientsOrder.Carbohydrates ->
                        MultiColorProgressIndicatorItem(
                            progress = carbohydrates / sum,
                            color = nutrientsPalette.carbohydratesOnSurfaceContainer,
                        )

                    NutrientsOrder.Fats ->
                        MultiColorProgressIndicatorItem(
                            progress = fats / sum,
                            color = nutrientsPalette.fatsOnSurfaceContainer,
                        )

                    else -> null
                }
            }
        }

    MultiColorProgressIndicator(
        items = items,
        modifier =
            modifier
                .defaultMinSize(minWidth = 0.dp, minHeight = 16.dp)
                .clip(MaterialTheme.shapes.small),
    )
}

private data class MacroDistributionItem(
    val label: String,
    val color: androidx.compose.ui.graphics.Color,
    val progress: Float,
)

/**
 * A one-line legend (color dot + label per macro) above the [EnergyProgressIndicator] bar, and
 * each macro's share as a percentage below the bar, aligned under its corresponding color
 * segment.
 */
@Composable
fun MacroDistributionIndicator(
    proteins: Float,
    carbohydrates: Float,
    fats: Float,
    modifier: Modifier = Modifier,
) {
    val nutrientsPalette = LocalNutrientsPalette.current
    val order = LocalNutrientsOrder.current

    val proteinsLabel = stringResource(Res.string.nutriment_proteins)
    val carbohydratesLabel = stringResource(Res.string.nutriment_carbohydrates)
    val fatsLabel = stringResource(Res.string.nutriment_fats)

    val sum = proteins + carbohydrates + fats

    val items =
        remember(order, sum, proteins, carbohydrates, fats, proteinsLabel, carbohydratesLabel, fatsLabel) {
            order.mapNotNull {
                when (it) {
                    NutrientsOrder.Proteins ->
                        MacroDistributionItem(
                            label = proteinsLabel,
                            color = nutrientsPalette.proteinsOnSurfaceContainer,
                            progress = if (sum > 0f) proteins / sum else 0f,
                        )

                    NutrientsOrder.Carbohydrates ->
                        MacroDistributionItem(
                            label = carbohydratesLabel,
                            color = nutrientsPalette.carbohydratesOnSurfaceContainer,
                            progress = if (sum > 0f) carbohydrates / sum else 0f,
                        )

                    NutrientsOrder.Fats ->
                        MacroDistributionItem(
                            label = fatsLabel,
                            color = nutrientsPalette.fatsOnSurfaceContainer,
                            progress = if (sum > 0f) fats / sum else 0f,
                        )

                    else -> null
                }
            }
        }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier =
                            Modifier.size(8.dp).clip(CircleShape).background(item.color)
                    )
                    Text(text = item.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        EnergyProgressIndicator(
            proteins = proteins,
            carbohydrates = carbohydrates,
            fats = fats,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
            items.forEach { item ->
                Box(
                    modifier = Modifier.weight(item.progress.coerceAtLeast(0.0001f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${(item.progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = item.color,
                    )
                }
            }
        }
    }
}
