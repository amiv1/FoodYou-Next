package com.maksimowiczm.foodyou.app.testutil.food

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.maksimowiczm.foodyou.app.testutil.FoodYouComposeTest
import com.maksimowiczm.foodyou.app.testutil.anyDisplayed
import com.maksimowiczm.foodyou.common.compose.utility.TestTags
import com.maksimowiczm.foodyou.common.domain.food.FoodSource
import com.maksimowiczm.foodyou.common.domain.food.NutrientValue
import com.maksimowiczm.foodyou.common.domain.food.NutritionFacts
import com.maksimowiczm.foodyou.common.infrastructure.room.toEntity
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Covers the live search field in the "My food and recipes" screen: basic filtering, the
 * no-matches empty state, clearing restoring the full list, search coexisting with the
 * selection/delete flow, and a regression check that typing here does not leak into the
 * Add-Food screen's shared "recent searches" history (the reason `YourFoodViewModel` calls
 * `FoodSearchRepository` directly instead of `FoodSearchUseCase`).
 */
class YourFoodSearchTest : FoodYouComposeTest() {

    private val suffix = System.currentTimeMillis() % 1_000_000
    private val appleName = "Zzapple $suffix"
    private val bananaName = "Zzbanana $suffix"

    private var appleId: FoodId.Product? = null
    private var bananaId: FoodId.Product? = null

    @Before
    fun seedProducts() {
        runBlocking {
            appleId = insertTestProduct(appleName)
            bananaId = insertTestProduct(bananaName)
        }
    }

    @After
    fun cleanUp() {
        runBlocking {
            appleId?.let { id ->
                productRepository.observeProduct(id).first()?.let {
                    productRepository.deleteProduct(it)
                }
            }
            bananaId?.let { id ->
                productRepository.observeProduct(id).first()?.let {
                    productRepository.deleteProduct(it)
                }
            }
        }
    }

    private suspend fun insertTestProduct(name: String): FoodId.Product =
        productRepository.insertProduct(
            name = name,
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

    @Test
    fun typing_a_matching_query_filters_the_list_to_only_that_product() {
        navigateToYourFood()

        composeRule.onNodeWithTag(TestTags.YourFoodSearchField).performTextInput("Zzapple")
        waitForDebounce()

        assert(composeRule.onAllNodesWithText(appleName).anyDisplayed()) {
            "Expected '$appleName' to still be listed after searching for its name."
        }
        assert(composeRule.onAllNodesWithText(bananaName).anyDisplayed().not()) {
            "Expected '$bananaName' to be filtered out by the search."
        }
    }

    @Test
    fun typing_a_query_matching_nothing_shows_the_empty_state() {
        navigateToYourFood()

        composeRule.onNodeWithTag(TestTags.YourFoodSearchField)
            .performTextInput("no such food $suffix xyz")
        waitForDebounce()

        assert(composeRule.onAllNodesWithText(appleName).anyDisplayed().not())
        assert(composeRule.onAllNodesWithText(bananaName).anyDisplayed().not())
        assert(
            composeRule
                .onAllNodesWithText("No food found", substring = true)
                .anyDisplayed() ||
                composeRule.onAllNodesWithText("food found", substring = true).anyDisplayed()
        ) {
            "Expected the 'no food found' empty state to show when a search has zero matches."
        }
    }

    @Test
    fun clearing_the_search_field_restores_the_full_list() {
        navigateToYourFood()

        composeRule.onNodeWithTag(TestTags.YourFoodSearchField).performTextInput("Zzapple")
        waitForDebounce()
        assert(composeRule.onAllNodesWithText(bananaName).anyDisplayed().not())

        composeRule.onNodeWithTag(TestTags.YourFoodSearchField).performTextClearance()
        waitForDebounce()

        assert(composeRule.onAllNodesWithText(appleName).anyDisplayed()) {
            "Expected '$appleName' to reappear after clearing the search field."
        }
        assert(composeRule.onAllNodesWithText(bananaName).anyDisplayed()) {
            "Expected '$bananaName' to reappear after clearing the search field."
        }
    }

    @Test
    fun search_coexists_with_selection_and_delete() {
        navigateToYourFood()

        composeRule.onNodeWithTag(TestTags.YourFoodSearchField).performTextInput("Zzapple")
        waitForDebounce()

        val appleCheckboxTag = TestTags.yourFoodCheckbox(appleId.toString())
        composeRule.onNodeWithTag(appleCheckboxTag).performClick()
        composeRule.waitForIdle()

        // Search field stays visible/editable while a selection is active.
        composeRule.onNodeWithTag(TestTags.YourFoodSearchField).assertExists()
        assert(composeRule.onAllNodesWithText("1 selected", substring = true).anyDisplayed())

        composeRule.onNodeWithTag(TestTags.YourFoodDeleteButton).performClick()
        composeRule.waitForIdle()
        composeRule
            .onNode(hasText("Delete") and hasClickAction())
            .performClick()
        composeRule.waitForIdle()

        assert(composeRule.onAllNodesWithText(appleName).anyDisplayed().not()) {
            "Expected '$appleName' to be deleted while filtered by search."
        }
        appleId = null

        // List remains correctly filtered (query still active) after the delete.
        assert(composeRule.onAllNodesWithText(bananaName).anyDisplayed().not()) {
            "Expected the search filter to still be applied after deleting the selected item."
        }
    }

    @Test
    fun typing_here_does_not_pollute_the_shared_recent_searches_history() {
        navigateToYourFood()

        val junkQuery = "junkquery$suffix"
        composeRule.onNodeWithTag(TestTags.YourFoodSearchField).performTextInput(junkQuery)
        waitForDebounce()

        runBlocking {
            val history = foodSearchHistoryRepository.observeHistory(limit = 50).first()
            val polluted = history.any { it.query.query.contains(junkQuery) }
            assert(!polluted) {
                "Expected typing in the My Food search field to not write into the shared " +
                    "recent-searches history, but found a matching entry."
            }
        }
    }

    private fun waitForDebounce() {
        // Filtering is applied immediately (no debounce), but Paging's `cachedIn`/recomposition
        // can take an extra frame or two to reflect a new query, so poll briefly until settled.
        composeRule.waitForIdle()
        repeat(5) {
            Thread.sleep(100)
            composeRule.waitForIdle()
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
