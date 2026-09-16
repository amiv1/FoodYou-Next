package com.maksimowiczm.foodyou.app.testutil.home

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.maksimowiczm.foodyou.app.testutil.FoodYouComposeTest
import com.maksimowiczm.foodyou.app.testutil.anyDisplayed
import com.maksimowiczm.foodyou.common.compose.utility.TestTags
import com.maksimowiczm.foodyou.common.domain.food.NutrientValue
import com.maksimowiczm.foodyou.common.domain.food.NutritionFacts
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Covers the Home screen's Goals-card ("Show calories" / "Show macronutrients") and Meals-card
 * (same two toggles) customization settings, verifying that toggling them actually
 * hides/shows the corresponding sections of the real rendered Home screen.
 */
class HomeGoalsAndMealsCardCustomizationTest : FoodYouComposeTest() {

    private var testMealId: Long = -1
    private val entryName = "Customization Test Entry ${System.currentTimeMillis()}"
    private val today: LocalDate
        get() = dateProvider.now().date

    @Before
    fun seedData() {
        runBlocking {
            testMealId = createTestMeal()
            manualDiaryEntryRepository.insert(
                name = entryName,
                mealId = testMealId,
                date = today,
                nutritionFacts =
                    NutritionFacts(
                        energy = NutrientValue.from(777.0),
                        proteins = NutrientValue.from(11.0),
                        carbohydrates = NutrientValue.from(22.0),
                        fats = NutrientValue.from(33.0),
                    ),
                createdAt = dateProvider.now(),
            )
        }
    }

    @After
    fun cleanUp() {
        runBlocking {
            // Restore both toggles to their default (ON) state and remove the test meal
            // (cascades its entries), regardless of which assertions ran.
            settingsRepository.update {
                copy(showGoalsCalories = true, showGoalsMacronutrients = true)
            }
            mealsPreferencesRepository.update {
                copy(showCalories = true, showMacronutrients = true)
            }
            if (testMealId != -1L) {
                mealRepository.deleteMeal(testMealId)
            }
        }
    }

    @Test
    fun goals_card_shows_calorie_row_by_default() {
        composeRule.waitForIdle()
        assert(composeRule.onAllNodesWithText("Target").anyDisplayed())
        assert(composeRule.onAllNodesWithText("Consumed").anyDisplayed())
    }

    @Test
    fun disabling_show_calories_hides_the_calorie_row_but_keeps_macronutrients() {
        runBlocking { settingsRepository.update { copy(showGoalsCalories = false) } }
        composeRule.waitForIdle()

        assert(!composeRule.onAllNodesWithText("Target").anyDisplayed())
        assert(!composeRule.onAllNodesWithText("Consumed").anyDisplayed())
    }

    @Test
    fun disabling_both_goals_toggles_hides_the_whole_goals_card() {
        runBlocking {
            settingsRepository.update {
                copy(showGoalsCalories = false, showGoalsMacronutrients = false)
            }
        }
        composeRule.waitForIdle()

        assert(!composeRule.onAllNodesWithText("Target").anyDisplayed())
        assert(!composeRule.onAllNodesWithText("Consumed").anyDisplayed())
        assert(!composeRule.onAllNodesWithText("Excess").anyDisplayed())
        assert(!composeRule.onAllNodesWithText("Remaining").anyDisplayed())
    }

    @Test
    fun re_enabling_both_goals_toggles_shows_the_card_again() {
        runBlocking {
            settingsRepository.update {
                copy(showGoalsCalories = false, showGoalsMacronutrients = false)
            }
        }
        composeRule.waitForIdle()
        assert(!composeRule.onAllNodesWithText("Target").anyDisplayed())

        runBlocking {
            settingsRepository.update {
                copy(showGoalsCalories = true, showGoalsMacronutrients = true)
            }
        }
        composeRule.waitForIdle()
        assert(composeRule.onAllNodesWithText("Target").anyDisplayed())
    }

    @Test
    fun meal_card_shows_entry_calories_and_macros_by_default() {
        scrollUntilEntryVisible()
        assert(composeRule.onAllNodesWithText(entryName).anyDisplayed())
        assert(composeRule.onAllNodesWithText("777").anyDisplayed())
    }

    @Test
    fun disabling_meals_show_calories_hides_the_entrys_calorie_caption() {
        runBlocking {
            // Also hide the Goals card's own calorie row: since this is the only diary entry for
            // today, its "Consumed" value would otherwise also read "777" and make this
            // assertion ambiguous between the two independent features.
            settingsRepository.update { copy(showGoalsCalories = false) }
            mealsPreferencesRepository.update { copy(showCalories = false) }
        }
        scrollUntilEntryVisible()

        // The entry itself (and its name) must remain visible - only the calorie caption hides.
        assert(composeRule.onAllNodesWithText(entryName).anyDisplayed())
        assert(!composeRule.onAllNodesWithText("777").anyDisplayed())
    }

    @Test
    fun disabling_meals_show_macronutrients_hides_the_entrys_macro_captions() {
        runBlocking {
            // Also hide the Goals card's own macronutrient row, for the same reason as above -
            // its "consumed" grams would otherwise coincidentally match our seeded values.
            settingsRepository.update { copy(showGoalsMacronutrients = false) }
            mealsPreferencesRepository.update { copy(showMacronutrients = false) }
        }
        scrollUntilEntryVisible()

        assert(composeRule.onAllNodesWithText(entryName).anyDisplayed())
        assert(!composeRule.onAllNodesWithText("11").anyDisplayed())
        assert(!composeRule.onAllNodesWithText("22").anyDisplayed())
        assert(!composeRule.onAllNodesWithText("33").anyDisplayed())
    }

    /**
     * The test meal is created with the last rank, so it renders at the bottom of the (scrollable)
     * Meals section - scroll the Home screen's vertical content down until it (or its seeded
     * entry) becomes visible. Vertical swipes pass through the swipe-to-change-date gesture
     * detector untouched (it only recognizes horizontal drags), so this reaches the real
     * underlying `LazyColumn`.
     */
    private fun scrollUntilEntryVisible() {
        composeRule.waitForIdle()
        repeat(10) {
            if (composeRule.onAllNodesWithText(entryName).anyDisplayed()) return
            composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
    }
}
