package com.maksimowiczm.foodyou.app.ui.goals

import com.maksimowiczm.foodyou.app.ui.goals.master.GoalsViewModel
import com.maksimowiczm.foodyou.app.ui.goals.setup.DailyGoalsViewModel
import com.maksimowiczm.foodyou.common.infrastructure.koin.userPreferencesRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel

fun Module.goals() {
    viewModel {
        GoalsViewModel(
            goalsRepository = get(),
            observeDiaryMealsUseCase = get(),
            settingsRepository = userPreferencesRepository(),
        )
    }
    viewModel {
        DailyGoalsViewModel(
            goalsRepository = get(),
            settingsRepository = userPreferencesRepository(),
        )
    }
}
