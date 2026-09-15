package com.maksimowiczm.foodyou.app.ui.food.diary.update

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
import com.maksimowiczm.foodyou.fooddiary.domain.entity.DiaryFood
import com.maksimowiczm.foodyou.fooddiary.domain.entity.DiaryFoodRecipe
import foodyou.app.generated.resources.Res
import foodyou.app.generated.resources.headline_macronutrients
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NutrientList(
    food: DiaryFood,
    measurement: Measurement,
    modifier: Modifier = Modifier,
) {
    val weight = remember(food, measurement) { food.weight(measurement) }
    val facts = remember(food, weight) { food.nutritionFacts * (weight / 100) }

    Column(modifier) {
        val proteins = facts.proteins.value
        val carbohydrates = facts.carbohydrates.value
        val fats = facts.fats.value

        val measurementString =
            measurement.stringResourceWithWeight(
                totalWeight = food.totalWeight,
                servingWeight = food.servingWeight,
                isLiquid = food.isLiquid,
            ) ?: error("Invalid measurement: $measurement for ${food.name}")

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

                if (food is DiaryFoodRecipe && !food.nutritionFacts.isComplete) {
                    val foods =
                        remember(food) {
                            food
                                .flatIngredients()
                                .filterNot { it.nutritionFacts.isComplete }
                                .map { it.name }
                        }

                    IncompleteFoodsList(foods = foods, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}
