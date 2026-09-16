package com.maksimowiczm.foodyou.app.testutil.fooddiary

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.maksimowiczm.foodyou.app.testutil.FoodYouComposeTest
import com.maksimowiczm.foodyou.app.testutil.anyDisplayed
import com.maksimowiczm.foodyou.app.testutil.onDisplayed
import com.maksimowiczm.foodyou.app.ui.food.diary.add.toDiaryFood
import com.maksimowiczm.foodyou.common.compose.utility.TestTags
import com.maksimowiczm.foodyou.common.domain.food.FoodSource
import com.maksimowiczm.foodyou.common.domain.food.NutrientValue
import com.maksimowiczm.foodyou.common.domain.food.NutritionFacts
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.food.domain.entity.RecipeIngredient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Covers copying a whole meal's entries and copying a single entry to another meal, plus edge
 * cases: an empty source meal hides the copy button, self-copy duplicates an entry in place, and
 * copying a recipe-based entry retains the recipe's name/macros.
 */
class MealAndEntryCopyTest : FoodYouComposeTest() {

    private val suffix = System.currentTimeMillis() % 1_000_000

    private var sourceMealId: Long = -1
    private var targetMealId: Long = -1
    private var emptyMealId: Long = -1

    private val today by lazy { dateProvider.now().date }
    private val entryOneName = "Copy Entry1 $suffix"
    private val entryTwoName = "Copy Entry2 $suffix"

    @Before
    fun seedMeals() {
        // Kept short: MealCard's header row has no ellipsis/weight on the meal name, so an
        // overly long name (e.g. a full 13-digit millis timestamp) can starve the copy icon
        // button of any layout width, making it unclickable.
        sourceMealId = createTestMeal("Copy Src $suffix")
        targetMealId = createTestMeal("Copy Tgt $suffix")
        emptyMealId = createTestMeal("Copy Empty $suffix")

        runBlocking {
            manualDiaryEntryRepository.insert(
                name = entryOneName,
                mealId = sourceMealId,
                date = today,
                nutritionFacts =
                    NutritionFacts(
                        energy = NutrientValue.from(111.0),
                        proteins = NutrientValue.from(11.0),
                        carbohydrates = NutrientValue.from(11.0),
                        fats = NutrientValue.from(1.0),
                    ),
                createdAt = dateProvider.now(),
            )
            manualDiaryEntryRepository.insert(
                name = entryTwoName,
                mealId = sourceMealId,
                date = today,
                nutritionFacts =
                    NutritionFacts(
                        energy = NutrientValue.from(222.0),
                        proteins = NutrientValue.from(22.0),
                        carbohydrates = NutrientValue.from(22.0),
                        fats = NutrientValue.from(2.0),
                    ),
                createdAt = dateProvider.now(),
            )
        }
    }

    @After
    fun cleanUp() {
        runBlocking {
            listOf(sourceMealId, targetMealId, emptyMealId).forEach {
                if (it != -1L) mealRepository.deleteMeal(it)
            }
        }
    }

    @Test
    fun copying_a_whole_meal_appends_its_entries_to_the_target_meal_keeping_the_source_intact() {
        composeRule.waitForIdle()
        clickTagRobustly(TestTags.mealCopyButton(sourceMealId))
        composeRule.waitForIdle()
        val targetMealName = mealName(targetMealId)
        selectMealChip(targetMealName)
        clickButtonWithText("Copy")
        composeRule.waitForIdle()

        val targetEntries = allEntryNames(targetMealId)
        assert(targetEntries.containsAll(listOf(entryOneName, entryTwoName))) {
            "Expected target meal to contain both copied entries, got: $targetEntries"
        }

        val sourceEntries = allEntryNames(sourceMealId)
        assert(sourceEntries.containsAll(listOf(entryOneName, entryTwoName))) {
            "Expected source meal to still contain its original entries, got: $sourceEntries"
        }
    }

    @Test
    fun copying_a_single_entry_only_copies_that_one_entry() {
        composeRule.waitForIdle()
        clickTextRobustly(entryOneName)
        composeRule.waitForIdle()

        clickButtonWithText("Copy")
        composeRule.waitForIdle()

        val targetMealName = mealName(targetMealId)
        selectMealChip(targetMealName)
        clickButtonWithText("Copy")
        composeRule.waitForIdle()

        val targetEntries = allEntryNames(targetMealId)
        assert(targetEntries == listOf(entryOneName)) {
            "Expected only '$entryOneName' in the target meal, got: $targetEntries"
        }
    }

    @Test
    fun copy_button_is_absent_for_a_meal_with_no_entries() {
        composeRule.waitForIdle()
        scrollUntilTextVisible(mealName(emptyMealId))
        composeRule.onAllNodesWithTag(TestTags.mealCopyButton(emptyMealId)).let { collection ->
            assert(!collection.anyDisplayed()) {
                "Expected no visible copy button for an empty meal."
            }
        }
    }

    @Test
    fun self_copy_duplicates_the_entry_in_the_same_meal() {
        runBlocking {
            val entry =
                manualDiaryEntryRepository.observeAll(sourceMealId, today).first().first {
                    it.name == entryOneName
                }
            copyDiaryEntryUseCase.copyManualEntry(
                entryId = entry.id,
                targetMealId = sourceMealId,
                targetDate = today,
            )

            val entries = allEntryNames(sourceMealId)
            val duplicateCount = entries.count { it == entryOneName }
            assert(duplicateCount == 2) {
                "Expected 2 occurrences of '$entryOneName' after self-copy, found $duplicateCount " +
                    "in $entries"
            }
        }
    }

    @Test
    fun copying_a_recipe_based_entry_retains_its_name_and_macros() {
        runBlocking {
            val productId =
                productRepository.insertProduct(
                    name = "Copy Test Product $suffix",
                    brand = null,
                    barcode = null,
                    note = null,
                    isLiquid = false,
                    packageWeight = null,
                    servingWeight = null,
                    source = FoodSource(FoodSource.Type.User),
                    nutritionFacts =
                        NutritionFacts(
                            energy = NutrientValue.from(400.0),
                            proteins = NutrientValue.from(40.0),
                            carbohydrates = NutrientValue.from(30.0),
                            fats = NutrientValue.from(10.0),
                        ),
                )
            val product = productRepository.observeProduct(productId).first()!!

            val recipeName = "Copy Test Recipe $suffix"
            val recipeId =
                recipeRepository.insertRecipe(
                    name = recipeName,
                    servings = 1,
                    note = null,
                    isLiquid = false,
                    ingredients = listOf(RecipeIngredient(product, Measurement.Gram(100.0))),
                )
            val recipe = recipeRepository.observeRecipe(recipeId).first()!!

            val entryId =
                foodDiaryEntryRepository.insert(
                    measurement = Measurement.Serving(1.0),
                    mealId = sourceMealId,
                    date = today,
                    food = recipe.toDiaryFood(),
                    createdAt = dateProvider.now(),
                )

            copyDiaryEntryUseCase.copyFoodEntry(
                entryId = entryId,
                targetMealId = targetMealId,
                targetDate = today,
            )

            val copied =
                foodDiaryEntryRepository.observeAll(targetMealId, today).first().firstOrNull {
                    it.food.name == recipeName
                }
            assertNotNull(copied) { "Expected a copied recipe entry named '$recipeName'." }
            assert(copied!!.food.nutritionFacts.energy == recipe.nutritionFacts.energy) {
                "Expected copied entry's energy to match the recipe's, got " +
                    "${copied.food.nutritionFacts.energy} vs ${recipe.nutritionFacts.energy}"
            }

            // Clean up the extra product/recipe created for this test.
            recipeRepository.deleteRecipe(recipe)
            productRepository.deleteProduct(product)
        }
    }

    @Test
    fun changing_a_copied_entrys_weight_does_not_affect_the_source_entry() {
        runBlocking {
            // 100g of a 200 kcal/100g product = 200 kcal.
            val productId =
                productRepository.insertProduct(
                    name = "Copy Weight Product $suffix",
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
            val product = productRepository.observeProduct(productId).first()!!

            val sourceEntryId =
                foodDiaryEntryRepository.insert(
                    measurement = Measurement.Gram(100.0),
                    mealId = sourceMealId,
                    date = today,
                    food = product.toDiaryFood(),
                    createdAt = dateProvider.now(),
                )

            copyDiaryEntryUseCase.copyFoodEntry(
                entryId = sourceEntryId,
                targetMealId = targetMealId,
                targetDate = today,
            )

            val copiedEntry =
                foodDiaryEntryRepository.observeAll(targetMealId, today).first().first {
                    it.food.name == product.name
                }

            // Change the copied entry's weight (100g -> 25g) and confirm the source entry, still
            // at its original 100g, is untouched - i.e. the two entries are independent rows, not
            // sharing the same measurement.
            foodDiaryEntryRepository.update(
                copiedEntry.copy(measurement = Measurement.Gram(25.0))
            )

            val sourceEntryAfter =
                foodDiaryEntryRepository.observe(sourceEntryId).first()!!
            assert(sourceEntryAfter.measurement == Measurement.Gram(100.0)) {
                "Expected the source entry's measurement to remain 100g after editing the " +
                    "copied entry's weight, got ${sourceEntryAfter.measurement}."
            }
            assert(sourceEntryAfter.nutritionFacts.energy == NutrientValue.from(200.0)) {
                "Expected the source entry's energy to remain 200 kcal, got " +
                    "${sourceEntryAfter.nutritionFacts.energy}."
            }

            val copiedEntryAfter =
                foodDiaryEntryRepository.observe(copiedEntry.id).first()!!
            assert(copiedEntryAfter.measurement == Measurement.Gram(25.0)) {
                "Expected the copied entry's measurement to be updated to 25g, got " +
                    "${copiedEntryAfter.measurement}."
            }

            // Clean up the extra product created for this test.
            productRepository.deleteProduct(product)
        }
    }

    private fun assertNotNull(value: Any?, message: () -> String) {
        assert(value != null, message)
    }

    private suspend fun allEntryNamesSuspend(mealId: Long): List<String> {
        val manual = manualDiaryEntryRepository.observeAll(mealId, today).first().map { it.name }
        val food = foodDiaryEntryRepository.observeAll(mealId, today).first().map { it.food.name }
        return manual + food
    }

    private fun allEntryNames(mealId: Long): List<String> = runBlocking {
        allEntryNamesSuspend(mealId)
    }

    private fun mealName(mealId: Long): String = runBlocking {
        mealRepository.observeMeals().first().first { it.id == mealId }.name
    }

    /**
     * Selects the `ChipsMealPicker` chip labeled [mealName] inside the currently-open
     * `CopyMealDialog`. Matching purely on text is ambiguous (the same meal name is also shown on
     * the underlying Home screen's meal card, whose merged semantics text also contains it) - the
     * chip is uniquely identified by its `Role.Checkbox` semantics (Material3 `InputChip`'s
     * selectable role).
     */
    private fun selectMealChip(mealName: String) {
        val chipMatcher =
            hasText(mealName) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        // The dialog's meal list is sourced from a StateFlow that may not have caught up with a
        // just-inserted test meal yet (especially for the very first dialog opened in a test run)
        // - wait for the chip to actually appear rather than asserting immediately.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodes(chipMatcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNode(chipMatcher).performClick()
    }

    /**
     * Clicks a clickable node (e.g. a dialog's confirm `TextButton`) whose text is exactly [text].
     * Some dialogs (e.g. the single-entry `CopyMealDialog`) reuse the same label for both their
     * title (plain, non-clickable `Text`) and their confirm button - restricting the match to
     * nodes with a click action disambiguates the two.
     */
    private fun clickButtonWithText(text: String) {
        composeRule.onNode(hasText(text) and hasClickAction()).performClick()
    }

    /**
     * Scrolls to and clicks the node tagged [tag]. Uses [androidx.compose.ui.test.performScrollTo]
     * (a semantics-driven scroll, not a raw touch gesture) once the node is composed - far more
     * reliable than swipe-gesture-based scrolling for pinpoint clicks.
     */
    private fun clickTagRobustly(tag: String) {
        scrollUntilTagVisible(tag)
        val collection = composeRule.onAllNodesWithTag(tag)
        val count = collection.fetchSemanticsNodes(atLeastOneRootRequired = false).size
        val target = if (count == 1) collection.get(0) else collection.onDisplayed()
        target.performScrollTo()
        composeRule.waitForIdle()
        target.performClick()
    }

    /** Same as [clickTagRobustly], but matching by visible text instead of a test tag. */
    private fun clickTextRobustly(text: String) {
        scrollUntilTextVisible(text)
        val collection = composeRule.onAllNodesWithText(text)
        val count = collection.fetchSemanticsNodes(atLeastOneRootRequired = false).size
        val target = if (count == 1) collection.get(0) else collection.onDisplayed()
        target.performScrollTo()
        composeRule.waitForIdle()
        target.performClick()
    }

    private fun scrollUntilTagVisible(tag: String) {
        repeat(10) {
            if (composeRule.onAllNodesWithTag(tag).anyDisplayed()) return
            composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
    }

    private fun scrollUntilTextVisible(text: String) {
        repeat(10) {
            if (composeRule.onAllNodesWithText(text).anyDisplayed()) return
            composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
    }
}
