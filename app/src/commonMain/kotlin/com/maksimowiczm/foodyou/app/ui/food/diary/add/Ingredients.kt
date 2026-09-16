package com.maksimowiczm.foodyou.app.ui.food.diary.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maksimowiczm.foodyou.app.ui.common.component.FoodErrorListItem
import com.maksimowiczm.foodyou.app.ui.common.component.FoodListItem
import com.maksimowiczm.foodyou.app.ui.common.utility.LocalEnergyFormatter
import com.maksimowiczm.foodyou.app.ui.common.utility.stringResourceWithWeight
import com.maksimowiczm.foodyou.app.ui.home.shared.FoodYouHomeCard
import com.maksimowiczm.foodyou.common.compose.extension.horizontal
import com.maksimowiczm.foodyou.common.compose.extension.vertical
import com.maksimowiczm.foodyou.common.compose.utility.formatClipZeros
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import foodyou.app.generated.resources.*
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Ingredients(
    ingredients: List<IngredientModel>,
    onIngredient: (FoodId, Measurement) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val g = stringResource(Res.string.unit_gram_short)

    val verticalPadding = contentPadding.vertical()
    val horizontal = contentPadding.horizontal()

    Column(modifier.padding(verticalPadding).padding(horizontal)) {
        FoodYouHomeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = stringResource(Res.string.headline_ingredients),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                val innerPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ingredients.forEach { ingredient ->
                    val facts = ingredient.nutritionFacts
                    val proteins = facts?.proteins?.value
                    val carbs = facts?.carbohydrates?.value
                    val fats = facts?.fats?.value
                    val energy = facts?.energy?.value
                    val measurementString =
                        ingredient.measurement.stringResourceWithWeight(
                            totalWeight = ingredient.totalWeight,
                            servingWeight = ingredient.servingWeight,
                            isLiquid = ingredient.isLiquid,
                        )

                    if (measurementString == null) {
                        FoodErrorListItem(
                            headline = ingredient.name,
                            errorMessage = stringResource(Res.string.error_measurement_error),
                            contentPadding = innerPadding,
                        )
                    } else if (proteins == null || carbs == null || fats == null || energy == null) {
                        FoodErrorListItem(
                            headline = ingredient.name,
                            errorMessage =
                                stringResource(Res.string.error_food_is_missing_required_fields),
                            contentPadding = innerPadding,
                        )
                    } else {
                        FoodListItem(
                            name = { Text(ingredient.name) },
                            proteins = {
                                val text = proteins.formatClipZeros() + " $g"
                                Text(text)
                            },
                            carbohydrates = {
                                val text = carbs.formatClipZeros() + " $g"
                                Text(text)
                            },
                            fats = {
                                val text = fats.formatClipZeros() + " $g"
                                Text(text)
                            },
                            calories = {
                                Text(LocalEnergyFormatter.current.formatEnergy(energy.roundToInt()))
                            },
                            measurement = { Text(measurementString) },
                            contentPadding = innerPadding,
                            isRecipe = ingredient.isRecipe,
                            onClick = { onIngredient(ingredient.foodId, ingredient.measurement) },
                        )
                    }
                }
            }
        }
    }
}
