package com.maksimowiczm.foodyou.app.ui.home.meals.card

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.valentinilk.shimmer.Shimmer
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun HorizontalMealsCards(
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
    // Must be same as meals count or more but since we don't have meals count yet set it to some
    // extreme value. If it is less than actual meals count pager will scroll back to the
    // last item which is annoying for the user.
    // Let's assume that user won't use more than 20 meals
    val pagerState = rememberPagerState(pageCount = { meals?.size ?: 20 })

    val transition = updateTransition(meals)

    HorizontalPager(
        state = pagerState,
        modifier =
            modifier.animateContentSize(
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            ),
        verticalAlignment = Alignment.Top,
        contentPadding =
            PaddingValues(
                start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = 24.dp,
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
    ) { page ->
        val meal = meals?.getOrNull(page)

        transition.Crossfade(
            contentKey = { it != null },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        ) {
            if (it != null && meal != null) {
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
            } else {
                MealCardSkeleton(shimmer = shimmer)
            }
        }
    }
}
