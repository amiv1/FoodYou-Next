package com.maksimowiczm.foodyou.app.ui.food.diary.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maksimowiczm.foodyou.app.ui.common.component.IncompleteFoodsList
import com.maksimowiczm.foodyou.app.ui.common.utility.stringResourceWithWeight
import com.maksimowiczm.foodyou.app.ui.food.component.MacroDistributionIndicator
import com.maksimowiczm.foodyou.app.ui.food.shared.component.NutrientList
import com.maksimowiczm.foodyou.app.ui.home.shared.FoodYouHomeCard
import com.maksimowiczm.foodyou.common.domain.food.isComplete
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import foodyou.app.generated.resources.Res
import foodyou.app.generated.resources.headline_macronutrients
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NutrientList(
    food: FoodModel,
    measurement: Measurement,
    onEditFood: (FoodId) -> Unit,
    modifier: Modifier = Modifier,
) {
    // This is stupid that it is here but it's going to be deleted in 4.0.0
    val facts =
        remember(food, measurement) {
            val weight =
                try {
                    food.weight(measurement)
                } catch (_: IllegalStateException) {
                    100.0
                }
            food.nutritionFacts * (weight / 100)
        }

    Column(modifier) {
        val proteins = facts.proteins.value
        val carbohydrates = facts.carbohydrates.value
        val fats = facts.fats.value

        val measurementString =
            measurement.stringResourceWithWeight(
                totalWeight = food.totalWeight,
                servingWeight = food.servingWeight,
                isLiquid = food.isLiquid,
            ) ?: error("Invalid measurement: $measurement for ${food.foodId}")

        FoodYouHomeCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                if (proteins != null && carbohydrates != null && fats != null) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(Res.string.headline_macronutrients),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Spacer(Modifier.height(8.dp))

                        MacroDistributionIndicator(
                            proteins = proteins.toFloat(),
                            carbohydrates = carbohydrates.toFloat(),
                            fats = fats.toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Text(
                    text = measurementString,
                    modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )

                NutrientList(facts)

                if (food is RecipeModel && !food.nutritionFacts.isComplete) {
                    val foods =
                        food.allIngredients
                            .filter { (foodId, _, facts) ->
                                foodId is FoodId.Product && !facts.isComplete
                            }
                            .map { (foodId, name) -> foodId to name }

                    IncompleteFoodsList(
                        foods = foods.map { (_, name) -> name }.distinct(),
                        onFoodClick = { i -> onEditFood(foods[i].first) },
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}
