package com.maksimowiczm.foodyou.app.testutil.fooddiary

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.espresso.Espresso
import com.maksimowiczm.foodyou.app.testutil.FoodYouComposeTest
import com.maksimowiczm.foodyou.app.testutil.anyDisplayed
import com.maksimowiczm.foodyou.app.testutil.onDisplayed
import com.maksimowiczm.foodyou.app.ui.food.diary.add.toDiaryFood
import com.maksimowiczm.foodyou.common.compose.utility.TestTags
import com.maksimowiczm.foodyou.common.domain.food.FoodSource
import com.maksimowiczm.foodyou.common.domain.food.NutrientValue
import com.maksimowiczm.foodyou.common.domain.food.NutritionFacts
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Covers editing a food diary entry's measurement (via the entry action bottom sheet's inline
 * measurement scrubber) and deleting an entry (via the bottom sheet's "Delete entry" action +
 * confirmation dialog).
 */
class EntryEditDeleteTest : FoodYouComposeTest() {

    private val suffix = System.currentTimeMillis() % 1_000_000
    private val productName = "Edit Product $suffix"
    private val editEntryFoodName = "$productName"
    private val deleteEntryName = "Delete Entry $suffix"

    private var testMealId: Long = -1
    private var productId: FoodId.Product? = null

    @Before
    fun seedMealAndEntries() {
        testMealId = createTestMeal("Edit Delete Meal $suffix")

        runBlocking {
            // 100g of product = 200 kcal - used to verify the scrubber's measurement edit
            // actually changes the displayed calories proportionally.
            val newProductId =
                productRepository.insertProduct(
                    name = productName,
                    brand = null,
                    barcode = null,
                    note = null,
                    isLiquid = false,
                    packageWeight = null,
                    servingWeight = null,
                    source = FoodSource(FoodSource.Type.User),
                    nutritionFacts =
                        NutritionFacts(
                            energy = NutrientValue.from(200.0),
                            proteins = NutrientValue.from(20.0),
                            carbohydrates = NutrientValue.from(10.0),
                            fats = NutrientValue.from(5.0),
                        ),
                )
            productId = newProductId
            val product = productRepository.observeProduct(newProductId).first()!!

            foodDiaryEntryRepository.insert(
                measurement = Measurement.Gram(100.0),
                mealId = testMealId,
                date = dateProvider.now().date,
                food = product.toDiaryFood(),
                createdAt = dateProvider.now(),
            )

            manualDiaryEntryRepository.insert(
                name = deleteEntryName,
                mealId = testMealId,
                date = dateProvider.now().date,
                nutritionFacts =
                    NutritionFacts(
                        energy = NutrientValue.from(50.0),
                        proteins = NutrientValue.from(5.0),
                        carbohydrates = NutrientValue.from(5.0),
                        fats = NutrientValue.from(1.0),
                    ),
                createdAt = dateProvider.now(),
            )
        }
    }

    @After
    fun cleanUp() {
        runBlocking {
            productId?.let { id ->
                productRepository.observeProduct(id).first()?.let {
                    productRepository.deleteProduct(it)
                }
            }
            mealRepository.deleteMeal(testMealId)
        }
    }

    @Test
    fun editing_an_entrys_measurement_updates_its_displayed_calories() {
        composeRule.waitForIdle()
        scrollUntilTextVisible(editEntryFoodName)
        composeRule.onAllNodesWithText(editEntryFoodName).onDisplayed().performClick()
        composeRule.waitForIdle()

        // Tap the scrubber's value text ("100 g") to enter edit mode, replace it with "50", then
        // confirm via the keyboard's IME action - halving the measurement should halve the
        // displayed calories once the sheet is dismissed.
        composeRule
            .onNode(hasText("100 g") and hasClickAction(), useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("50")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()

        // Dismiss the bottom sheet via the system back action (standard for Material3
        // ModalBottomSheet), rather than tapping a screen coordinate that could land on unrelated
        // content behind the sheet.
        Espresso.pressBack()
        composeRule.waitForIdle()

        scrollUntilTextVisible(editEntryFoodName)
        // 100g of a 200 kcal/100g product = 200 kcal; edited to 50g = 100 kcal.
        assert(composeRule.onAllNodesWithText("200 kcal", substring = true).anyDisplayed().not()) {
            "Expected the old 200 kcal value to be gone after editing the measurement to 50g."
        }
        assert(composeRule.onAllNodesWithText("100 kcal", substring = true).anyDisplayed()) {
            "Expected the entry's calories to show 100 kcal after editing its measurement to 50g."
        }
    }

    @Test
    fun deleting_an_entry_removes_it_from_the_meal_card() {
        composeRule.waitForIdle()
        scrollUntilTextVisible(deleteEntryName)
        composeRule.onAllNodesWithText(deleteEntryName).onDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Delete entry").performClick()
        composeRule.waitForIdle()

        // Confirm the delete dialog. Its confirm button label is the shorter "Delete" (distinct
        // from the bottom sheet's "Delete entry" item), and is uniquely a clickable button.
        composeRule.onNode(hasText("Delete") and hasClickAction()).performClick()
        composeRule.waitForIdle()

        val entries = allEntryNames(testMealId)
        assert(!entries.contains(deleteEntryName)) {
            "Expected '$deleteEntryName' to be removed from the meal, got: $entries"
        }
    }

    private fun allEntryNames(mealId: Long): List<String> = runBlocking {
        val foodNames =
            foodDiaryEntryRepository.observeAll(mealId, dateProvider.now().date).first().map {
                it.name
            }
        val manualNames =
            manualDiaryEntryRepository.observeAll(mealId, dateProvider.now().date).first().map {
                it.name
            }
        foodNames + manualNames
    }

    private fun scrollUntilTextVisible(text: String) {
        repeat(10) {
            if (composeRule.onAllNodesWithText(text).anyDisplayed()) return
            composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
    }
}
