package com.maksimowiczm.foodyou.app.ui.home.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.app.ui.common.component.ArrowBackIconButton
import foodyou.app.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GoalsCardSettings(
    onBack: () -> Unit,
    onGoalsSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: GoalsCardSettingsViewModel = koinViewModel()

    val showCalories by viewModel.showCalories.collectAsStateWithLifecycle()
    val showMacronutrients by viewModel.showMacronutrients.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier,
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(Res.string.headline_daily_goals)) },
                navigationIcon = { ArrowBackIconButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            stickyHeader {
                GoalsCard(
                    energy = 1600,
                    energyGoal = 2000,
                    proteins = 50,
                    proteinsGoal = 75,
                    carbohydrates = 200,
                    carbohydratesGoal = 300,
                    fats = 70,
                    fatsGoal = 90,
                    onClick = {},
                    onLongClick = {},
                    showCalories = showCalories,
                    showMacronutrients = showMacronutrients,
                    calorieAllowedDifference = 100,
                    modifier = Modifier.padding(16.dp),
                )
            }

            item { HorizontalDivider() }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.headline_show_calories)) },
                    trailingContent = {
                        Switch(checked = showCalories, onCheckedChange = null)
                    },
                    modifier =
                        Modifier.clickable { viewModel.toggleShowCalories(!showCalories) },
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text(stringResource(Res.string.headline_show_macronutrients))
                    },
                    trailingContent = {
                        Switch(checked = showMacronutrients, onCheckedChange = null)
                    },
                    modifier =
                        Modifier.clickable {
                            viewModel.toggleShowMacronutrients(!showMacronutrients)
                        },
                )
            }

            item { HorizontalDivider() }

            item {
                ListItem(
                    headlineContent = {
                        Text(stringResource(Res.string.headline_daily_goals_settings))
                    },
                    modifier = Modifier.clickable { onGoalsSettings() },
                )
            }
        }
    }
}


