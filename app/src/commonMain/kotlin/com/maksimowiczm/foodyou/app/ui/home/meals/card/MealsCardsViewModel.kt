package com.maksimowiczm.foodyou.app.ui.home.meals.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import com.maksimowiczm.foodyou.fooddiary.domain.entity.DiaryEntry
import com.maksimowiczm.foodyou.fooddiary.domain.entity.DiaryFoodRecipe
import com.maksimowiczm.foodyou.fooddiary.domain.entity.DiaryMeal
import com.maksimowiczm.foodyou.fooddiary.domain.entity.FoodDiaryEntry
import com.maksimowiczm.foodyou.fooddiary.domain.entity.ManualDiaryEntry
import com.maksimowiczm.foodyou.fooddiary.domain.entity.MealsPreferences
import com.maksimowiczm.foodyou.fooddiary.domain.repository.FoodDiaryEntryRepository
import com.maksimowiczm.foodyou.fooddiary.domain.repository.ManualDiaryEntryRepository
import com.maksimowiczm.foodyou.fooddiary.domain.repository.MealRepository
import com.maksimowiczm.foodyou.fooddiary.domain.usecase.CopyDiaryEntryUseCase
import com.maksimowiczm.foodyou.fooddiary.domain.usecase.CopyMealUseCase
import com.maksimowiczm.foodyou.fooddiary.domain.usecase.ObserveDiaryMealsUseCase
import com.maksimowiczm.foodyou.fooddiary.domain.usecase.UpdateFoodDiaryEntryUseCase
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

/**
 * Emitted after a copy action completes, so the UI can show a confirmation message and offer to
 * jump to the target date/meal.
 */
internal sealed interface CopyEvent {
    val targetDate: LocalDate
    val targetMealId: Long

    data class Meal(override val targetDate: LocalDate, override val targetMealId: Long) :
        CopyEvent

    data class Entry(override val targetDate: LocalDate, override val targetMealId: Long) :
        CopyEvent
}

internal class MealsCardsViewModel(
    private val observeDiaryMealsUseCase: ObserveDiaryMealsUseCase,
    private val foodEntryRepository: FoodDiaryEntryRepository,
    private val manualEntryRepository: ManualDiaryEntryRepository,
    private val updateFoodDiaryEntryUseCase: UpdateFoodDiaryEntryUseCase,
    private val copyMealUseCase: CopyMealUseCase,
    private val copyDiaryEntryUseCase: CopyDiaryEntryUseCase,
    private val mealRepository: MealRepository,
    mealsPreferencesRepository: UserPreferencesRepository<MealsPreferences>,
) : ViewModel() {
    private val dateState = MutableStateFlow<LocalDate?>(null)

    // Use a channel to emit events because it will wait for a consumer, unlike a SharedFlow which
    // would drop the event if nothing is currently collecting.
    private val copyEventChannel = Channel<CopyEvent>()
    val copyEvents = copyEventChannel.receiveAsFlow()

    val diaryMeals: StateFlow<List<MealModel>?> =
        dateState
            .filterNotNull()
            .flatMapLatest { date -> observeDiaryMealsUseCase.observe(date) }
            .map { list -> list.map { it.toMealModel() } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(60_000),
                initialValue = null,
            )

    val allMeals: StateFlow<List<MealOption>> =
        mealRepository
            .observeMeals()
            .map { meals -> meals.map { MealOption(id = it.id, name = it.name) } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(60_000),
                initialValue = emptyList(),
            )

    private val _layout = mealsPreferencesRepository.observe().map { it.layout }
    val layout =
        _layout.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2_000),
            initialValue = runBlocking { _layout.first() },
        )

    val showCalories =
        mealsPreferencesRepository
            .observe()
            .map { it.showCalories }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(2_000),
                initialValue = true,
            )

    val showMacronutrients =
        mealsPreferencesRepository
            .observe()
            .map { it.showMacronutrients }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(2_000),
                initialValue = true,
            )

    fun setDate(date: LocalDate) {
        viewModelScope.launch { dateState.value = date }
    }

    fun onDeleteEntry(model: MealEntryModel) {
        viewModelScope.launch {
            when (model) {
                is FoodMealEntryModel -> foodEntryRepository.delete(model.id)
                is ManualMealEntryModel -> manualEntryRepository.delete(model.id)
            }
        }
    }

    fun onUpdateMeasurement(mealId: Long, model: FoodMealEntryModel, measurement: Measurement) {
        val date = dateState.value ?: return

        viewModelScope.launch {
            updateFoodDiaryEntryUseCase.update(
                id = model.id,
                measurement = measurement,
                mealId = mealId,
                date = date,
            )
        }
    }

    fun copyMeal(sourceMealId: Long, targetMealId: Long, targetDate: LocalDate) {
        val sourceDate = dateState.value ?: return

        viewModelScope.launch {
            val copiedCount =
                copyMealUseCase.copy(
                    sourceMealId = sourceMealId,
                    sourceDate = sourceDate,
                    targetMealId = targetMealId,
                    targetDate = targetDate,
                )

            if (copiedCount > 0) {
                copyEventChannel.send(
                    CopyEvent.Meal(targetDate = targetDate, targetMealId = targetMealId)
                )
            }
        }
    }

    fun copyEntry(model: MealEntryModel, targetMealId: Long, targetDate: LocalDate) {
        viewModelScope.launch {
            when (model) {
                is FoodMealEntryModel ->
                    copyDiaryEntryUseCase.copyFoodEntry(model.id, targetMealId, targetDate)

                is ManualMealEntryModel ->
                    copyDiaryEntryUseCase.copyManualEntry(model.id, targetMealId, targetDate)
            }

            copyEventChannel.send(
                CopyEvent.Entry(targetDate = targetDate, targetMealId = targetMealId)
            )
        }
    }
}

private fun DiaryMeal.toMealModel(): MealModel =
    MealModel(
        id = meal.id,
        name = meal.name,
        from = meal.from,
        to = meal.to,
        isAllDay = meal.from == meal.to,
        foods = entries.map { it.toMealEntryModel() },
        energy = nutritionFacts.energy.value?.roundToInt() ?: 0,
        proteins = nutritionFacts.proteins.value ?: 0.0,
        carbohydrates = nutritionFacts.carbohydrates.value ?: 0.0,
        fats = nutritionFacts.fats.value ?: 0.0,
    )

private fun DiaryEntry.toMealEntryModel(): MealEntryModel =
    when (this) {
        is FoodDiaryEntry ->
            FoodMealEntryModel(
                id = id,
                name = food.name,
                energy = nutritionFacts.energy.value?.roundToInt(),
                proteins = nutritionFacts.proteins.value,
                carbohydrates = nutritionFacts.carbohydrates.value,
                fats = nutritionFacts.fats.value,
                measurement = measurement,
                weight = weight,
                isLiquid = food.isLiquid,
                isRecipe = food is DiaryFoodRecipe,
                totalWeight = food.totalWeight,
                servingWeight = food.servingWeight,
            )

        is ManualDiaryEntry ->
            ManualMealEntryModel(
                id = id,
                name = name,
                energy = nutritionFacts.energy.value?.roundToInt(),
                proteins = nutritionFacts.proteins.value,
                carbohydrates = nutritionFacts.carbohydrates.value,
                fats = nutritionFacts.fats.value,
            )
    }
