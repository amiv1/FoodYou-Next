package com.maksimowiczm.foodyou.app.ui.home.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maksimowiczm.foodyou.common.domain.food.NutritionFactsField
import com.maksimowiczm.foodyou.common.domain.food.sum
import com.maksimowiczm.foodyou.fooddiary.domain.usecase.ObserveDiaryMealsUseCase
import com.maksimowiczm.foodyou.goals.domain.repository.GoalsRepository
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

internal class GoalsViewModel(
    private val observeDiaryMealsUseCase: ObserveDiaryMealsUseCase,
    private val goalsRepository: GoalsRepository,
) : ViewModel() {

    private val dateState = MutableStateFlow<LocalDate?>(null)

    fun setDate(date: LocalDate) {
        dateState.value = date
    }

    val model: StateFlow<DaySummaryModel?> =
        dateState
            .filterNotNull()
            .flatMapLatest { date ->
                combine(
                    observeDiaryMealsUseCase.observe(date),
                    goalsRepository.observeDailyGoals(date),
                ) { meals, goal ->
                    val facts = meals.map { it.nutritionFacts }.sum()

                    DaySummaryModel(
                        energy = facts.energy.value?.roundToInt() ?: 0,
                        energyGoal = goal[NutritionFactsField.Energy].roundToInt(),
                        proteins = facts.proteins.value?.roundToInt() ?: 0,
                        proteinsGoal = goal[NutritionFactsField.Proteins].roundToInt(),
                        carbohydrates = facts.carbohydrates.value?.roundToInt() ?: 0,
                        carbohydratesGoal = goal[NutritionFactsField.Carbohydrates].roundToInt(),
                        fats = facts.fats.value?.roundToInt() ?: 0,
                        fatsGoal = goal[NutritionFactsField.Fats].roundToInt(),
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(2_000),
                initialValue = null,
            )
}
