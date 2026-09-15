package com.maksimowiczm.foodyou.fooddiary.domain.usecase

import com.maksimowiczm.foodyou.common.domain.database.TransactionProvider
import com.maksimowiczm.foodyou.common.domain.date.DateProvider
import com.maksimowiczm.foodyou.fooddiary.domain.repository.FoodDiaryEntryRepository
import com.maksimowiczm.foodyou.fooddiary.domain.repository.ManualDiaryEntryRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/**
 * Copies all diary entries (food and manual) from one meal/date to another meal/date, leaving the
 * original entries untouched.
 */
class CopyMealUseCase(
    private val foodEntryRepository: FoodDiaryEntryRepository,
    private val manualEntryRepository: ManualDiaryEntryRepository,
    private val transactionProvider: TransactionProvider,
    private val dateProvider: DateProvider,
) {
    /** Returns the number of entries copied. */
    suspend fun copy(
        sourceMealId: Long,
        sourceDate: LocalDate,
        targetMealId: Long,
        targetDate: LocalDate,
    ): Int {
        val foodEntries = foodEntryRepository.observeAll(sourceMealId, sourceDate).first()
        val manualEntries = manualEntryRepository.observeAll(sourceMealId, sourceDate).first()

        if (foodEntries.isEmpty() && manualEntries.isEmpty()) {
            return 0
        }

        val now = dateProvider.now()

        transactionProvider.withTransaction {
            foodEntries.forEach { entry ->
                foodEntryRepository.insert(
                    measurement = entry.measurement,
                    mealId = targetMealId,
                    date = targetDate,
                    food = entry.food,
                    createdAt = now,
                )
            }

            manualEntries.forEach { entry ->
                manualEntryRepository.insert(
                    name = entry.name,
                    mealId = targetMealId,
                    date = targetDate,
                    nutritionFacts = entry.nutritionFacts,
                    createdAt = now,
                )
            }
        }

        return foodEntries.size + manualEntries.size
    }
}
