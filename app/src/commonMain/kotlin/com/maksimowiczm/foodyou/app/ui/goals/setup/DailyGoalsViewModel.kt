package com.maksimowiczm.foodyou.app.ui.goals.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import com.maksimowiczm.foodyou.goals.domain.entity.WeeklyGoals
import com.maksimowiczm.foodyou.goals.domain.repository.GoalsRepository
import com.maksimowiczm.foodyou.settings.domain.entity.Settings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class DailyGoalsViewModel(
    private val goalsRepository: GoalsRepository,
    private val settingsRepository: UserPreferencesRepository<Settings>,
) : ViewModel() {

    val weeklyGoals =
        goalsRepository
            .observeWeeklyGoals()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(2_000),
                initialValue = null,
            )

    val calorieAllowedDifference: StateFlow<Int> =
        settingsRepository
            .observe()
            .map { it.calorieAllowedDifference }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(2_000),
                initialValue = 100,
            )

    private val _eventChannel = Channel<DailyGoalsViewModelEvent>()
    val events = _eventChannel.receiveAsFlow()

    fun updateWeeklyGoals(weeklyGoals: WeeklyGoals) {
        viewModelScope.launch {
            goalsRepository.updateWeeklyGoals(weeklyGoals)
            _eventChannel.send(DailyGoalsViewModelEvent.Updated)
        }
    }

    fun updateCalorieAllowedDifference(value: Int) {
        viewModelScope.launch { settingsRepository.update { copy(calorieAllowedDifference = value) } }
    }
}
