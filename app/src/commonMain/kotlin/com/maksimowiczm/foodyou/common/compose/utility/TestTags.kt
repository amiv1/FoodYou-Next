package com.maksimowiczm.foodyou.common.compose.utility

/**
 * Centralized [androidx.compose.ui.platform.testTag] constants used to locate composables from
 * instrumented UI tests.
 *
 * These tags have no effect on production behavior or accessibility - they only exist so
 * instrumented tests (`androidInstrumentedTest`) can reliably find composables that either have no
 * unique visible text/content description, or that are rendered multiple times simultaneously
 * (e.g. one per meal, or one per visible day column during the home screen swipe gesture).
 */
internal object TestTags {
    /** The whole-screen drag area used to swipe between days on the Home screen. */
    const val HomeDateSwipeArea = "home_date_swipe_area"

    /** Center button of the compact calendar bar; shows "Today" or the selected date. */
    const val CalendarDateButton = "calendar_date_button"

    /** Previous-day arrow button of the compact calendar bar. */
    const val CalendarPreviousDayButton = "calendar_previous_day_button"

    /** Next-day arrow button of the compact calendar bar. */
    const val CalendarNextDayButton = "calendar_next_day_button"

    /** Tag for a meal card's "Add" button, suffixed by the meal's id. */
    fun mealAddButton(mealId: Long): String = "meal_add_button_$mealId"

    /** Tag for a meal card's "Quick add" button, suffixed by the meal's id. */
    fun mealQuickAddButton(mealId: Long): String = "meal_quick_add_button_$mealId"

    /** Tag for a meal card's "Copy meal" button, suffixed by the meal's id. */
    fun mealCopyButton(mealId: Long): String = "meal_copy_button_$mealId"

    /** Home screen's top app bar overflow ("3 dot") menu button. */
    const val HomeOverflowMenuButton = "home_overflow_menu_button"

    /** Home screen's overflow menu item navigating to the "My food and recipes" screen. */
    const val HomeMyFoodAndRecipesMenuItem = "home_my_food_and_recipes_menu_item"

    /** "My food and recipes" screen's "+" toolbar button (create product/recipe). */
    const val YourFoodCreateButton = "your_food_create_button"

    /** "My food and recipes" screen's contextual "Delete" button, shown while selecting. */
    const val YourFoodDeleteButton = "your_food_delete_button"

    /** "My food and recipes" screen's "Select all" button, shown while selecting. */
    const val YourFoodSelectAllButton = "your_food_select_all_button"

    /** Home screen's "Today" floating action button, shown when a non-today date is selected. */
    const val TodayFab = "today_fab"

    /** "My food and recipes" screen's "Close selection" button, shown while selecting. */
    const val YourFoodCloseSelectionButton = "your_food_close_selection_button"

    /** "My food and recipes" screen's search input field. */
    const val YourFoodSearchField = "your_food_search_field"

    /** Tag for a "My food and recipes" row's checkbox, suffixed by the food's id. */
    fun yourFoodCheckbox(foodId: String): String = "your_food_checkbox_$foodId"
}
