package com.maksimowiczm.foodyou.app.ui.home.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import com.maksimowiczm.foodyou.settings.domain.entity.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal class GoalsCardSettingsViewModel(
    private val settingsRepository: UserPreferencesRepository<Settings>
) : ViewModel() {

    private val _showCalories = settingsRepository.observe().map { it.showGoalsCalories }
    val showCalories =
        _showCalories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2_000),
            initialValue = runBlocking { _showCalories.first() },
        )

    fun toggleShowCalories(newState: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { copy(showGoalsCalories = newState) }
        }
    }

    private val _showMacronutrients =
        settingsRepository.observe().map { it.showGoalsMacronutrients }
    val showMacronutrients =
        _showMacronutrients.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2_000),
            initialValue = runBlocking { _showMacronutrients.first() },
        )

    fun toggleShowMacronutrients(newState: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { copy(showGoalsMacronutrients = newState) }
        }
    }
}
