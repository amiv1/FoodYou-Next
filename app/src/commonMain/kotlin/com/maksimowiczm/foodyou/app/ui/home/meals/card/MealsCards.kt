package com.maksimowiczm.foodyou.app.ui.home.meals.card

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.fooddiary.domain.entity.MealsCardsLayout
import com.valentinilk.shimmer.Shimmer
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun MealsCards(
    date: LocalDate,
    shimmer: Shimmer,
    onAdd: (epochDay: Long, mealId: Long) -> Unit,
    onQuickAdd: (epochDay: Long, mealId: Long) -> Unit,
    onEditEntry: (foodEntryId: Long?, manualEntryId: Long?) -> Unit,
    onLongClick: (mealId: Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: MealsCardsViewModel = koinViewModel(key = "meals-${date.toEpochDays()}"),
) {
    val diaryMeals = viewModel.diaryMeals.collectAsStateWithLifecycle().value
    val allMeals by viewModel.allMeals.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()

    LaunchedEffect(date, viewModel) { viewModel.setDate(date) }

    when (layout) {
        MealsCardsLayout.Horizontal ->
            HorizontalMealsCards(
                meals = diaryMeals,
                allMeals = allMeals,
                date = date,
                onAdd = { mealId -> onAdd(date.toEpochDays(), mealId) },
                onQuickAdd = { mealId -> onQuickAdd(date.toEpochDays(), mealId) },
                onEditEntry = { model ->
                    val foodEntry = model as? FoodMealEntryModel
                    val manualEntry = model as? ManualMealEntryModel
                    onEditEntry(foodEntry?.id?.value, manualEntry?.id?.value)
                },
                onDeleteEntry = viewModel::onDeleteEntry,
                onUpdateMeasurement = viewModel::onUpdateMeasurement,
                onCopyMeal = viewModel::copyMeal,
                onCopyEntry = viewModel::copyEntry,
                onLongClick = onLongClick,
                shimmer = shimmer,
                contentPadding = contentPadding,
                modifier = modifier,
            )

        MealsCardsLayout.Vertical ->
            VerticalMealsCards(
                meals = diaryMeals,
                allMeals = allMeals,
                date = date,
                onAdd = { mealId -> onAdd(date.toEpochDays(), mealId) },
                onQuickAdd = { mealId -> onQuickAdd(date.toEpochDays(), mealId) },
                onEditEntry = { model ->
                    val foodEntry = model as? FoodMealEntryModel
                    val manualEntry = model as? ManualMealEntryModel
                    onEditEntry(foodEntry?.id?.value, manualEntry?.id?.value)
                },
                onDeleteEntry = viewModel::onDeleteEntry,
                onUpdateMeasurement = viewModel::onUpdateMeasurement,
                onCopyMeal = viewModel::copyMeal,
                onCopyEntry = viewModel::copyEntry,
                onLongClick = onLongClick,
                shimmer = shimmer,
                contentPadding = contentPadding,
                modifier = modifier,
            )
    }
}
