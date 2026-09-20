package com.maksimowiczm.foodyou.app.testutil.food

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.maksimowiczm.foodyou.app.testutil.FoodYouComposeTest
import com.maksimowiczm.foodyou.app.testutil.anyDisplayed
import com.maksimowiczm.foodyou.app.testutil.onDisplayed
import com.maksimowiczm.foodyou.common.compose.utility.TestTags
import com.maksimowiczm.foodyou.common.domain.food.FoodSource
import com.maksimowiczm.foodyou.common.domain.food.NutrientValue
import com.maksimowiczm.foodyou.common.domain.food.NutritionFacts
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Covers the "My food and recipes" management screen: reaching it from the Home screen's
 * overflow menu, selecting rows via their always-on checkboxes, batch deletion with a
 * confirm/cancel dialog, and tapping a row (not its checkbox) navigating to the existing product
 * edit screen.
 */
class YourFoodManagementTest : FoodYouComposeTest() {

    private val suffix = System.currentTimeMillis() % 1_000_000
    private val keepProductName = "Keep Product $suffix"
    private val deleteProductName = "Delete Product $suffix"

    private var keepProductId: FoodId.Product? = null
    private var deleteProductId: FoodId.Product? = null

    @Before
    fun seedProducts() {
        runBlocking {
            keepProductId =
                productRepository.insertProduct(
                    name = keepProductName,
                    brand = null,
                    barcode = null,
                    note = null,
                    isLiquid = false,
                    packageWeight = null,
                    servingWeight = null,
                    source = FoodSource(FoodSource.Type.User),
                    nutritionFacts =
                        NutritionFacts(
                            energy = NutrientValue.from(100.0),
                            proteins = NutrientValue.from(10.0),
                            carbohydrates = NutrientValue.from(10.0),
                            fats = NutrientValue.from(1.0),
                        ),
                )
            deleteProductId =
                productRepository.insertProduct(
                    name = deleteProductName,
                    brand = null,
                    barcode = null,
                    note = null,
                    isLiquid = false,
                    packageWeight = null,
                    servingWeight = null,
                    source = FoodSource(FoodSource.Type.User),
                    nutritionFacts =
                        NutritionFacts(
                            energy = NutrientValue.from(150.0),
                            proteins = NutrientValue.from(15.0),
                            carbohydrates = NutrientValue.from(15.0),
                            fats = NutrientValue.from(2.0),
                        ),
                )
        }
    }

    @After
    fun cleanUp() {
        runBlocking {
            keepProductId?.let { id ->
                productRepository.observeProduct(id).first()?.let {
                    productRepository.deleteProduct(it)
                }
            }
            deleteProductId?.let { id ->
                productRepository.observeProduct(id).first()?.let {
                    productRepository.deleteProduct(it)
                }
            }
        }
    }

    @Test
    fun opening_your_food_screen_lists_seeded_products_and_batch_delete_flow_works() {
        navigateToYourFood()

        // Both seeded products are listed.
        assert(composeRule.onAllNodesWithText(keepProductName).anyDisplayed())
        assert(composeRule.onAllNodesWithText(deleteProductName).anyDisplayed())

        val deleteCheckboxTag = TestTags.yourFoodCheckbox(deleteProductId.toString())

        // Checking one checkbox reveals the contextual selection bar and hides the "+" toolbar
        // button.
        composeRule.onNodeWithTag(deleteCheckboxTag).performClick()
        composeRule.waitForIdle()
        assert(composeRule.onAllNodesWithText("1").anyDisplayed()) {
            "Expected the contextual selection bar to show the selected count '1'."
        }
        assert(
            composeRule
                .onAllNodesWithTag(TestTags.YourFoodCreateButton)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        ) {
            "Expected the '+' toolbar button to be hidden while a selection is active."
        }

        // Canceling the delete dialog leaves all items intact.
        composeRule.onNodeWithTag(TestTags.YourFoodDeleteButton).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasText("Cancel") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        assert(composeRule.onAllNodesWithText(deleteProductName).anyDisplayed()) {
            "Expected '$deleteProductName' to still be listed after canceling the delete dialog."
        }

        // Confirming delete removes only the checked item, leaving the rest.
        composeRule.onNodeWithTag(TestTags.YourFoodDeleteButton).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasText("Delete") and hasClickAction()).performClick()
        composeRule.waitForIdle()

        assert(composeRule.onAllNodesWithText(deleteProductName).anyDisplayed().not()) {
            "Expected '$deleteProductName' to be removed after confirming delete."
        }
        assert(composeRule.onAllNodesWithText(keepProductName).anyDisplayed()) {
            "Expected '$keepProductName' to remain after deleting the other product."
        }
        deleteProductId = null
    }

    @Test
    fun tapping_a_row_not_its_checkbox_navigates_to_the_product_edit_screen() {
        navigateToYourFood()

        composeRule.onAllNodesWithText(keepProductName).onDisplayed().performClick()
        composeRule.waitForIdle()

        assert(composeRule.onAllNodesWithText("Edit product").anyDisplayed()) {
            "Expected tapping the row to navigate to the product edit screen."
        }
    }

    private fun navigateToYourFood() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.HomeOverflowMenuButton).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.HomeMyFoodAndRecipesMenuItem).performClick()
        composeRule.waitForIdle()
    }
}
