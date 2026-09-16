package com.maksimowiczm.foodyou.app.testutil

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.maksimowiczm.foodyou.app.infrastructure.android.MainActivity
import com.maksimowiczm.foodyou.common.domain.date.DateProvider
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import com.maksimowiczm.foodyou.fooddiary.domain.entity.MealsPreferences
import com.maksimowiczm.foodyou.fooddiary.domain.repository.FoodDiaryEntryRepository
import com.maksimowiczm.foodyou.fooddiary.domain.repository.ManualDiaryEntryRepository
import com.maksimowiczm.foodyou.fooddiary.domain.repository.MealRepository
import com.maksimowiczm.foodyou.food.domain.repository.ProductRepository
import com.maksimowiczm.foodyou.food.domain.repository.RecipeRepository
import com.maksimowiczm.foodyou.settings.domain.entity.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.koin.android.ext.android.get
import org.koin.core.qualifier.named

/**
 * Base class for Compose UI instrumented tests that launch the real [MainActivity], using the
 * real Koin dependency graph (real Room database, real DataStore-backed settings) - there is no
 * fake/test Koin module infrastructure in this project, mirroring the existing
 * `SecureActivityTest`.
 *
 * Since the app under test's storage persists across test runs on the device, subclasses are
 * expected to be self-cleaning: create only the data they need (preferring direct repository
 * calls for setup/seeding), and remove what they created in an `@After` step.
 */
abstract class FoodYouComposeTest {

    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    /** Resolves a Koin dependency from the real application graph. */
    protected inline fun <reified T : Any> get(qualifier: org.koin.core.qualifier.Qualifier? = null): T =
        composeRule.activity.get(qualifier)

    protected val settingsRepository: UserPreferencesRepository<Settings>
        get() = get(named(Settings::class.qualifiedName!!))

    protected val mealsPreferencesRepository: UserPreferencesRepository<MealsPreferences>
        get() = get(named(MealsPreferences::class.qualifiedName!!))

    protected val mealRepository: MealRepository
        get() = get()

    protected val manualDiaryEntryRepository: ManualDiaryEntryRepository
        get() = get()

    protected val foodDiaryEntryRepository: FoodDiaryEntryRepository
        get() = get()

    protected val productRepository: ProductRepository
        get() = get()

    protected val recipeRepository: RecipeRepository
        get() = get()

    protected val copyMealUseCase: com.maksimowiczm.foodyou.fooddiary.domain.usecase.CopyMealUseCase
        get() = get()

    protected val copyDiaryEntryUseCase:
        com.maksimowiczm.foodyou.fooddiary.domain.usecase.CopyDiaryEntryUseCase
        get() = get()

    protected val dateProvider: DateProvider
        get() = get()

    /**
     * Creates a uniquely-named test meal (via [MealRepository]) and returns its id, resolved by
     * re-reading the meal list after insertion (`insertMealWithLastRank` does not return an id).
     */
    protected fun createTestMeal(
        name: String = "Test Meal ${System.currentTimeMillis()}"
    ): Long = runBlocking {
        mealRepository.insertMealWithLastRank(
            name = name,
            from = kotlinx.datetime.LocalTime(0, 0),
            to = kotlinx.datetime.LocalTime(23, 59),
        )
        mealRepository.observeMeals().first().first { it.name == name }.id
    }

    @Before
    fun baseSetUp() {
        // Force-finish onboarding so Home is immediately reachable, regardless of whether this is
        // the device's first launch or a subsequent test run.
        runBlocking { settingsRepository.update { copy(onboardingFinished = true) } }

        // Mark the "What's new" changelog as already seen for the current app version, so its
        // modal bottom sheet doesn't pop up over the Home screen and intercept test clicks.
        val appConfig = get<com.maksimowiczm.foodyou.common.config.AppConfig>()
        runBlocking {
            settingsRepository.update { copy(lastRememberedVersion = appConfig.versionName) }
        }
    }
}

/**
 * Returns the single currently on-screen match for [tag], out of possibly several simultaneously
 * composed matches (e.g. the Home screen mounts up to 3 day columns - previous/current/next - at
 * once during the swipe gesture, each with their own identically-tagged calendar buttons).
 */
fun SemanticsNodeInteractionCollection.onDisplayed(): SemanticsNodeInteraction {
    val count = fetchSemanticsNodes(atLeastOneRootRequired = false).size
    require(count > 0) { "No nodes found for this tag." }
    for (i in 0 until count) {
        val candidate = get(i)
        if (candidate.isDisplayed()) {
            return candidate
        }
    }
    error("No displayed node found among $count matches.")
}

/** True if at least one of the matched nodes is currently displayed on screen. */
fun SemanticsNodeInteractionCollection.anyDisplayed(): Boolean {
    val count = fetchSemanticsNodes(atLeastOneRootRequired = false).size
    for (i in 0 until count) {
        if (get(i).isDisplayed()) return true
    }
    return false
}

/** Reads the plain text content of a node's `Text`/`EditableText` semantics, if any. */
fun SemanticsNodeInteraction.textValue(): String =
    fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.joinToString(separator = "") {
        it.text
    } ?: ""
