package com.maksimowiczm.foodyou.app.ui.home.meals.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.valentinilk.shimmer.Shimmer
import kotlinx.datetime.LocalDate

@Composable
internal fun VerticalMealsCards(
    meals: List<MealModel>?,
    allMeals: List<MealOption>,
    date: LocalDate,
    onAdd: (mealId: Long) -> Unit,
    onQuickAdd: (mealId: Long) -> Unit,
    onEditEntry: (MealEntryModel) -> Unit,
    onDeleteEntry: (MealEntryModel) -> Unit,
    onUpdateMeasurement: (mealId: Long, FoodMealEntryModel, Measurement) -> Unit,
    onCopyMeal: (sourceMealId: Long, targetMealId: Long, targetDate: LocalDate) -> Unit,
    onCopyEntry: (entry: MealEntryModel, targetMealId: Long, targetDate: LocalDate) -> Unit,
    onLongClick: (mealId: Long) -> Unit,
    shimmer: Shimmer,
    contentPadding: PaddingValues,
    showCalories: Boolean,
    showMacronutrients: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (meals == null) {
            repeat(4) { MealCardSkeleton(shimmer) }
        } else {
            meals.forEach { meal ->
                MealCard(
                    meal = meal,
                    allMeals = allMeals,
                    date = date,
                    onAddFood = { onAdd(meal.id) },
                    onQuickAdd = { onQuickAdd(meal.id) },
                    onEditEntry = onEditEntry,
                    onDeleteEntry = onDeleteEntry,
                    onUpdateMeasurement = { entry, measurement ->
                        onUpdateMeasurement(meal.id, entry, measurement)
                    },
                    onCopyMeal = { targetMealId, targetDate ->
                        onCopyMeal(meal.id, targetMealId, targetDate)
                    },
                    onCopyEntry = onCopyEntry,
                    onLongClick = { onLongClick(meal.id) },
                    showCalories = showCalories,
                    showMacronutrients = showMacronutrients,
                )
            }
        }
    }
}
