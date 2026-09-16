package com.maksimowiczm.foodyou.app.testutil.fooddiary

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.entity.RecipeIngredient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Covers creating a product and a recipe (seeded directly via repositories, mirroring how the
 * real creation screens persist data) and adding either to the diary, plus the "recipe with zero
 * ingredients can't be saved" edge case exercised through the real Create Recipe UI.
 */
class RecipeAndProductJournalTest : FoodYouComposeTest() {

    private val suffix = System.currentTimeMillis()
    private val productName = "Test Product $suffix"
    private val recipeName = "Test Recipe $suffix"
    private var testMealId: Long = -1
    private var productId: FoodId.Product? = null
    private var recipeId: FoodId.Recipe? = null

    @Before
    fun seedMeal() {
        testMealId = createTestMeal("Test Journal Meal $suffix")
    }

    @After
    fun cleanUp() {
        runBlocking {
            recipeId?.let { id ->
                recipeRepository.observeRecipe(id).first()?.let { recipeRepository.deleteRecipe(it) }
            }
            productId?.let { id ->
                productRepository.observeProduct(id).first()?.let {
                    productRepository.deleteProduct(it)
                }
            }
            mealRepository.deleteMeal(testMealId)
        }
    }

    @Test
    fun recipe_built_from_a_product_shows_up_in_the_diary_with_scaled_macros() {
        runBlocking {
            // 100g of product = 200 kcal / 20g protein / 10g carbs / 5g fat.
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

            // Recipe uses 50g of the product as its only ingredient -> half the per-100g facts.
            val newRecipeId =
                recipeRepository.insertRecipe(
                    name = recipeName,
                    servings = 1,
                    note = null,
                    isLiquid = false,
                    ingredients = listOf(RecipeIngredient(product, Measurement.Gram(50.0))),
                )
            recipeId = newRecipeId
            val recipe = recipeRepository.observeRecipe(newRecipeId).first()!!

            foodDiaryEntryRepository.insert(
                measurement = Measurement.Serving(1.0),
                mealId = testMealId,
                date = dateProvider.now().date,
                food = recipe.toDiaryFood(),
                createdAt = dateProvider.now(),
            )
        }

        composeRule.waitForIdle()
        scrollUntilVisible(recipeName)

        assert(composeRule.onAllNodesWithText(recipeName).anyDisplayed())
        // The recipe's only serving totals 100 kcal (50g at 200 kcal/100g).
        assert(composeRule.onAllNodesWithText("100").anyDisplayed())
    }

    @Test
    fun creating_a_recipe_with_zero_ingredients_keeps_save_disabled() {
        composeRule.waitForIdle()
        scrollUntilTagVisible(TestTags.mealAddButton(testMealId))
        composeRule.onAllNodesWithTag(TestTags.mealAddButton(testMealId)).onDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Create").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Recipe").performClick()
        composeRule.waitForIdle()

        // No ingredients were added - Save must remain disabled, and no recipe should be created.
        composeRule.onNodeWithContentDescription("Save").assertIsNotEnabled()
    }

    private fun scrollUntilVisible(text: String) {
        repeat(10) {
            if (composeRule.onAllNodesWithText(text).anyDisplayed()) return
            composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
    }

    private fun scrollUntilTagVisible(tag: String) {
        repeat(10) {
            if (composeRule.onAllNodesWithTag(tag).anyDisplayed()) return
            composeRule.onNodeWithTag(TestTags.HomeDateSwipeArea).performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
    }
}
