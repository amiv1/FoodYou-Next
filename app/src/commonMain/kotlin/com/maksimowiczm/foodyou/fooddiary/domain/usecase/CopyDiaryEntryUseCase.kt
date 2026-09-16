package com.maksimowiczm.foodyou.fooddiary.domain.usecase

import com.maksimowiczm.foodyou.common.domain.date.DateProvider
import com.maksimowiczm.foodyou.fooddiary.domain.entity.FoodDiaryEntryId
import com.maksimowiczm.foodyou.fooddiary.domain.entity.ManualDiaryEntryId
import com.maksimowiczm.foodyou.fooddiary.domain.repository.FoodDiaryEntryRepository
import com.maksimowiczm.foodyou.fooddiary.domain.repository.ManualDiaryEntryRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/**
 * Copies a single diary entry (food or manual) to another meal/date, leaving the original entry
 * untouched.
 */
class CopyDiaryEntryUseCase(
    private val foodEntryRepository: FoodDiaryEntryRepository,
    private val manualEntryRepository: ManualDiaryEntryRepository,
    private val dateProvider: DateProvider,
) {
    suspend fun copyFoodEntry(
        entryId: FoodDiaryEntryId,
        targetMealId: Long,
        targetDate: LocalDate,
    ) {
        val entry = foodEntryRepository.observe(entryId).first() ?: return

        foodEntryRepository.insert(
            measurement = entry.measurement,
            mealId = targetMealId,
            date = targetDate,
            food = entry.food,
            createdAt = dateProvider.now(),
        )
    }

    suspend fun copyManualEntry(
        entryId: ManualDiaryEntryId,
        targetMealId: Long,
        targetDate: LocalDate,
    ) {
        val entry = manualEntryRepository.observe(entryId).first() ?: return

        manualEntryRepository.insert(
            name = entry.name,
            mealId = targetMealId,
            date = targetDate,
            nutritionFacts = entry.nutritionFacts,
            createdAt = dateProvider.now(),
        )
    }
}
