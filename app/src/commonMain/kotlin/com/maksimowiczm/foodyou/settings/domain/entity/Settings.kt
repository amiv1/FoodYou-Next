package com.maksimowiczm.foodyou.settings.domain.entity

import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferences

data class Settings(
    val lastRememberedVersion: String?,
    val hidePreviewDialog: Boolean,
    val showTranslationWarning: Boolean,
    val nutrientsOrder: List<NutrientsOrder>,
    val secureScreen: Boolean,
    val homeCardOrder: List<HomeCard>,
    val onboardingFinished: Boolean,
    val energyFormat: EnergyFormat,
    val appLaunchInfo: AppLaunchInfo,
    val allowFutureDates: Boolean,
    val showGoalsCalories: Boolean,
    val showGoalsMacronutrients: Boolean,
    val calorieAllowedDifference: Int,
) : UserPreferences
